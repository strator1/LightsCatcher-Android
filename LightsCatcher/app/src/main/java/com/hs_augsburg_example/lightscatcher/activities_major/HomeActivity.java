package com.hs_augsburg_example.lightscatcher.activities_major;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.BuildConfig;
import com.hs_augsburg_example.lightscatcher.activities_minor.DatenschutzActivity;
import com.hs_augsburg_example.lightscatcher.activities_minor.HelpActivity;
import com.hs_augsburg_example.lightscatcher.activities_minor.LoginActivity;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.activities_minor.TermsOfUseActivity;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.Log;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;


import java.util.Observable;
import java.util.Observer;

import static com.hs_augsburg_example.lightscatcher.R.id.fab;
import static com.hs_augsburg_example.lightscatcher.R.id.home_layout_connection;
import static com.hs_augsburg_example.lightscatcher.R.id.home_txt_score;

/**
 * Main screen of the App. Offers Info about current user and a high-score-list.
 */
public class HomeActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "HomeActivity";
    private static final boolean LOG = Log.ENABLED && true;

    // GUI:
    private TextView txtUserName;
    private TextView txtUserRank;
    private TextView txtUserScore;
    private SwipeRefreshLayout swipeLayout;
    private LinearLayout warning;
    private TextView btnBackupCount;
    private TextView btnUploadStatus;
    private TextView txtConnection;
    private ImageView imgConnection;
    private RecyclerView recyclerView;

    // PRIVATE:
    private Query sortedUsers = null;
    private Query top10 = null;
    private FirebaseRecyclerAdapter<User, UserHolder> adapter = null;
    private ValueEventListener listenerForCurrentRanking;
    private Observer loginObserver;
    private Observer connectionObserver;
    private Observer backupListener;
    private Observer uploadListener;
    private ShowcaseHandler showcaseHandler;

    /* =======================================
     * ACTIVITY-LIFE-CYCLE:
     * -------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (LOG) Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);


        boolean loggedIn = UserInformation.shared.tryAuthenticate();
        if (!loggedIn) {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        ActivityRegistry.register(this);

        if (BuildConfig.DEBUG)
            Toast.makeText(getApplicationContext(), "DEBUG: " + BuildConfig.DEBUG, Toast.LENGTH_SHORT).show();

        // GUI-stuff
        setContentView(R.layout.activity_home);
        this.txtUserName = (TextView) findViewById(R.id.home_txt_username);
        this.txtUserRank = (TextView) findViewById(R.id.home_txt_rank);
        this.txtUserScore = (TextView) findViewById(R.id.home_txt_score);
        this.swipeLayout = (SwipeRefreshLayout) findViewById(R.id.home_refreshLayout);
        this.warning = (LinearLayout) findViewById(R.id.home_layout_warning);
        this.btnBackupCount = (TextView) findViewById(R.id.home_txt_backupCount);
        this.btnUploadStatus = (Button) findViewById(R.id.home_btn_clickForUpload);
        this.txtConnection = (TextView) findViewById(R.id.home_txt_connection);
        this.imgConnection = (ImageView) findViewById(R.id.home_img_connection);
        recyclerView = (RecyclerView) findViewById(R.id.list_userRanking);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToCamera();
            }
        });

        // start resume backup files
        try {
            PersistenceManager.shared.startAutoRecover(this.getApplicationContext());
        } catch (Exception ex) {
            Log.e(TAG, ex);
        }

        // show loading animation:
        setRefreshAnimation(true);
        swipeLayout.setOnRefreshListener(this);


        // query ordered list of users from firebase
        sortedUsers = FirebaseDatabase.getInstance().getReference("users").orderByChild("points");
        // top 50 users:
        top10 = sortedUsers.limitToLast(50);

        // Create a new adapter
        adapter = new FirebaseRecyclerAdapter<User, UserHolder>(User.class, R.layout.item_user, UserHolder.class, top10) {
            @Override
            protected void populateViewHolder(UserHolder viewHolder, User model, int position) {
                // @{link position} is used for the rank of each user in the listview
                // invert position because we use a reverse layout manager.
                // as a consequence position 0 is the first item from the bottom
                position = adapter.getItemCount() - position;
                viewHolder.applyUser(model, position);
            }
        };
        top10.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setRefreshAnimation(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                setRefreshAnimation(false);
            }
        });

        // use reverse layout to sort users in descending order
        // firebase-queries currently do not support descending order
        final LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setReverseLayout(true);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

//        if (true) {
        if (UserPreference.shouldShowDialog(getApplicationContext(), ShowcaseHandler.SETTINGS_KEY)) {

            showcaseHandler = new ShowcaseHandler();
            showcaseHandler.startShowcase();
        }
    }

    @Override
    protected void onStart() {
        if (LOG) Log.d(TAG, "onStart");
        super.onStart();

        // listen to login-changes:
        loginObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (LOG) Log.d(TAG, "update from loginObserver");
                HomeActivity.this.updateUI_UserData();
            }
        };
        UserInformation.shared.addObserver(loginObserver);

        // listen to connection state:
        this.connectionObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (LOG) Log.d(TAG, "connectionObserver.update");
                updateOnlineStatus();
            }
        };
        PersistenceManager.shared.connectedMonitor.addObserver(connectionObserver);

        //listen to pending uploads
        this.backupListener = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (LOG) Log.d(TAG, "backupListener.update");
                updateBackupCount();
            }
        };
        PersistenceManager.shared.backupStorage.addObserver(backupListener);

        // called when upload tasks are scheduled
        this.uploadListener = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (LOG) Log.d(TAG, "backupListener.update");
                updateUploadStatus();
            }
        };
        PersistenceManager.shared.uploadMonitor.addObserver(uploadListener);

        synchronized (PersistenceManager.shared.backupStorage) {
            final Context ctx = getApplicationContext();
            final String DIALOG_KEY = "OfflineWarning";
            final int count = PersistenceManager.shared.backupStorage.list(ctx).length;

            if (count >= 50) {
                if (UserPreference.shouldShowDialog(ctx, DIALOG_KEY)) {
                    AlertDialog dlg = new AlertDialog.Builder(this)
                            .setTitle("")
                            .setMessage("Du hast jetzt schon " + count + " Fotos im Offline-Modus gemacht. Denk bitte daran, dass du die App demnächst online eine Zeit lang laufen lässt, damit die Bilder auch wirklich hochgeladen werden.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    UserPreference.neverShowAgain(ctx, DIALOG_KEY, true);
                                }
                            })
                            .create();
                    dlg.show();
                }
            }
        }

        // Listen to the whole userslist to calculate the rank of the current user
        // this might cause a lot of network-traffic when there are many users.
        // maybe there's a better solution, but for now it does the job
        listenerForCurrentRanking = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int usersRank = -1;
                int i = 0;
                int n = (int) dataSnapshot.getChildrenCount();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String key = child.getKey();
                    if (key.equals(UserInformation.shared.getUserId())) {
                        usersRank = n - i;

                    }
                    i++;
                    HomeActivity.this.updateUI_UserRank(usersRank);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this.getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT);
                HomeActivity.this.updateUI_UserRank(-1);
            }
        };
        sortedUsers.addValueEventListener(listenerForCurrentRanking);
    }

    @Override
    protected void onResume() {
        if (LOG) Log.d(TAG, "onResume");

        this.updateUI_UserData();
        this.updateOnlineStatus();
        this.updateBackupCount();
        this.updateUploadStatus();

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (LOG) Log.d(TAG, "onPause");
        super.onPause();
    }

    /**
     * Called when swiping the ranking-list
     */
    @Override
    public void onRefresh() {
        //show loading animation during loading
        setRefreshAnimation(true);

        // update user interface
        updateOnlineStatus();
        updateBackupCount();
        this.adapter.notifyDataSetChanged();
        this.updateUI_UserData();
        // this does not update the current user's rank. let's hope that {@link listenerForCurrentRanking} works properly

        //stop loading animation
        setRefreshAnimation(false);
    }

    @Override
    protected void onStop() {
        if (LOG) Log.d(TAG, "onStop");

        // detach listeners from firebase and other services:
        if (sortedUsers != null) sortedUsers.removeEventListener(listenerForCurrentRanking);

        if (backupListener != null)
            PersistenceManager.shared.backupStorage.deleteObserver(backupListener);
        if (connectionObserver != null)
            PersistenceManager.shared.connectedMonitor.deleteObserver(connectionObserver);
        if (uploadListener != null)
            PersistenceManager.shared.uploadMonitor.deleteObserver(uploadListener);
        if (loginObserver != null) UserInformation.shared.deleteObserver(loginObserver);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (LOG) Log.d(TAG, "onDestroy");

        super.onDestroy();
        if (adapter != null) adapter.cleanup();
    }

    /* =======================================
     * UPDATE VIEWS:
     * -------------------------------------*/

    private void updateOnlineStatus() {
        updateOnlineStatus(PersistenceManager.shared.connectedMonitor.isConnected());
    }

    private void updateOnlineStatus(boolean connected) {
        try {
            int vis = connected ? View.GONE : View.VISIBLE;
            warning.setVisibility(vis);

            txtConnection.setText(connected ? "Online" : "Offline");
            imgConnection.setImageResource(connected ? R.mipmap.ic_yes : R.mipmap.ic_no);
        } catch (Exception ex) {
            Log.e(TAG, ex);
        }
    }

    private void updateBackupCount() {
        try {

            int count = PersistenceManager.shared.backupStorage.list(this.getApplicationContext()).length;
            if (count > 0) {
                btnBackupCount.setVisibility(View.VISIBLE);
                btnBackupCount.setText(getResources().getQuantityString(R.plurals.home_txt_uploadCount, count, count));
                btnUploadStatus.setVisibility(View.VISIBLE);
            } else {
                btnBackupCount.setVisibility(View.GONE);
                btnBackupCount.setText("");
                btnUploadStatus.setVisibility(View.GONE);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex);
        }
    }

    private void updateUploadStatus() {
        try {
            int activeCount = PersistenceManager.shared.uploadMonitor.countActiveTasks();
            boolean isActive = activeCount > 0;

            String txt = isActive ? getString(R.string.home_txt_uploading) : getString(R.string.home_txt_clickForUpload);
            btnUploadStatus.setText(txt);
        } catch (Exception ex) {
            Log.e(TAG, ex);
        }
    }

    private void updateUI_UserRank(int rank) {
        txtUserRank.setText(getString(R.string.home_txt_rank, Integer.toString(rank)));
    }

    private void updateUI_UserData() {
        User usr = UserInformation.shared.getUserSnapshot();
        if (usr != null) {
            txtUserName.setText(usr.name);
            txtUserScore.setText(getString(R.string.home_txt_score, Integer.toString(usr.points)));
            // NOTE: the user's rank is updated by {@link listenerForCurrentRanking}
        } else {
            txtUserName.setText("-");
            txtUserScore.setText(getString(R.string.home_txt_score, "N.A."));
        }
    }

    private void setRefreshAnimation(final boolean b) {
        if (swipeLayout.isRefreshing())
            swipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(b);
                }
            });
    }

    /* =======================================
     * EVENT-HANDLERS:
     * -------------------------------------*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.datenschutz:
                Intent intentDatenschutz = new Intent(HomeActivity.this, DatenschutzActivity.class);
                startActivity(intentDatenschutz);
                return true;
            case R.id.help:
                Intent intentHelp = new Intent(HomeActivity.this, HelpActivity.class);
                startActivity(intentHelp);
                return true;
            case R.id.terms_of_use:
                Intent intentTermsOfUse = new Intent(HomeActivity.this, TermsOfUseActivity.class);
                startActivity(intentTermsOfUse);
                return true;
            case R.id.home_action_resetIntro:
                Context ctx = getApplicationContext();
                UserPreference.neverShowAgain(ctx, ShowcaseHandler.SETTINGS_KEY, false);
                UserPreference.neverShowAgain(ctx, TakePictureActivity.ShowcaseHandler.SETTINGS_KEY, false);
                UserPreference.neverShowAgain(ctx, SubmitDialog.ShowcaseHandler.SETTINGS_KEY, false);

                Intent intent = getIntent();
                finish();
                startActivity(intent);

                return true;
            case R.id.logout:
                UserInformation.shared.logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        ActivityRegistry.finishAll();
        super.onBackPressed();
    }

    public void startUpload_Click(View view) {
        PersistenceManager.shared.retryPendingUploads(this.getApplicationContext());
    }

    private void navigateToCamera() {
        if (showcaseHandler != null) {
            showcaseHandler.showcaseView.hide();
        }
        Intent intent = new Intent(HomeActivity.this, TakePictureActivity.class);
        startActivity(intent);
    }

    /* =======================================
     * INNER CLASSES:
     * -------------------------------------*/

    private class ShowcaseHandler implements View.OnClickListener {
        private static final String SETTINGS_KEY = "HELP_HOME";

        int counter = 0;
        private ShowcaseView showcaseView;

        public void startShowcase() {
            RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            buttonParams.setMargins(10, 10, 10, 10);

            showcaseView = new ShowcaseView.Builder(HomeActivity.this)
                    .withHoloShowcase2()
                    .setContentTitle("Willkommen bei LightsCatcher")
                    .setContentText("\nDanke, dass Du das Projekt unterstützt. :)\n\nLass Dir kurz zeigen, wie die App funktioniert.")
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setOnClickListener(this)
                    .build();
            showcaseView.setButtonText("Weiter");
            showcaseView.setButtonPosition(buttonParams);
        }

        @Override
        public void onClick(View v) {
            switch (counter) {
                case 0:
                    showcaseView.setShowcase(new ViewTarget(recyclerView), false);
                    showcaseView.setScaleMultiplier(2.3f);
                    showcaseView.setContentTitle("");
                    showcaseView.setContentText("Hier werden die 50 besten Ampeljäger angezeigt.");
                    break;
                case 1:
                    showcaseView.setScaleMultiplier(1);
                    showcaseView.setShowcase(new ViewTarget(findViewById(home_txt_score)), false);
                    showcaseView.setContentText("Deine aktuelle Platzierung wird hier angezeigt.");
                    break;
                case 2:
                    showcaseView.setShowcase(new ViewTarget(findViewById(home_layout_connection)), false);
                    showcaseView.setContentText("Hier siehst Du den Verbindungsstatus zu unserem Server. Du kannst übrigens auch offline Fußgängerampeln fotografieren. Die Fotos werden hochgeladen, sobald die App wieder im Online-Modus läuft.");
                    break;
                case 3:
                    showcaseView.setShowcase(new ViewTarget(findViewById(fab)), false);
                    showcaseView.setContentText("Mit dem Button rechts unten gelangst Du zur Kamera.\n\n Suche Dir eine Fußgängerampel und mach die ersten Punkte. :)");
                    showcaseView.setButtonText("Schließen");
                    RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    buttonParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    buttonParams.setMargins(10, 10, 10, 10);
                    showcaseView.setButtonPosition(buttonParams);

                    UserPreference.neverShowAgain(getApplicationContext(), SETTINGS_KEY, true);
                    break;
                case 4:
                default:
                    showcaseView.hide();
                    showcaseView = null;
                    showcaseHandler = null;
            }

            counter++;
        }
    }

    /**
     * A  Wrapper for a {@link User}. It's used by the RecyclerView
     */
    public static class UserHolder extends RecyclerView.ViewHolder {

        final TextView txtRank;
        final TextView txtName;
        final TextView txtPoints;

        public UserHolder(View v) {
            super(v);
            txtRank = (TextView) v.findViewById(R.id.item_user_rank);
            txtName = ((TextView) v.findViewById(R.id.item_user_name));
            txtPoints = ((TextView) v.findViewById(R.id.item_user_score));
        }

        public void applyUser(User usr, int rank) {
            txtRank.setText(rank + ":");
            txtName.setText(usr.name);
            txtPoints.setText(Integer.toString(usr.points));
        }
    }
}
