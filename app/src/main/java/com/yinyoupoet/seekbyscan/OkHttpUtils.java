package com.yinyoupoet.seekbyscan;

import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by hasee on 2017/11/14.
 */

public class OkHttpUtils {
    public static boolean isLogin = false;     //这个用来检测是否需要自启动的
    public static int ID;              //这个是用来保存用户登录的ID的，所有activity都用得到
    static OkHttpClient mOKHttpClient;
    static final String BASE_URL = "http://47.94.212.1/SeekByScan_war/";

    static Response mResponse;                  //用来返回的response
    public static boolean refreshMajor = true;

    public static double longitude;             //经度
    public static double latitude;              //纬度

    public OkHttpUtils() {

    }

    public static OkHttpClient getClient() {
        if (mOKHttpClient == null) {
            mOKHttpClient = new OkHttpClient();
        }
        return mOKHttpClient;
    }

}