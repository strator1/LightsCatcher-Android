package com.hs_augsburg_example.lightscatcher.singletons;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.hs_augsburg_example.lightscatcher.dataModels.Record;
import com.hs_augsburg_example.lightscatcher.dataModels.User;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import static java.lang.String.format;

/**
 * Created by quirin on 26.04.17.
 */

@SuppressWarnings("VisibleForTests")
public class PersistenceManager {
    private static final String TAG = "PersistenceManager";
    public static final PersistenceManager shared = new PersistenceManager();

    //destination folder of lights database-entries
    public final String DATA_MODEL_VERSION = "v1_1";

    // path where the images are stored temporarily on the device
    private static final String INTERNAL_IMG_PATH = "lights_images";

    // shared settings key
    private static final String PENDING_UPLOADS = "pendingUploads";

    /**
     * Supervises the state of the connection to the Firebase-database
     */
    public final ConnectedListener connectedListener;

    private final FirebaseDatabase db;
    private final DatabaseReference root;
    private final DatabaseReference users;
    private final DatabaseReference lights;

    private final StorageReference lights_images;

    public static void init() {
        // just to get the static constructor called.
    }

    private PersistenceManager() {
        db = FirebaseDatabase.getInstance();
        db.setPersistenceEnabled(true);
        root = db.getReference();
        users = root.child("users");
        lights = root.child("lights/" + DATA_MODEL_VERSION);
        lights_images = FirebaseStorage.getInstance().getReference("lights_images");


        connectedListener = new ConnectedListener();
        FirebaseDatabase.getInstance().getReference(".info/connected").addValueEventListener(connectedListener);
    }

    public List<UploadTask> getPendingImageUploads() {
        return lights_images.getActiveUploadTasks();
    }

    public Task persist(User usr) {
        if (usr.uid == null) throw new IllegalArgumentException("User.uid was null.");
        return users.child(usr.uid).setValue(usr);
    }

    public Task persist(Record rec) {
        if (rec.id == null) throw new IllegalArgumentException("Record.id was null.");
        return lights.child(rec.id).setValue(rec);
    }

