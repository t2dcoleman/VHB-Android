package com.t2.vhb;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.t2.fcads.FipsWrapper;
import com.t2.vhb.db.VhbProvider;
import com.t2.vhb.home.EulaActivity;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends Activity implements OnClickListener {
    private Timer timer = new Timer();
    private Handler.Callback startHandler = msg -> {
        startNextActivity();
        return true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_splash);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        FipsWrapper fw = new FipsWrapper();
        fw.ctoStart();

        if(Global.databaseHelper == null)
        {
            Global.databaseHelper = new VhbProvider();
            Global.context = this;
        }


        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                startHandler.handleMessage(null);
            }
        }, 5000);
    }

    private void startNextActivity() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean eulaAccepted = prefs
                .getBoolean(getString(R.string.pref_setup_eula_accepted), false);

        if(!eulaAccepted) {
            this.startActivity(new Intent(this, EulaActivity.class));
            this.finish();
        } else if(FipsWrapper.getInstance(getApplicationContext()).doIsL() == 1) {
            this.startActivity(new Intent(this, LoginActivity.class));
            this.finish();
        } else {
            this.startActivity(new Intent(this, CreateLoginActivity.class));
            this.finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onClick(View arg0) {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        startNextActivity();
    }
}
