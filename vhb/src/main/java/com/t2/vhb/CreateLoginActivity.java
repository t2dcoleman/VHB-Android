/*
* CreateLoginActivity.java
* Create Login activity prompts the user to setup security features in our app.
*
* Created by Chris Fahlin.
*
* Positive Activity Jackpot
*
* Copyright  2009-2017 United States Government as represented by
* the Chief Information Officer of the National Center for Telehealth
* and Technology. All Rights Reserved.
*
* Copyright  2009-2017 Contributors. All Rights Reserved.
*
* THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE,
* REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN
* COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT
* AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY").
* THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN
* INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR
* REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES,
* DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED
* HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE
* RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
*
* Government Agency: The National Center for Telehealth and Technology
* Government Agency Original Software Designation: Positive Activity Jackpot
* Government Agency Original Software Title: Positive Activity Jackpot
* User Registration Requested. Please send email
* with your contact information to: robert.a.kayl.civ@mail.mil
* Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
*
*/
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


public class CreateLoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_login);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        final Button saveButton = (Button) findViewById(R.id.saveSecurityBtn);

        //Passwords
        final EditText passwordField = (EditText) findViewById(R.id.setupPasswordText);
        final EditText passwordConfirmationField = (EditText) findViewById(R.id.setupPasswordConfirmText);

        //Question1
        final EditText question1= (EditText) findViewById(R.id.question1Text);
        final EditText question1Answer = (EditText) findViewById(R.id.question1Answer);

        //Question2
        final EditText question2 = (EditText) findViewById(R.id.question2Text);
        final EditText question2Answer = (EditText) findViewById(R.id.question2Answer);


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
