package com.modo.hsjx;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.modo.hsjx.googleOAuth.GoogleOAuthUtil;
import com.modo.hsjx.googlePay.GooglePlayUtil;

import org.greenrobot.eventbus.EventBus;

public class MainActivity extends AppCompatActivity {

    private GooglePlayUtil mGooglePlayUtil;
    private GoogleOAuthUtil mGoogleOAuthUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("result", "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        //谷歌支付
        if (requestCode == GooglePlayUtil.RC_REQUEST && mGooglePlayUtil != null) {
            mGooglePlayUtil.onActivityResult(requestCode, resultCode, data);
        }

        //谷歌登录
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == GoogleOAuthUtil.RC_SIGN_IN && mGoogleOAuthUtil!= null) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            mGoogleOAuthUtil.handleSignInResult(task);
        }
    }
}
