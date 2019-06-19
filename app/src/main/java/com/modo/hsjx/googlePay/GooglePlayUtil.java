package com.modo.hsjx.googlePay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.modo.hsjx.content.EventBusContents;
import com.modo.hsjx.util.ConsoleLogUtil;
import com.modo.hsjx.util.IabBroadcastReceiver;
import com.modo.hsjx.util.IabHelper;
import com.modo.hsjx.util.IabResult;
import com.modo.hsjx.util.Inventory;
import com.modo.hsjx.util.LogUtil;
import com.modo.hsjx.util.Purchase;
import com.modo.hsjx.widget.MyAlertDialog;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by hyk on 2018/11/7.
 */

public class GooglePlayUtil implements IabBroadcastReceiver.IabBroadcastListener{

    private final String TAG = GooglePlayUtil.class.getSimpleName();

    private String SKU;

    private Context mContext;


    private String base64EncodedPublicKey;
    public static final int RC_REQUEST = 10001;

    public List<String> mSkuList;

    //当购买的时候会用通知
    IabBroadcastReceiver mBroadcastReceiver;

    private IabHelper mHelper;
    private Inventory mInventory;
    private Purchase mPurchase;

    /**
     * 是否循环去请求sku，如果是刚开始初始化也就是APP刚起来的时候要去循环，看看有没有中断掉的订单
     * 如果直接购买后的话就不用去循环了，直接去消费掉就好了
     */
    private boolean mIsLooperSku = true;

    private final int LOOPER_SDK_SING = 0x002;
    private final int LOOPER_TIME = 3000;
    private int mLooperTimes = 1;
    private final int NOTIFY_PAY = 0x003;
    private final int MAX_LOOPER_TIME = 10;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case LOOPER_SDK_SING:
                    LogUtil.e(TAG,"轮询去请求SDK验证："+mLooperTimes);
                    if(mLooperTimes<=MAX_LOOPER_TIME){
                            mLooperTimes++;
                            Purchase purchase = (Purchase) msg.obj;
                            //发送定时去一直请求服务器验证的接口，直到服务器返回为止
                            Message message = Message.obtain();
                            message.obj = purchase;
                            message.what = LOOPER_SDK_SING;

                            postLooperSign(purchase);
                            mHandler.sendMessageDelayed(message,LOOPER_TIME*mLooperTimes);
                    }
                    break;
                case NOTIFY_PAY:
                    if(mLooperTimes<=MAX_LOOPER_TIME ){
                        mLooperTimes++;
                        Purchase purchase = (Purchase) msg.obj;
                        //发送定时去一直请求服务器验证的接口，直到服务器返回为止
                        Message message = Message.obtain();
                        message.obj = purchase;
                        message.what = NOTIFY_PAY;

                        mHandler.sendMessageDelayed(message,LOOPER_TIME*mLooperTimes);
                        postNotifySign(purchase);
                    }
                    break;

