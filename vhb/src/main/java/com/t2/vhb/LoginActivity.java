/*
* LoginActivity.java
* Login activity allows the user to login using either their fingerprint or their passcode.
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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.t2.fcads.FipsWrapper;
import com.t2.vhb.home.HomeActivity;

public class LoginActivity extends Activity {

    private FingerprintManagerCompat fManager;
    private AlertDialog fingerprintDialog;
    private AlertDialog fingerprintNotSetUpDialog;
    private final CancellationSignal cancelSignal = new CancellationSignal();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        final Button fingerprintLogin = (Button)findViewById(R.id.fingerprintBtn);
        fingerprintLogin.setVisibility(View.INVISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintLogin.setVisibility(View.VISIBLE);
            fingerprintDialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.unlock_via_fingerprint))
                    .setMessage(getString(R.string.security_fingerprint_text))
                    .setNegativeButton("Cancel", (dialog, which) -> cancelSignal.cancel())
                    .create();

            fingerprintNotSetUpDialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.unlock_via_fingerprint))
                    .setMessage(getString(R.string.security_fingerprint_notsetup_text))
                    .create();

            fManager = FingerprintManagerCompat.from(this);

            fingerprintLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(fManager.isHardwareDetected() && fManager.hasEnrolledFingerprints()) {
                        fingerprintDialog.show();

                        fManager.authenticate(null, 0, cancelSignal, new FingerprintManagerCompat.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                                super.onAuthenticationHelp(helpCode, helpString);
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                            }

                            @Override
                            public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                fingerprintDialog.dismiss();
                                moveToNextScreen();
                            }

                            @Override
                            public void onAuthenticationError(int errorCode, CharSequence errString) {
                                super.onAuthenticationError(errorCode, errString);
                            }
                        }, null);
                    }
                    else {
                        fingerprintNotSetUpDialog.show();

                    }
                }
            });
        }


        final EditText passwordField = (EditText) findViewById(R.id.passwordText);
        final Button unlockButton = (Button) findViewById(R.id.unlockBtn);
        final Button forgotPassButton = (Button) findViewById(R.id.forgotPassBtn);

        unlockButton.setOnClickListener(v -> {
            int successful = FipsWrapper.getInstance(getApplicationContext())
                    .doCP(passwordField.getText().toString());

            if (successful == 0) {
                moveToNextScreen();
            } else {
                showIncorrectPasswordDialog();
                passwordField.setText("");
            }
        });

        forgotPassButton.setOnClickListener(v -> {
            LoginActivity.this.startActivity(new Intent(LoginActivity.this, ForgotPassActivity.class));
            LoginActivity.this.finish();
        });

    }

    private void showIncorrectPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You entered an incorrect password")
                .setPositiveButton("OK", (dialog, id) -> {

                });

        builder.create().show();
    }

    private void moveToNextScreen() {
        this.startActivity(new Intent(this, HomeActivity.class));
        this.finish();
    }

}
