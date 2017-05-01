package com.hs_augsburg_example.lightscatcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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


import java.util.Observable;
import java.util.Observer;

/**
 * Main screen of the App. Offers Info about current user and a high-score-list.
 */
public class HomeActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "HomeActivity";
    private FirebaseRecyclerAdapter<User, UserHolder> adapter = null;
    private Query sortedUsers = null;
    private Query top10 = null;
    private TextView txtUserName;
    private TextView txtUserRank;
    private TextView txtUserScore;
    private SwipeRefreshLayout swipeLayout;
    private ValueEventListener listenerForCurrentRanking;
    private Observer loginObserver;
    private Observer connectionObserver;
    private ToggleButton btnStatus;
    private LinearLayout warning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegistry.register(this);

        //Has to be called before using Firebase
        PersistenceManager.init();

        // GUI-stuff
        setContentView(R.layout.activity_home);
        this.txtUserName = (TextView) findViewById(R.id.home_txt_username);
        this.txtUserRank = (TextView) findViewById(R.id.home_txt_rank);
        this.txtUserScore = (TextView) findViewById(R.id.home_txt_score);
        this.swipeLayout = (SwipeRefreshLayout) findViewById(R.id.home_refreshLayout);
        this.btnStatus = (ToggleButton) findViewById(R.id.home_btn_status);
        this.warning = (LinearLayout) findViewById(R.id.home_warning);
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


        // listen to login-changes:
        loginObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                //Log.d(TAG,"update from loginObserver");
                HomeActivity.this.updateUI_UserData(UserInformation.shared.getUserSnapshot());
            }
        };
        UserInformation.shared.addObserver(loginObserver);


        // listen to connection state:
        this.connectionObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                HomeActivity.this.updateOnlineStatus();
            }
        };
        PersistenceManager.shared.connectedListener.addObserver(connectionObserver);

        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeActivity.this.updateOnlineStatus();
            }
        });

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
                    if (key.equals(UserInformation.shared.getUid())) {
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

    private void updateOnlineStatus() {
        boolean connected = PersistenceManager.shared.connectedListener.isConnected();
        btnStatus.setChecked(connected);
        warning.setVisibility(connected ? View.GONE : View.VISIBLE);
        if (connected) {
            // resume unfinished upload tasks
            try {
                PersistenceManager.shared.resumePendingUploads(this.getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.updateOnlineStatus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // detach all listeners from FireBase
        if (sortedUsers != null) sortedUsers.removeEventListener(listenerForCurrentRanking);
        if (adapter != null) adapter.cleanup();
        if (connectionObserver != null)
            PersistenceManager.shared.connectedListener.deleteObserver(connectionObserver);

        // and other services:
        if (loginObserver != null) UserInformation.shared.deleteObserver(loginObserver);
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

    private void updateUI_UserData(User usr) {
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
        //show loading animation during loading
        swipeLayout.setRefreshing(true);

        // update the view
        this.adapter.notifyDataSetChanged();
        this.updateUI_UserData(UserInformation.shared.getUserSnapshot());
        // this does not update the current user's rank. let's hope that {@link listenerForCurrentRanking} works properly

        //stop loading animation
        swipeLayout.setRefreshing(false);
    }

    @Override
    public void onBackPressed() {
        ActivityRegistry.finishAll();
        super.onBackPressed();
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
