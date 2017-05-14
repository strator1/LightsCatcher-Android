package com.hs_augsburg_example.lightscatcher.singletons;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;


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
import com.hs_augsburg_example.lightscatcher.dataModels.Light;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.utils.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static java.lang.String.format;

/**
 * Created by quirin on 26.04.17.
 */

@SuppressWarnings("VisibleForTests")
public class PersistenceManager {
    private static final String TAG = "PersistenceManager";
    private static final boolean LOG = Log.ENABLED && true;

    public static final PersistenceManager shared = new PersistenceManager();

    //destination folder of lights database-entries
    static final String DATA_MODEL_VERSION = "v1_0_testing";

    // path where the images are stored temporarily on the device
    @VisibleForTesting
    public static final String INTERNAL_IMG_PATH = "lights_images";

    //Supervises the state of the connection to the Firebase-database
    public final ConnectedListener connectedListener;
    public Observer connectedObserver;

    private final FirebaseDatabase db;
    private final DatabaseReference root;
    private final DatabaseReference users;
    private final DatabaseReference lights;

    private final StorageReference lights_images;
    public final BackupStorage backupStorage;

    public static void init() {
        // just to get the static constructor called.
    }

    private PersistenceManager() {
        db = FirebaseDatabase.getInstance();
        db.setPersistenceEnabled(true);
        root = db.getReference();
        users = root.child("users");
        lights = root.child("lights/" + DATA_MODEL_VERSION);
        lights_images = FirebaseStorage.getInstance().getReference(DATA_MODEL_VERSION);


        connectedListener = new ConnectedListener();
        db.getReference(".info/connected").addValueEventListener(connectedListener);
        backupStorage = new BackupStorage();
    }

