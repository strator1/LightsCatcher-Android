package com.hs_augsburg_example.lightscatcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.hs_augsburg_example.lightscatcher.camera.TakePictureActivity;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;

public class HomeActivity extends AppCompatActivity {

    private FirebaseListAdapter<User> adapter = null;
    private DatabaseReference usersDatabase = null;
    private Query top10 = null;

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

        ActivityRegistry.register(this);

        // Connect to the Firebase database and query top10 users
        usersDatabase = FirebaseDatabase.getInstance().getReference("users");
        top10 = usersDatabase.orderByChild("points");

        // Create a new Adapter
        adapter = new FirebaseListAdapter<User>(this, User.class, R.layout.item_user, top10) {
            @Override
            protected void populateView(View v, User model, int position) {
                try {
                    ((TextView) v.findViewById(R.id.item_user_name)).setText(model.name);
                    ((TextView) v.findViewById(R.id.item_user_score)).setText(Integer.toString(model.points));
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        };



        // Assign adapter to ListView
        final ListView listView = (ListView) findViewById(R.id.view_userRanking);
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

    private void refreshUserItems() {

    }

    private void navigateToCamera() {
        Intent intent = new Intent(HomeActivity.this, TakePictureActivity.class);
        startActivity(intent);
    }

}
