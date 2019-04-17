/*
* ForgotPassActivity.java
* Forgot Pass Activity allows the user to reset their pass if they remember the answers
* to their security questions.
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
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.t2.fcads.FipsWrapper;
import com.t2.vhb.home.HomeActivity;

public class ForgotPassActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pass);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        final Button submitButton = (Button) findViewById(R.id.submitButton);
        final TextView resetPassQ2 = (TextView) findViewById(R.id.resetPassQ2);
        final TextView resetPassQ1 = (TextView) findViewById(R.id.resetPassQ1);

        final EditText resetPassQ1Answer = (EditText) findViewById(R.id.forgotPassQuestion1Answer);
        final EditText resetPassQ2Answer = (EditText) findViewById(R.id.forgotPassQuestion2Answer);
        final EditText newPass = (EditText) findViewById(R.id.newPasswordText);

        //Sets the text to the questions the user defined on setup
        resetPassQ1.setText(PreferenceHelper.getQuestion1());
        resetPassQ2.setText(PreferenceHelper.getQuestion2());


        submitButton.setOnClickListener(v -> {
            String resetPassQ1AnswerText = resetPassQ1Answer.getText().toString();
            String resetPassQ2AnswerText = resetPassQ2Answer.getText().toString();
            String newPassText = newPass.getText().toString();
            String answerString = resetPassQ1AnswerText+resetPassQ2AnswerText;


            if(FipsWrapper.getInstance(getApplicationContext()).doCA(answerString) == 0) {
                FipsWrapper.getInstance(getApplicationContext())
                        .doCPUA(newPassText, answerString);
                moveToNextScreen();

            } else {

                if(!checkValidPassword(newPassText)) {
                    newPass.setError(
                            Html.fromHtml("<font color='red'>Password must be at least 4 characters!</font>"));
                }

                if(!resetPassQ1AnswerText.equals(PreferenceHelper.getQuestion1Answer()))
                    resetPassQ1Answer.setError(
                            Html.fromHtml("<font color='red'>You entered an incorrect answer, please try again!</font>"));

                if(!resetPassQ2AnswerText.equals(PreferenceHelper.getQuestion2Answer()))
                    resetPassQ2Answer.setError(
                            Html.fromHtml("<font color='red'>You entered an incorrect answer, please try again!</font>"));
            }
        });
    }

    private void moveToNextScreen() {
        this.startActivity(new Intent(this, HomeActivity.class));
        this.finish();

    }

    private boolean checkValidPassword(String p) {
        return p.length() >= 4;
    }

}