                default:
                    break;
            }
            return false;
        }
    });

    public GooglePlayUtil(Context context, String publicKey, List<String> skuList){
        this.mContext = context;
        this.base64EncodedPublicKey = publicKey;
        this.mSkuList = skuList;
        init();
    }

    public void buy(Activity activity, String sku, String payload){
        this.SKU = sku;
        try {
            mIsLooperSku = false;
            mHelper.launchPurchaseFlow(activity, SKU, RC_REQUEST,mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
//            complain("Error launching purchase flow. Another async operation in progress.");
            LogUtil.e(TAG,"购买失败，有库存,去消耗库存吧");
        }catch (IllegalStateException e){
            complain(e.getMessage());
        }
    }

    /**
     * 取消去sdk轮询
     */
    public void cancleLooperSdkSign(){
        mLooperTimes = 1;
        mHandler.removeMessages(LOOPER_SDK_SING);
    }

    public void cancleNotifyPay(){
        mLooperTimes = 1;
        mHandler.removeMessages(NOTIFY_PAY);
    }

    private void init(){
        mHelper = new IabHelper(mContext, base64EncodedPublicKey);
        mHelper.enableDebugLogging(true);

        mIsLooperSku = true;
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
//                    complain("Problem setting up in-app billing: " + result);
                    ConsoleLogUtil.logE("和in-app billing建立连接出现问题："+result,ConsoleLogUtil.LOG_TYPE_SERVICE,1);
//                    complain("很抱歉該機型不支持谷歌服務");
                    MyAlertDialog mDialog = new MyAlertDialog(mContext).builder()

                            .setTitle("提示")
                            .setMsg("您的設備不支持谷歌服務，無法使用本軟體")
                            .setNegativeButton("确定", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    postExit();
                                }
                            });
                    mDialog.show();
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(GooglePlayUtil.this);
//                监听所购买的东西是否有变动的receiver
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                mContext.getApplicationContext().registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
//                请求存货
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    /**
     * 获取SKU的库存
     * @param sku
     */
    private void getInventoryPurchase(String sku){
        Purchase gasPurchase = mInventory.getPurchase(sku);
        if (gasPurchase != null) { //&& verifyDeveloperPayload(gasPurchase) demo中有这个需要校验开发者设置的能够购买几个，最多能有几个库存的意思吧
            Log.d(TAG, "有库存，消耗掉");
            //todo 请求sdk：验证商品是否合法，等待sdk如果合法的话就消耗掉
            postNotifySign(gasPurchase);
            Message message = Message.obtain();
            message.obj = gasPurchase;
            message.what = NOTIFY_PAY;
            mHandler.sendMessageDelayed(message,LOOPER_TIME);
        }
    }

    /**
     * 消费掉库存,这个是主动调用
     * @param sku
     */
    public void consumeAsync(String sku, String orderId){
        try {
            if(mPurchase!=null){
                //校验订单号和谷歌的附加字段是否是同一个订单号，是的话才消费
                if(orderId.equals(mPurchase.getDeveloperPayload())){
                    mHelper.consumeAsync(mPurchase, mConsumeFinishedListener);
                }
                return;
            }
            Purchase purchase = mInventory.getPurchase(sku);
            if(purchase == null){
                ConsoleLogUtil.logE("服务器让消费的sku不存在,sku:"+sku,ConsoleLogUtil.LOG_TYPE_PAY,5);
                LogUtil.e(TAG,"服务器让消费的sku不存在,sku:"+sku);
                return;
            }
            //校验订单号和谷歌的附加字段是否是同一个订单号，是的话才消费
            if(orderId.equals(purchase.getDeveloperPayload())){
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            }
        } catch (IabHelper.IabAsyncInProgressException e) {

        }
    }



    //请求存货的时候的监听
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            mInventory = inventory;
            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            if(mIsLooperSku){
                LogUtil.e(TAG,"初始化查询库存");
                //循环查看有没有没有消费掉的订单，如果有的话就重新请求服务器然后进行消费
                // Check for gas delivery -- if we own gas, we should fill up the tank immediately
                if(mSkuList == null || mSkuList.size()<1){
                    return;
                }
                for (int i = 0; i < mSkuList.size(); i++) {
                    if(!TextUtils.isEmpty(mSkuList.get(i))){
                        getInventoryPurchase(mSkuList.get(i));
                    }
                }
            }else{
                getInventoryPurchase(SKU);
            }

            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };



    @Override
    public void receivedBroadcast() {
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
//            complain("Error querying inventory. Another async operation in progress.");
            complain("购买失败： Another async operation in progress.");
        }
    }
    /**
     *Callback for when a purchase is finished,购买完成后的回调
      */
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Gson gson = new Gson();
            String s = gson.toJson(purchase);
            Log.d(TAG, "购买结果: " + result + ", 购买的东西: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                payFailed();
                if(7 == result.getResponse()){
//                    LogUtil.e(TAG,"还有库存，请求服务器去消耗掉");
                    mIsLooperSku = false;
                    try {
                        mHelper.queryInventoryAsync(mGotInventoryListener);
                    } catch (IabHelper.IabAsyncInProgressException exception) {
                        LogUtil.e(TAG,"请求库存失败:"+exception.getMessage());
                    }
                    return;
                }
                if(result.getMessage().contains("response: -1002:Bad response received")){
                    MyAlertDialog mDialog = new MyAlertDialog(mContext).builder()
                            .setTitle("提示")
                            .setMsg("請去：設置-->應用中打開“谷歌商店”的彈窗權限，然後重試")
                            .setNegativeButton("确定", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            });
                    mDialog.show();
                    return;
                }
                ConsoleLogUtil.logE("购买失败，msg："+result.getMessage()+",sku:"+SKU,ConsoleLogUtil.LOG_TYPE_PAY,6);
                complain("購買失敗");
                return;
            }

            LogUtil.i(TAG, "购买成功："+purchase.getSku());
            mIsLooperSku = false;
            SKU = purchase.getSku();
            mPurchase = purchase;
            //todo 購買成功后請求sdk然後進行消費
                //发送定时去一直请求服务器验证的接口，直到服务器返回为止
                Message message = Message.obtain();
                message.obj = purchase;
                message.what = LOOPER_SDK_SING;
                mHandler.sendMessageDelayed(message,LOOPER_TIME);
                postLooperSign(purchase);

        }
    };


    /**
     * Called when consumption is complete,消费掉的回调
      */
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "消费成功");
                ConsoleLogUtil.logI("消费成功",ConsoleLogUtil.LOG_TYPE_PAY,4);
//                complain("消费成功");
            }else {
//                complain("Error while consuming: " + result);
                ConsoleLogUtil.logE("消费失败："+result,ConsoleLogUtil.LOG_TYPE_PAY,04);
                complain("消费失败："+result);
            }

            mPurchase = null;
            Log.d(TAG, "End consumption flow.");
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...

        }else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }


    private void complain(String msg){
        Toast.makeText(mContext,msg, Toast.LENGTH_SHORT).show();
    }

//    private PurchaseCallback mPurchaseCallback;
//    public interface PurchaseCallback{
//        void purchaseSuccess(Purchase purchase);
//    }

    private void postLooperSign(Purchase purchase){
        Message message = Message.obtain();
        message.obj = purchase;
        message.what = EventBusContents.LOOPER_SDK_SING;
        EventBus.getDefault().post(message);
    }

    private void postNotifySign(Purchase purchase){
        Message message = Message.obtain();
        message.obj = purchase;
        message.what = EventBusContents.NOTIFY_SIGN;
        EventBus.getDefault().post(message);
    }

    private void postExit(){
        Message message = Message.obtain();
        message.obj = "关闭系统";
        message.what = EventBusContents.EXIT_SYS;
        EventBus.getDefault().post(message);
    }
    private void payFailed(){
        Message message = Message.obtain();
        message.obj = "支付失败";
        message.what = EventBusContents.PAY_FAILED;
        EventBus.getDefault().post(message);
    }

}
