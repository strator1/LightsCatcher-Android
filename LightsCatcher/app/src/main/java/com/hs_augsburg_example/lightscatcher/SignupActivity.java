package com.hs_augsburg_example.lightscatcher;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.ExceptionHelp;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;

public class SignupActivity extends AppCompatActivity {
    private static final boolean LOG = false;
    private EditText inputEmail, inputPassword, inputName;
    private Button btnSignIn, btnSignUp;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        ActivityRegistry.register(this);

        //Get Firebase auth instance

        btnSignIn = (Button) findViewById(R.id.sign_in_button);
        btnSignUp = (Button) findViewById(R.id.sign_up_button);
        inputName = (EditText) findViewById(R.id.name);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UserPreference.isUserBanned(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), getString(R.string.banned_message), Toast.LENGTH_SHORT).show();
                    return;
                }

                final String name = inputName.getText().toString().trim();
                final String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.enter_username), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.enter_password), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), getString(R.string.minimum_password), Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSignUp.setEnabled(false);

                progressBar.setVisibility(View.VISIBLE);

                //create user
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                btnSignUp.setEnabled(true);
                                progressBar.setVisibility(View.INVISIBLE);
                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {

                                    Exception e = task.getException();
                                    if (LOG && e != null) Log.e("LOGIN", e.toString(), e);

                                    String detail = ExceptionHelp.germanMsg(SignupActivity.this, e);
                                    Toast.makeText(SignupActivity.this, getString(R.string.error_registration_failed) + " " + detail, Toast.LENGTH_LONG).show();

                                    //
                                } else {
                                    // create database-entry for this user
                                    User usr = new User(task.getResult().getUser().getUid(), name, email);
                                    PersistenceManager.shared.persist(usr);

                                    startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                                    finish();
                                }
                            }
                        });

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void onPrivacyClick(View view) {
        startActivity(new Intent(this, DatenschutzActivity.class));
    }
}
