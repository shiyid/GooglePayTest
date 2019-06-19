package com.modo.hsjx.util;

import com.modo.hsjx.postLog.PostLogHttp;

/**
 * Created by hyk on 2018/11/23.
 */

public class ConsoleLogUtil {
    public static boolean mEnable = false;
    public static StringBuffer stringBuffer = new StringBuffer();

    public static final String LOG_TYPE_INSTALL= "install";
    public static final String LOG_TYPE_LOGIN = "login";
    public static final String LOG_TYPE_PAY = "pay";
    public static final String LOG_TYPE_NORMAL = "normal";
    public static final String LOG_TYPE_JS_ERROR = "jsError";
    public static final String LOG_TYPE_ERROR = "error";
    public static final String LOG_TYPE_SERVICE = "googleService";
    public static final String LOG_TYPE_TAG = "tag";

    public static void setEnable(boolean enable){
        mEnable = enable;
    }

    public static void logI(String msg){
        if(mEnable){
            stringBuffer.append(msg+"\n");
        }
        LogUtil.i("ConsoleLogUtil",msg);
    }
    public static void logI(String msg, String logType, Integer logOrder){
        if(mEnable){
            stringBuffer.append(msg+"\n");
        }
        LogUtil.i("ConsoleLogUtil",msg);
        PostLogHttp.postLog(msg,"INFO",logType,logOrder,"");
    }

    public static void logE(String msg){
        if(mEnable){
            stringBuffer.append(msg+"\n");
        }
        LogUtil.e("ConsoleLogUtil",msg);
    }
    public static void logE(String msg, String logType, Integer logOrder){
        if(mEnable){
            stringBuffer.append(msg+"\n");
        }
        LogUtil.e("ConsoleLogUtil",msg);
        PostLogHttp.postLog(msg,"ERROR",logType,logOrder,"");
    }
    public static void logInstall(int time, String adId, String deviceId){
        if(mEnable){
            stringBuffer.append("打开APP,times:"+time+",deviceId:"+deviceId+",adId:"+adId+"\n");
        }
        String extra = "{\"times\":"+time+",\"adId\":\""+adId+"\"}";
        LogUtil.e("ConsoleLogUtil","打开APP");
        PostLogHttp.postLog("打开APP","INFO",LOG_TYPE_INSTALL,21,extra);
    }
}
