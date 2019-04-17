package com.t2.vhb;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.t2.fcads.FipsWrapper;
import com.t2.vhb.home.HomeActivity;

public class UpdateLoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_login);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
				WindowManager.LayoutParams.FLAG_SECURE);

        final Button saveButton = (Button) findViewById(R.id.saveUpdateSecurityBtn);

        //Passwords
        final EditText passwordField = (EditText) findViewById(R.id.updatePasswordText);
        final EditText passwordConfirmationField = (EditText) findViewById(R.id.updatePasswordConfirmText);

        //Question1
        final EditText question1= (EditText) findViewById(R.id.question1Update);
        final EditText question1Answer = (EditText) findViewById(R.id.question1AnswerUpdate);

        //Question2
        final EditText question2 = (EditText) findViewById(R.id.question2Update);
        final EditText question2Answer = (EditText) findViewById(R.id.question2AnswerUpdate);


        saveButton.setOnClickListener(v -> {

            String password = passwordField.getText().toString();
            String passwordConfirmation = passwordConfirmationField.getText().toString();

            if (!password.equals(passwordConfirmation) && checkValidPassword(password)) {
                showPasswordDialog();
                passwordField.setText("");
                passwordConfirmationField.setText("");
            }

            String question1Text = question1.getText().toString();
            String question1AnserText = question1Answer.getText().toString();

            String question2Text = question2.getText().toString();
            String question2AnserText = question2Answer.getText().toString();

            if (question1AnserText.isEmpty() || question2AnserText.isEmpty() ) {
                showSecurityQuestionDialog();
            }

            if(password.equals(passwordConfirmation) && checkValidPassword(password)
                    && !question1AnserText.isEmpty() && !question2AnserText.isEmpty()) {

                FipsWrapper.getInstance(getApplicationContext()).doDeInitL();

                FipsWrapper.getInstance(getApplicationContext())
                        .doInitL(password, question1AnserText + question2AnserText);

                PreferenceHelper.setQuestion1(question1Text);
                PreferenceHelper.setQuestion2(question2Text);


                PreferenceHelper.setQuestion1Answer(question1AnserText);
                PreferenceHelper.setQuestion2Answer(question2AnserText);

                moveToNextScreen();
            } else if (!checkValidPassword(password)) {
                passwordField.setError(Html.fromHtml("<font color='red'>Password must be at least 4 characters</font>"));
                passwordConfirmationField.setError(Html.fromHtml("<font color='red'>Password must be at least 4 characters</font>"));
            } else if (!password.equals(passwordConfirmation)) {
                passwordField.setError(Html.fromHtml("<font color='red'>Passwords must match!</font>"));
                passwordConfirmationField.setError(Html.fromHtml("<font color='red'>Passwords must match!</font>"));
            }
        });


    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("The passwords must match, and must have at least 4 characters!")
                .setPositiveButton("OK", (dialog, id) -> {

                });

        builder.create().show();
    }

    private void showSecurityQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You must enter an answer to each security question.")
                .setPositiveButton("OK", (dialog, id) -> {

                });

        builder.create().show();
    }

    private void moveToNextScreen() {
        this.startActivity(new Intent(this, HomeActivity.class));
        this.finish();

    }

    private boolean checkValidPassword(String p) {
        return p.length() >= 4;
    }

}
