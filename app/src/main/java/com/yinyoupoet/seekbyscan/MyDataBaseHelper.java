package com.yinyoupoet.seekbyscan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by hasee on 2017/11/14.
 */

public class MyDataBaseHelper extends SQLiteOpenHelper {
    public static final String CREATE_USERINFO = "create table userInfo(" +
            "userName varchar(20) UNIQUE," +
            "userPwd varchar(16) )";
    public static final String CREATE_CARINFO = "create table carInfo(" +
            "position varchar(255)," +
            "time varchar(100)," +
            "seqNum varchar(100) )";
    //本地数据库只包含一行数据，自启动
    private Context mContext;

    public MyDataBaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //新建一张表，存在则自动不执行
        db.execSQL(CREATE_USERINFO);
        db.execSQL(CREATE_CARINFO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