    public void startResume(final Context ctx) {
        if (connectedListener != null)
            return; // already listening

        connectedObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (LOG)
                    Log.d(TAG, "connectedObserver.update, connected: " + connectedListener.isConnected);
                if (connectedListener.isConnected)
                    resumePendingUploads(ctx);
            }
        };
        connectedListener.addObserver(connectedObserver);
        resumePendingUploads(ctx);
    }

    public List<UploadTask> getPendingImageUploads() {
        return lights_images.getActiveUploadTasks();
    }

    public Task persist(User usr) {
        if (usr.uid == null) throw new IllegalArgumentException("User.uid was null.");
        return users.child(usr.uid).setValue(usr);
    }

    public Task persist(Light light, String uid) {
        if (uid == null) throw new IllegalArgumentException("uid cannot be null");
        return lights.child(uid).setValue(light);
    }

    public UploadTask persistLightsImage(Context ctx, final String imgId, Bitmap bmp) {
        // this is the destination
        StorageReference ref = lights_images.child(imgId);

        // compress image to reduce network-traffic for users
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 40, baos);

        // save the image on the device so it doesn't get lost in case the upload fails for any reason
        // with this we get a second chance to restart the upload
        synchronized (backupStorage) {
            try {
                backupStorage.put(ctx, imgId, baos);
            } catch (Exception ex) {
                Log.e(TAG, ex);
            } finally {
                try {
                    baos.close();
                } catch (IOException e) {
                    Log.e(TAG, e);
                }
            }
            byte[] imageBytes = baos.toByteArray();
            return uploadImageInternal(ctx, ref, imageBytes);
        }
    }

    /**
     * Tries to revoke all pending uploads, if there are any backup-files on this device
     *
     * @param ctx
     * @return all newly started upload tasks
     */
    public UploadTask[] resumePendingUploads(Context ctx) {
        List<UploadTask> result = new ArrayList<>(1);
        synchronized (this.backupStorage) {
            // HOW IT WORKS IN GENERAL:
            // every time an upload is initiated the image-data is not just uploaded
            // but also stored in the internal app storage on the device.

            File[] files = backupStorage.list(ctx);
            for (File file : files) {
                if (LOG) Log.d(TAG, "found backup image: " + file.toString());
                String imageId = file.getName();
                StorageReference ref = lights_images.child(imageId);

                if (shouldStartNewUpload(ref)) {
                    result.add(uploadImageInternal(ctx, ref, file));
                }
            }
            UploadTask[] array = new UploadTask[result.size()];
            result.toArray(array);
            return array;
        }
    }

    /**
     * Indicates whether you should start an upload task or not, because an upload to the given ref is in progress
     *
     * @return true if no upload is currently active or if all active upload-tasks have failed. false if there is an active task in progress
     */
    private boolean shouldStartNewUpload(StorageReference ref) {
        List<UploadTask> active = ref.getActiveUploadTasks();

        switch (active.size()) {
            case 0:
                if (LOG)
                    Log.d(TAG, "should start because there are no active tasks" + ref.toString());
                return true;
            case 1:
                if (LOG)
                    Log.d(TAG, "found 1 active UploadTask to storage location " + ref.toString());
                UploadTask uploadTask = active.get(0);
                if (uploadTask.isInProgress()) {
                    if (LOG)
                        Log.d(TAG, "active task was in Progress, let's hope it will succeed anytime");
                    //this is just fine, task will hopefully be finished soon
                    return false;
                } else if (uploadTask.isPaused()) {
                    if (LOG) Log.d(TAG, "existing task is paused, resuming");
                    boolean resumed = uploadTask.resume();
                    return !resumed;
                } else if (uploadTask.isCanceled()) {
                    Exception e = uploadTask.getException();
                    if (LOG)
                        Log.e(TAG, "existing task was canceled, restarting; exception: " + (e == null ? "null" : e.getMessage()));
                    return true;
                } else {
                    if (LOG)
                        Log.e(TAG, "not sure about existing upload task, request a restart to make sure it is uploaded");
                    try {
                        uploadTask.cancel();
                    } catch (Exception e) {
                        Log.e(TAG, e);
                    }
                    return true;
                }
            default:
                // this should not happen usually
                if (LOG)
                    Log.d(TAG, format("found %1$s existing UploadTasks to storage location '%2$s'", active.size(), ref.toString()));

                boolean shouldStart = true;
                for (UploadTask task : active) {
                    if (LOG)
                        Log.d(TAG, format("\t$=%1$s; inProgress: %2$s; canceled: %3$s; exception: %4$s", task.toString(), task.isInProgress(), task.isCanceled(), task.getException().toString()));
                    if (task.isInProgress()) shouldStart = false;
                }
                return shouldStart;
        }
    }

    private UploadTask uploadImageInternal(Context ctx, StorageReference ref, File file) {
        UploadTask uploadTask = ref.putFile(Uri.fromFile(file));
        listenToImageUploadTask(ctx, uploadTask);
        return uploadTask;
    }

    private UploadTask uploadImageInternal(Context ctx, StorageReference ref, byte[] imageData) {
        UploadTask uploadTask = ref.putBytes(imageData);
        listenToImageUploadTask(ctx, uploadTask);
        return uploadTask;
    }


    private void listenToImageUploadTask(final Context ctx, final UploadTask task) {
        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                // this may not get called in case of bad network connection
                if (LOG)
                    Log.d(TAG, "onProgress during upload to {0}, bytes transferred: {1} of {2}", taskSnapshot.getStorage().getPath(), taskSnapshot.getBytesTransferred(), taskSnapshot.getTotalByteCount());
            }
        });
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (LOG)
                    Log.d(TAG, "onSuccess during upload to " + taskSnapshot.getStorage().getPath());
                String imgId = taskSnapshot.getStorage().getName();

                // upload finished,
                try {
                    // pass the image-url to the database
                    String url = taskSnapshot.getDownloadUrl().toString();
                    if (LOG) Log.d(TAG, "set .imageUrl: " + url);
                    lights.child(imgId + "/imageUrl").setValue(url);
                } catch (Exception ex) {
                    Log.e(TAG, ex);
                }

                try {
                    // remove backup-file from the device
                    backupStorage.pop(ctx, imgId);
                } catch (Exception ex) {
                    Log.e(TAG, ex);
                }
            }


        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (LOG)
                    Log.d(TAG, "onFailure during upload to " + task.getSnapshot().getStorage().getPath());
                Log.e(TAG, e);
            }
        });
    }

    public class ConnectedListener extends Observable implements ValueEventListener {
        private boolean isConnected;

        public boolean isConnected() {
            return isConnected;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            this.isConnected = dataSnapshot.getValue(Boolean.class);
            if (LOG)
                Log.d(TAG, ".info/connected changed: {0}; observers: {1}", isConnected, this.countObservers());
            this.setChanged();
            this.notifyObservers();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            this.isConnected = false;
            if (LOG)
                Log.d(TAG, ".info/connected listener was cancelled: " + databaseError.getMessage());
            this.setChanged();
            this.notifyObservers();
        }
    }

    public class BackupStorage extends Observable {
        @NonNull
        private File getImageBackupFile(Context ctx, String imgId) {
            return new File(ctx.getDir(INTERNAL_IMG_PATH, Context.MODE_PRIVATE), imgId);
        }

        public File[] list(Context ctx) {
            synchronized (this) {
                // let's see if there are any files to upload:
                File dir = ctx.getDir(INTERNAL_IMG_PATH, Context.MODE_PRIVATE);
                if (!dir.exists()) {
                    if (LOG) Log.d(TAG, "INTERNAL_IMG_PATH does not exist");
                    return new File[0];
                }

                if (LOG) Log.d(TAG, "Found backup files: " + dir.listFiles().length);
                return dir.listFiles();
            }
        }

        public void clean(Context ctx) {
            synchronized (this) {
                // let's see if there are any files to upload:
                File dir = ctx.getDir(INTERNAL_IMG_PATH, Context.MODE_PRIVATE);
                if (!dir.exists()) {
                    if (LOG) Log.d(TAG, "INTERNAL_IMG_PATH does not exist");
                    return;
                }

                if (LOG) Log.d(TAG, "Found backup files: " + dir.listFiles().length);
                for (File f : dir.listFiles()) {
                    f.delete();
                }
                this.setChanged();
                this.notifyObservers();
            }
        }

        private void pop(Context ctx, String imgId) {
            synchronized (this) {
                File file = getImageBackupFile(ctx, imgId);
                if (file.exists()) {
                    if (LOG) Log.d(TAG, "delete file " + file.toURI());
                    boolean deleted = file.delete();
                    if (!deleted) Log.e(TAG, "backup-file was not deleted");
                } else {
                    Log.e(TAG, "tried to delete backup-file but it does not exist at: " + file.getAbsolutePath());
                }
                this.setChanged();
                this.notifyObservers();
            }
        }

        private void put(Context ctx, String filename, ByteArrayOutputStream baos) throws IOException {
            synchronized (this) {
                File file = getImageBackupFile(ctx, filename);
                if (file.exists() || file.createNewFile()) {
                    // file exists or was created
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(file);
                        if (LOG) Log.d(TAG, "writing photo to file: " + file.getAbsolutePath());
                        baos.writeTo(out);
                    } finally {
                        if (out != null)
                            out.close();
                    }
                } else {
                    Log.e(TAG, "backup-file does not exist and could not be created");
                }
                this.setChanged();
                this.notifyObservers();
            }
        }

        @Override
        public void notifyObservers() {
            super.notifyObservers();
            if(LOG)Log.d(TAG,"BackupStorage.notifyObservers, observers: " + this.countObservers());
        }
    }
}
