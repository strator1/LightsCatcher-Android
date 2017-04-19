package com.hs_augsburg_example.lightscatcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.hs_augsburg_example.lightscatcher.camera.TakePictureActivity;
import com.hs_augsburg_example.lightscatcher.dataAccess.FirebaseAdapter;
import com.hs_augsburg_example.lightscatcher.dataModels.User;

public class HomeActivity extends AppCompatActivity {

    private ArrayAdapter<User> adapter = null;
    private DatabaseReference usersDatabase = null;
    private Query top10 = null;
    private ChildEventListener listener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToCamera();
            }
        });

        // Get ListView object from xml
        final ListView listView = (ListView) findViewById(R.id.view_userRanking);

        // Create a new Adapter
        adapter = new FirebaseAdapter(this, R.layout.list_item_user,);

        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // Connect to the Firebase database and get reference to users
        usersDatabase = FirebaseDatabase.getInstance().getReference("users");
        top10 = usersDatabase.orderByChild("points").limitToFirst(10);

        // Assign a listener to detect changes to the top10
        this.listener = new ChildEventListener() {

            // This function is called once for each child that exists
            // when the listener is added. Then it is called
            // each time a new child is added.
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                User value = dataSnapshot.getValue(User.class);
                adapter.add(value);
            }

            // This function is called each time a child item is removed.
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                User value = dataSnapshot.getValue(User.class);
                adapter.remove(value);
            }

            // The following functions are also required in ChildEventListener implementations.
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {

            }

            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                //Log.w("TAG:", "Failed to read value.", error.toException());
            }
        };

        top10.addChildEventListener(listener);
    }

    private void refreshUserItems(){

    }
    private void navigateToCamera() {
        Intent intent = new Intent(this, TakePictureActivity.class);
        startActivity(intent);
    }

}
