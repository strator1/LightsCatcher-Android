package com.hs_augsburg_example.lightscatcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
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
    private DatabaseReference usersDatabase = null;
    private FirebaseAuth mAuth;
    private Query top10 = null;
    private TextView txtUserName;
    private TextView txtUserRank;
    private TextView txtUserScore;
    private SwipeRefreshLayout swipeLayout;

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

        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        fetchUser(UserInformation.shared.getUid());
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
        usersDatabase = FirebaseDatabase.getInstance().getReference("users");
        top10 = usersDatabase.orderByChild("points");

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
        User usr = UserInformation.shared.current;
        if(usr != null)
        {
            txtUserName.setText(usr.name);
            txtUserRank.setText("Rang: " + calculateRankOfUser(usr));
            txtUserScore.setText("(Punkte: " + usr.points + ")");
        }
        else
        {
            txtUserName.setText("-");
            txtUserRank.setText("-");
            txtUserScore.setText("-");
        }
    }

    private static int calculateRankOfUser(User usr){
        return -1;
    }
    private void refreshListItems() {
        //show loading animation during loading
        swipeLayout.setRefreshing(true);

        // notify the view
        this.adapter.notifyDataSetChanged();

        //stop loading animation during loading
        swipeLayout.setRefreshing(false);
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
        refreshListItems();
    }

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

            //rank of the user:

            txtRank.setText(position+ ":");
            txtName.setText(model.name );
            txtPoints.setText(Integer.toString(model.points));
        }
    }
}
