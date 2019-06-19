package com.modo.hsjx.content;

/**
 * Created by hyk on 2018/11/15.
 */

public class EventBusContents {
    public static final int LOOPER_SDK_SING = 0x002;
    public static final int NOTIFY_SIGN = 0x003;
    //    关闭程序
    public static final int EXIT_SYS = 0x004;
    //支付失败
    public static final int PAY_FAILED = 0x005;

//    dialog错误code,太懒了不想重新弄个类，就写在这里了
    public static final String INIT_FAILED = "遊戲SDK初始化失敗，請重啟或者聯繫客服！錯誤碼101";
    public static final String ON_ERROR = "遊戲引擎初始化失敗，請重啟或者聯繫客服！錯誤碼102";
//    public static final String ON_JS_ERROR = "軟體錯誤，請重啟或者聯繫客服！錯誤碼103";
    public static final String GOOGLE_LOGIN_FAILED = "谷歌賬號登錄失敗，請重啟或者聯繫客服！錯誤碼104";
}
