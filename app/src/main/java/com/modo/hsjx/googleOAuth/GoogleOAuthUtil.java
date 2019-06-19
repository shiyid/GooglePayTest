package com.modo.hsjx.googleOAuth;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.modo.hsjx.util.ConsoleLogUtil;
import com.modo.hsjx.util.LogUtil;

/**
 * Created by hyk on 2018/11/12.
 *
 * 谷歌登录授权
 */

public class GoogleOAuthUtil {

    private Activity mActivity;
    GoogleSignInClient mGoogleSignInClient;
    public static int RC_SIGN_IN = 0x001;
    private final String TAG = GoogleOAuthUtil.class.getSimpleName();

//    private String clienId = "284005743049-d7gka6fv96bvbntlvhecm4tads367a19.apps.googleusercontent.com";
//    private String clienId = "284005743049-om4ahdjhbrdb6eraa9tcjckssvqfs88t.apps.googleusercontent.com";
//    private String clienId = "284005743049-ih4qq6vv7c49gh8bqeqtvggu7vljtd7u.apps.googleusercontent.com";

    private String mClientId;
    private boolean mIsUseClientId;
    public GoogleOAuthUtil(Activity context, String clientId, boolean isUseClientId, LoginCallback loginCallback){
        this.mActivity = context;
        this.mLoginCallback = loginCallback;
        mClientId = clientId;
        mIsUseClientId = isUseClientId;
        ConsoleLogUtil.logI("谷歌登录clienId："+mClientId+",是否启用clientId:"+mIsUseClientId,ConsoleLogUtil.LOG_TYPE_LOGIN,0);
    }

    /**
     * 在activity的oncreate方法里面调用
     */
    public void create(){
        // Request only the user's ID token, which can be used to identify the
        // user securely to your backend. This will contain the user's basic
        // profile (name, profile picture URL, etc) so you should not need to
        // make an additional call to personalize your application.
        GoogleSignInOptions gso;
        if(mIsUseClientId){
            gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(mClientId)
                    .requestEmail()
                    .build();
        }else{
            gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
        }
//        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(clienId)
//                .requestEmail()
//                .build();

//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(mActivity.getApplicationContext(), gso);
    }

    /**
     * 一般在activity的onStart里面调用
     */
    public void start(){
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        signIn();
        //如果已经有登录过的话，就返回已经登录的账号，如果没有登录的话就返回空
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mActivity.getApplicationContext());
//        if (account != null) {
//            if(mLoginCallback == null){
//                Log.e(TAG,"mLoginCallback不能为空");
//                return;
//            }
//            mLoginCallback.loginSuccess(account);
//        }else{
//            //如果没有登录过的话就去登录
//            signIn();
//        }
//        updateUI(account);

//        GoogleSignIn.().addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
//                    @Override
//                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
//                        handleSignInResult(task);
//                    }
//                });
    }

    public void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        mActivity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if(mLoginCallback == null){
                Log.e(TAG,"mLoginCallback不能为空");
                return;
            }
            mLoginCallback.loginSuccess(account);
            ConsoleLogUtil.logI("登录成功,id:"+account.getId()+",整体："+ new Gson().toJson(account),ConsoleLogUtil.LOG_TYPE_LOGIN,6);
            Log.i(TAG,"登录成功："+account.getIdToken());
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            LogUtil.e(TAG, "谷歌登录失败:failed code=" + e.getStatusCode()+",msg:"+e.toString());
            ConsoleLogUtil.logE( "谷歌登录失败，code：" + e.getStatusCode()+",msg:"+e.toString(),ConsoleLogUtil.LOG_TYPE_LOGIN,06);
            if(mLoginCallback != null){
                mLoginCallback.loginFailed("谷歌登录失败，code：" + e.getStatusCode()+",msg:"+e.toString());
            }
        }
    }


    private LoginCallback mLoginCallback;
    public interface LoginCallback{
        void loginSuccess(GoogleSignInAccount account);
        void loginFailed(String msg);
    }
}
