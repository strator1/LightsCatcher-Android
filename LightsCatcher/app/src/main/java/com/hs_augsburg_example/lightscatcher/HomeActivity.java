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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.camera.TakePictureActivity;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.ReverseFirebaseRecyclerAdapter;

public class HomeActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private ReverseFirebaseRecyclerAdapter<User, UserHolder> adapter = null;
    private Query sortedUsers = null;
    private Query top10 = null;
    private FirebaseAuth mAuth;
    private TextView txtUserName;
    private TextView txtUserRank;
    private TextView txtUserScore;
    private SwipeRefreshLayout swipeLayout;
    private ValueEventListener listenerForCurrentRanking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        this.txtUserName = (TextView) findViewById(R.id.home_txt_username);
        this.txtUserRank = (TextView) findViewById(R.id.home_txt_rank);
        this.txtUserScore = (TextView)findViewById(R.id.home_txt_score);

        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUsr = mAuth.getCurrentUser();

        if (firebaseUsr == null) {
            // Not logged in yet.
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        fetchUser(firebaseUsr.getUid());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToCamera();
            }
        });

        ActivityRegistry.register(this);



        this.swipeLayout = (SwipeRefreshLayout) findViewById(R.id.home_refreshLayout);
        swipeLayout.setOnRefreshListener(this);

        // query ordered list of users from firebase
        sortedUsers = FirebaseDatabase.getInstance().getReference("users").orderByChild("points");
        top10 = sortedUsers.limitToLast(50);
        /*sortedUsers.keepSynced(true);
        top10.keepSynced(true);*/

        // Create a new Adapter
        adapter = new ReverseFirebaseRecyclerAdapter<User,UserHolder>(User.class, R.layout.item_user,UserHolder.class, top10) {
            @Override
            protected void populateViewHolder(UserHolder viewHolder, User model, int position) {
                viewHolder.applyUser(model,position);
            }
        };

        // Assign adapter to ListView
        final RecyclerView listView = (RecyclerView) findViewById(R.id.list_userRanking);
        final LinearLayoutManager lm = new LinearLayoutManager(this);
        //  lm.setReverseLayout(true);
        // lm.setStackFromEnd(true);
        listView.setLayoutManager(lm);
        listView.setAdapter(adapter);

        // Listen to the whole userslist to calculate the rank of the current user
        // this might cause a lot of traffic when there are many users
        // maybe there's a better solution, but for now it does the job
        listenerForCurrentRanking = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("CALL","listenerForCurrentRanking.onDataChange");
                int usersRank = -1;
                int i = 0;
                int n = (int)dataSnapshot.getChildrenCount();
                for (DataSnapshot child: dataSnapshot.getChildren()){
                    String key = child.getKey();
                    if (key.equals(UserInformation.shared.getUid()))
                    {
                        usersRank = n - i;

                    }
                    i++;
                    HomeActivity.this.updateCurrentUserRank(usersRank);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this.getApplicationContext(),databaseError.getMessage(),Toast.LENGTH_SHORT);
                HomeActivity.this.updateCurrentUserRank(-1);
            }
        };
        sortedUsers.addValueEventListener(listenerForCurrentRanking);
    }

    @Override
    protected void onStop() {
        sortedUsers.removeEventListener(listenerForCurrentRanking);
        adapter.cleanup();
        super.onStop();
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
            case R.id.settings_menu:
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshUserData(){
        User usr = UserInformation.shared.getCurrent();
        if(usr != null)
        {
            txtUserName.setText(usr.name);
            txtUserScore.setText("(Punkte: " + usr.points + ")");
        }
        else
        {
            txtUserName.setText("-");
            txtUserScore.setText("(Punkte: N.A.)" );
        }
    }

    private void updateCurrentUserRank(int rank){
        txtUserRank.setText("Rang: "+ rank);
    }

    private void navigateToCamera() {
        Intent intent = new Intent(HomeActivity.this, TakePictureActivity.class);
        startActivity(intent);
    }


    private void fetchUser(String uid) {

        FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                UserInformation.shared.setCurrent(user);
                HomeActivity.this.refreshUserData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Called when swiping the ranking-list
     */
    @Override
    public void onRefresh() {
        //show loading animation during loading
        swipeLayout.setRefreshing(true);

        // notify the view
        this.adapter.notifyDataSetChanged();
        refreshUserData();
        //stop loading animation during loading
        swipeLayout.setRefreshing(false);
    }

    /**
     * A  Wrapper for a {@link User}. It's used by the RecyclerView
     */
    public static class UserHolder extends RecyclerView.ViewHolder{

        final TextView txtRank;
        final TextView txtName;
        final TextView txtPoints;

        public UserHolder(View v) {
            super(v);
            txtRank = (TextView) v.findViewById(R.id.item_user_rank);
            txtName = ((TextView) v.findViewById(R.id.item_user_name));
            txtPoints = ((TextView) v.findViewById(R.id.item_user_score));
        }

        public void applyUser(User model,int position){

            txtRank.setText((position +1)+ ":");
            txtName.setText(model.name );
            txtPoints.setText(Integer.toString(model.points));
        }
    }
}