    public StorageTask<UploadTask.TaskSnapshot> persistLightsImage(Context ctx, final String imgId, Bitmap bmp) throws IOException {

        // this is the destination
        StorageReference ref = lights_images.child(imgId);
        try {
            // create a bookmark in the shared application settings
            // this is required when we try to recover the upload
            addUploadBookmark(ctx, ref, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream baos = null;
        FileOutputStream out = null;

        try {

            // compress image to reduce network-traffic for users
            baos= new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 40, baos);

            // save the image on the device so it doesn't get lost when the upload gets canceled
            File imagesDir = ctx.getDir(INTERNAL_IMG_PATH, Context.MODE_PRIVATE);

            File file = new File(imagesDir, imgId);
            if (!file.exists()) {
                file.createNewFile();
            }

            out = new FileOutputStream(file);
            baos.writeTo(out);

            // now start the upload()
            UploadTask uploadTask = ref.putFile(Uri.fromFile(file));

            // and register listeners
            listenToImageUploadTask(ctx, uploadTask, ref, file);
            return uploadTask;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            out.close();
            baos.close();
        }
    }

    public void resumePendingUploads(Context ctx) {
        SharedPreferences sessions = ctx.getSharedPreferences(PENDING_UPLOADS, Context.MODE_PRIVATE);
        Set<? extends Map.Entry<String, ?>> set = sessions.getAll().entrySet();
        if (set.size() == 0) {
            Log.d(TAG, "no pending uploads found");
        } else {
            for (Map.Entry<String, ?> p : set) {
                String storageUri = p.getKey();
                String sessionUri = (String) p.getValue();

                Log.d(TAG, format("Found pending upload to '%1$s'; sessionId: '%2$s'", storageUri, sessionUri));

                UploadTask uploadTask = null;
                try {
                    uploadTask = resumeUploadSession(storageUri, sessionUri, ctx);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (uploadTask == null) {
                    Log.d(TAG, "Failed to resume upload.");
                }
            }
        }
    }

    private UploadTask resumeUploadSession(String storagePath, String sessionUri, Context ctx) {
        StorageReference ref = FirebaseStorage.getInstance().getReference(storagePath);

        String imgId = ref.getName();

        File file = new File(ctx.getDir(INTERNAL_IMG_PATH, Context.MODE_PRIVATE), imgId);
        if (!file.exists()) {
            Log.d(TAG, "cannot restore upload, image-file is missing. expected path: " + file.toString());

            // no chance to resume the upload when there's no image file.
            // remove the bookmark because this should not be tried again
            removeUploadBookmark(ctx, storagePath);
            return null;
        }

        StorageMetadata meta = getJPGMeta();
        return resumeUploadSession(ctx, ref, file, meta, sessionUri);
    }

    private UploadTask resumeUploadSession(Context ctx, final StorageReference ref, File file, StorageMetadata meta, String sessionId) {
        // decide whether to
        // 1) restart the upload
        // 2) reuse an existing upload session
        // 3) do nothing because uploads are already in progress.
        List<UploadTask> active = ref.getActiveUploadTasks();
        UploadTask uploadTask;
        switch (active.size()) {
            case 0:
                if (sessionId == null) {
                    Log.d(TAG, "restarting UploadTask to storage location " + ref.toString());
                    // no upload-session available. we have to (re-)start the upload task.
                    // this may occur when the process terminated and no network-connection could be established before
                    uploadTask = ref.putFile(Uri.fromFile(file), meta);
                    listenToImageUploadTask(ctx, uploadTask, ref, file);
                } else {
                    // upload started before but process was terminated
                    Log.d(TAG, "resuming UploadTask to storage location " + ref.toString());
                    uploadTask = ref.putFile(Uri.fromFile(file), meta, Uri.parse(sessionId));
                    listenToImageUploadTask(ctx, uploadTask, ref, file);
                }
                break;
            case 1:
                Log.d(TAG, "found existing UploadTasks to storage location " + ref.toString());
                uploadTask = active.get(0);
                Log.d(TAG, format("\t$=%1$s; inProgress: %2$s; canceled: %3$s", uploadTask.toString(), uploadTask.isInProgress(), uploadTask.isCanceled()));

                if (uploadTask.isInProgress()) {
                    //this is just fine, task will hopefully be finished soon
                    Log.d(TAG, "existing task is running");
                    break;
                } else if (uploadTask.isPaused()) {
                    Log.d(TAG, "existing task is paused, resuming");
                    uploadTask.resume();
                } else if (uploadTask.isCanceled()) {
                    Exception e = uploadTask.getException();
                    Log.d(TAG, "existing task is canceled, restarting; exception: " + (e == null ? "null" : e.getMessage()));
                    uploadTask = ref.putFile(Uri.fromFile(file), meta);
                    listenToImageUploadTask(ctx, uploadTask, ref, file);
                }
                break;
            default:
                // this should not happen usually
                Log.d(TAG, format("found %1$s existing UploadTasks to storage location '%2$s'", active.size(), ref.toString()));
                for (UploadTask task : active) {
                    Log.d(TAG, format("\t$=%1$s; inProgress: %2$s; canceled: %3$s; exception: %4$s", task.toString(), task.isInProgress(), task.isCanceled(), task.getException().toString()));
                }
                uploadTask = active.get(0);
                break;
        }
        return uploadTask;
    }

    private void listenToImageUploadTask(final Context ctx, final UploadTask task, final StorageReference ref, final File file) {
        final boolean[] saved = {false};
        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "onProgress from " + taskSnapshot.getStorage().toString());
                // this may not get called in case of bad network connection
                if (!saved[0]) {
                    Uri sessionUri = taskSnapshot.getUploadSessionUri();
                    if (sessionUri != null) {
                        // A persisted session has begun with the server.
                        // Now we know the sessionUri, which we can use to resume the upload
                        saved[0] = true;
                        addUploadBookmark(ctx, ref, sessionUri);
                    }
                }
            }
        });
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "onSuccess from " + taskSnapshot.getStorage().getPath());

                // upload finished,
                // remove the bookmark
                try {
                    removeUploadBookmark(ctx, taskSnapshot.getStorage().getPath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // delete image data on the device
                try {
                    Log.d(TAG, "delete file " + file.toURI());
                    file.deleteOnExit();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // and add the image url to the database
                // this assumes that the key of lights elements in the database matches the storage-id of the image
                try {
                    String imgId = taskSnapshot.getStorage().getName();
                    String url = taskSnapshot.getDownloadUrl().toString();
                    lights.child(imgId + "/imageUrl").setValue(url);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure from " + task.getSnapshot().getStorage().toString());
                e.printStackTrace();
            }
        });
    }

    private void addUploadBookmark(Context ctx, StorageReference ref, Uri sessionUri) {
        SharedPreferences sessions = ctx.getSharedPreferences(PENDING_UPLOADS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sessions.edit();
        edit.putString(ref.getPath(), sessionUri == null ? null : sessionUri.toString());
        edit.apply();
        Log.d(TAG, "add upload bookmark: " + ref.toString());
    }

    private void removeUploadBookmark(Context ctx, String storageRef) {
        SharedPreferences sessions = ctx.getSharedPreferences(PENDING_UPLOADS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sessions.edit();
        edit.remove(storageRef);
        edit.apply();
        Log.d(TAG, "remove upload bookmark: " + storageRef);
    }


    private StorageMetadata getJPGMeta() {
        return new StorageMetadata.Builder()
                .setContentType("image/jpg")
                .build();

    }

    public class ConnectedListener extends Observable implements ValueEventListener {
        private boolean isConnected;

        public boolean isConnected() {
            return isConnected;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            this.isConnected = dataSnapshot.getValue(Boolean.class);
//            Log.d(TAG, ".info/connected changed: " + isConnected);
            this.setChanged();
            this.notifyObservers();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            this.isConnected = false;
//            Log.d(TAG, ".info/connected hanged: " + isConnected);
            this.setChanged();
            this.notifyObservers();
        }
    }

}
