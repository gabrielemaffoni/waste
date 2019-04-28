package com.gimaf.waste;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.*;
import androidx.appcompat.*;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Similar to sign in, this view will just create a new user on the database
 */
public class Signup extends AppCompatActivity implements Button.OnClickListener {
    private EditText email;
    private EditText password;
    private Button signup;
    private Button signin;
    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        auth = FirebaseAuth.getInstance();
        email = findViewById(R.id.email_signup);
        password = findViewById(R.id.password_signup);
        signup = findViewById(R.id.signup_button);
        signin = findViewById(R.id.signin_button_signup);

        signup.setOnClickListener(this);
        signin.setOnClickListener(this);

    }

    /**
     * Checks the current user at the beginning
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        checkIfUserExist(currentUser);
    }

    /**
     * Checks which button is clicked and acts as consequence
     * @param v  The clicked button
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == signin.getId()) {
            goToLogin();
        } else if (id == signup.getId()) {
            createNewAccount();
        }
    }

    /**
     * If they press on "Login" we will send them at the login page
     */
    public void goToLogin() {
        Intent loginIntent = new Intent(this, Login.class);
        startActivity(loginIntent);
        finish();
    }

    public void goToMainPage(FirebaseUser user) {
        Intent mainPage = new Intent(this, MainActivity.class);
        startActivity(mainPage);
        finish();
    }

    /**
     * If the user does exist, we'll send them to the main page
     * @param user
     */
    public void checkIfUserExist(FirebaseUser user) {
        if (user != null) {
            goToMainPage(user);
        }
    }

    /**
     * Checks that all the fields are filled. if not, it avoids crashing by showing a Toast.
     */
    public void createNewAccount() {
        if (!email.getText().toString().isEmpty() && !password.getText().toString().isEmpty()) {
            createNewUser(email.getText().toString(), password.getText().toString());
        } else {
            Toast.makeText(Signup.this, "Please, fill all the data", Toast.LENGTH_SHORT).show();
        }
    }

    public void createNewUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // If the user is created
                if (task.isSuccessful()) {

                    Toast.makeText(Signup.this, "You created a new account!", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = auth.getCurrentUser();
                    try {
                        createDatabaseVoices(user);
                        goToMainPage(user);
                    } catch (NullPointerException exception) {
                        Log.w("DATABASE", exception.getLocalizedMessage());
                    }

                } else {
                    Log.w("Signup", task.getException());
                    Toast.makeText(Signup.this, "There is some problems creating an account", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Creates new items in the different databases.
     * This way we can handle there is no crash whatsoever when it comes to uploading data there.
     * @param user The user Just created.
     */

    private void createDatabaseVoices(FirebaseUser user) {
        database = FirebaseDatabase.getInstance().getReference();
        database.child("items").child(user.getUid()).child("number_of_items").setValue(0);
        database.child("users").child(user.getUid()).child("token").setValue(0);
    }
}
