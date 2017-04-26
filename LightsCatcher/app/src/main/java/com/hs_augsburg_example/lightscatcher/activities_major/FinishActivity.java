package com.hs_augsburg_example.lightscatcher.activities_major;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;

public class FinishActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);

        ActivityRegistry.register(this);

        Button nextPicture = (Button) findViewById(R.id.button_nextPicture);
        nextPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FinishActivity.this, TakePictureActivity.class);
                startActivity(intent);
                finish();
            }
        });
        Button home = (Button) findViewById(R.id.button_home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FinishActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        Button quit = (Button) findViewById(R.id.button_quit);
        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FinishActivity.this.finish();
                Intent intent = new Intent(FinishActivity.this, HomeActivity.class);
                startActivity(intent);
                FinishActivity.this.moveTaskToBack(true);
            }
        });
    }
}
