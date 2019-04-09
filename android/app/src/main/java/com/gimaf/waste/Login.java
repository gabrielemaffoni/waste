package com.gimaf.waste;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * This view permits the user to log in to their account
 */

public class Login extends AppCompatActivity implements Button.OnClickListener {

    private TextInputEditText password_editText;
    private TextInputEditText email_editText;
    private TextInputLayout password_layout;
    private TextInputLayout email_layout;
    private Button login;
    private Button signup;
    private DatabaseReference reference;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        reference = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        password_layout = findViewById(R.id.password_field);
        email_layout = findViewById(R.id.email_field);
        password_editText = findViewById(R.id.password_edit_text);
        email_editText = findViewById(R.id.email_edit_text);
        login = findViewById(R.id.login_button);
        signup = findViewById(R.id.signup_button_signin);

        login.setOnClickListener(this);
        signup.setOnClickListener(this);

    }

    /**
     * This method checks if the user exists and log them in. If so, they will go directly to the main page.
     *
     * @param password_text Password got from the field
     * @param email_text Email got from the field
     */
    private void logUserIn(String password_text, String email_text) {

        // Uses the Firebase Auth method .signInWithEmailAndPassword
        auth.signInWithEmailAndPassword(email_text, password_text).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    goToMainPage(auth.getCurrentUser());
                } else {
                    Log.w("Login problems", task.getException());
                    Toast.makeText(getApplicationContext(), "It appears there is some problems: ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * At the beginning checks if the user is already logged in. If so, they redirect them directly to the main page
     */
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = auth.getCurrentUser();
        sendToMainPage(currentUser);
    }

    /**
     * Simply checks if the user is already logged in
     * @param currentUser The user variable who should be logged in
     */

    public void sendToMainPage(FirebaseUser currentUser) {
        if (currentUser != null) {
            goToMainPage(currentUser);
        }
    }

    /**
     * Sends to the main page the user. Finishes this activity. Sends as extra the current user
     * @param currentUser The logged in user
     */
    private void goToMainPage(FirebaseUser currentUser) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("CURRENT_USER", currentUser);
        startActivity(intent);
        finish();
    }

    /**
     * ON button click checks what's been clicked
     * @param v the button
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == login.getId()) {
            signInButton();
        } else if (id == signup.getId()) {
            goToSignup();
        }
    }

    /***
     * If the sign in button is pressed, checks that the body inside the email is not empty.
     * If so, it checks the data, otherwise it shows a
     * Toast which will just tell the user to fill the fields
     *
     * TODO: To improve UX Highglight the empty fields in case they become so.
     */
    public void signInButton() {
        if (!password_editText.getText().toString().isEmpty() && !email_editText.getText().toString().isEmpty()) {
            logUserIn(password_editText.getText().toString(), email_editText.getText().toString());
        } else {
            Toast.makeText(getApplicationContext(), "Please fill all data!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * If the user presses the button "Sign up" they will just go to sign up view
     */
    private void goToSignup() {
        Intent signupIntent = new Intent(Login.this, Signup.class);
        startActivity(signupIntent);
    }
}
