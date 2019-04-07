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
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    private void logUserIn(String password_text, String email_text) {
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

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = auth.getCurrentUser();
        sendToMainPage(currentUser);
    }

    public void sendToMainPage(FirebaseUser currentUser) {
        if (currentUser != null) {
            goToMainPage(currentUser);
        }
    }

    private void goToMainPage(FirebaseUser currentUser) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("CURRENT_USER", currentUser);
        startActivity(intent);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == login.getId()) {
            signInButton();
        } else if (id == signup.getId()) {
            goToSignup();
        }
    }

    public void signInButton() {
        if (!password_editText.getText().toString().isEmpty() && !email_editText.getText().toString().isEmpty()) {
            logUserIn(password_editText.getText().toString(), email_editText.getText().toString());
        } else {
            Toast.makeText(getApplicationContext(), "Please fill all data!", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToSignup() {
        Intent signupIntent = new Intent(Login.this, Signup.class);
        startActivity(signupIntent);
    }
}
