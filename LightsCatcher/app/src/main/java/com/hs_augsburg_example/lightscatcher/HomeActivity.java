package com.hs_augsburg_example.lightscatcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.camera.TakePictureActivity;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.Log;


import java.util.Observable;
import java.util.Observer;

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
    private View layoutOffline;
    private View layoutOnline;
    private SwipeRefreshLayout swipeLayout;
    private LinearLayout warning;

    //FIELDS:
    private Query sortedUsers = null;
    private Query top10 = null;
    private FirebaseRecyclerAdapter<User, UserHolder> adapter = null;
    private ValueEventListener listenerForCurrentRanking;
    private Observer loginObserver;
    private Observer connectionObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (LOG) Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        ActivityRegistry.register(this);

        //Has to be called before using Firebase
        //PersistenceManager.init();

        // GUI-stuff
        setContentView(R.layout.activity_home);
        this.txtUserName = (TextView) findViewById(R.id.home_txt_username);
        this.txtUserRank = (TextView) findViewById(R.id.home_txt_rank);
        this.txtUserScore = (TextView) findViewById(R.id.home_txt_score);
        this.swipeLayout = (SwipeRefreshLayout) findViewById(R.id.home_refreshLayout);
        this.layoutOffline = findViewById(R.id.home_layout_offline);
        this.layoutOnline = findViewById(R.id.home_layout_online);
        this.warning = (LinearLayout) findViewById(R.id.home_layout_warning);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list_userRanking);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToCamera();
            }
        });


        // show loading animation:
        // it will be stopped by the {@link adapter} in onDataChanged
        swipeLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(true);
            }
        });
        swipeLayout.setOnRefreshListener(this);

        boolean loggedIn = UserInformation.shared.tryAuthenticate();
        if (!loggedIn) {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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

            @Override
            protected void onDataChanged() {
                super.onDataChanged();

                // if the loading-animation is still running stop it
                if (swipeLayout.isRefreshing())
                    swipeLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            swipeLayout.setRefreshing(false);
                        }
                    });
            }
        };

        // use reverse layout to sort users in descending order
        // firebase-queries currently do not support descending order
        final LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setReverseLayout(true);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

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
        this.updateUI_UserData();


        // listen to connection state:
        this.connectionObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (LOG) Log.d(TAG, "connectionObserver.update");
                onOnlineStatusChanged();
            }
        };
        PersistenceManager.shared.connectedListener.addObserver(connectionObserver);


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

        super.onResume();
        this.onOnlineStatusChanged();
    }

    @Override
    protected void onPause() {
        if (LOG) Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (LOG) Log.d(TAG, "onStop");

        // detach all listeners from FireBase
        if (sortedUsers != null) sortedUsers.removeEventListener(listenerForCurrentRanking);

        if (connectionObserver != null) PersistenceManager.shared.connectedListener.deleteObserver(connectionObserver);

        // and other services:
        if (loginObserver != null) UserInformation.shared.deleteObserver(loginObserver);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (LOG) Log.d(TAG, "onDestroy");

        super.onDestroy();
        if (adapter != null) adapter.cleanup();

    }

    private void onOnlineStatusChanged() {
        onOnlineStatusChanged(PersistenceManager.shared.connectedListener.isConnected());
    }

    private void onOnlineStatusChanged(boolean connected) {
        if (connected) {
            // try to resume unfinished upload tasks
            try {
                PersistenceManager.shared.resumePendingUploads(getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateOnlineStatus(connected);
    }

    private void updateOnlineStatus(boolean connected) {
        int vis = connected ? View.GONE : View.VISIBLE;
        int vis_invert = connected ? View.VISIBLE : View.GONE;
        warning.setVisibility(vis);
        layoutOffline.setVisibility(vis);
        layoutOnline.setVisibility(vis_invert);
    }


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
            case R.id.terms_of_use:
                Intent intentTermsOfUse = new Intent(HomeActivity.this, TermsOfUseActivity.class);
                startActivity(intentTermsOfUse);
                return true;
            case R.id.logout:
                UserInformation.shared.logout();
                Intent intentLogout = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intentLogout);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

    private void navigateToCamera() {
        Intent intent = new Intent(HomeActivity.this, TakePictureActivity.class);
        startActivity(intent);
    }

    /**
     * Called when swiping the ranking-list
     */
    @Override
    public void onRefresh() {
        onOnlineStatusChanged();

        //show loading animation during loading
        swipeLayout.setRefreshing(true);

        // update the view
        this.adapter.notifyDataSetChanged();
        this.updateUI_UserData();
        // this does not update the current user's rank. let's hope that {@link listenerForCurrentRanking} works properly

        //stop loading animation
        swipeLayout.setRefreshing(false);
    }

    @Override
    public void onBackPressed() {
        ActivityRegistry.finishAll();
        super.onBackPressed();
    }

    public void onOfflineInfoClicked(View view) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View dlgView = inflater.inflate(R.layout.content_offline_helpdialog, null);
        dialogBuilder.setView(dlgView);
        dialogBuilder.setTitle("Info zum Offline Modus");

        dialogBuilder.setPositiveButton("Verstanden", null);

        dialogBuilder.create().show();
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
