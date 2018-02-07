package com.yinyoupoet.seekbyscan;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class RegisterActivity extends Activity {
    EditText userName;
    EditText userPwd;
    EditText confirmPwd;
    Button register;

    ProgressDialog dialog;

    String lName;
    String lPwd;
    String lMac;                //这已经不是Mac了，而是设备的唯一编码，IMEI on GSM, MEID for CDMA
    String lPosition;
    String lTime;

    String test;

    private MyDataBaseHelper dbHelper;      //数据库的那个Helper
    SQLiteDatabase db;                      //获取到的数据库对象


    private Handler mHandle;   //在子线程中异步更新主UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        init();
    }

    private void init(){

        //region 初始化toolbar
        Toolbar register_toolbar = (Toolbar) findViewById(R.id.register_toolbar);
        register_toolbar.setTitle("注册");
        register_toolbar.setNavigationIcon(R.drawable.chatlist_return);
        register_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //endregion

        dbHelper = new MyDataBaseHelper(this,"localInfo.db",null,1);
        db = dbHelper.getWritableDatabase();

        mHandle = new Handler(Looper.getMainLooper());

        dialog = new ProgressDialog(this);
        userName = findViewById(R.id.rg_userName);
        userPwd = findViewById(R.id.rg_userPwd);
        confirmPwd = findViewById(R.id.rg_userConfirm_Pwd);
        register = findViewById(R.id.btn_rg);

        getLocalInfo();


        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPwd()){
                    dialog.show();
                    doRegister();
                }
            }
        });
    }

    //region 检查两次输入的密码是否合法
    private Boolean checkPwd(){
        String pwd = userPwd.getText().toString().trim();
        String confirm = confirmPwd.getText().toString().trim();

        if(pwd.length() < 6){
            Toast.makeText(RegisterActivity.this,"密码长度至少为6哦，请重试",Toast.LENGTH_LONG).show();
            return false;
        }

        if(pwd.equals(confirm)){
            return true;
        }else{
            Toast.makeText(RegisterActivity.this,"两次输入的密码不一样呦，请重试",Toast.LENGTH_LONG).show();
            return false;
        }
    }
    //endregion

    //region 进行注册操作
    private void doRegister(){
        final String name = userName.getText().toString().trim();
        final String pwd = userPwd.getText().toString().trim();
        String confirm = confirmPwd.getText().toString().trim();

        FormBody formBody = new FormBody.Builder()
                .add("userName",name)
                .add("pwd",pwd)
                .build();
        //创建一个request
        Request request = new Request.Builder()
                .url(OkHttpUtils.BASE_URL+"register")
                .post(formBody)
                .build();
        //发起异步请求，并加入回调
        OkHttpUtils.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandle.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Toast.makeText(RegisterActivity.this,"Failure：联网失败，请重试",Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    try {
                        String s = response.body().string();
                        JSONObject jsonObject = new JSONObject(s);
                        if(jsonObject.has("valid")){
                            Boolean valid = jsonObject.getBoolean("valid");
                            if(valid){
                                //注册成功，将数据存入数据库，并跳转
                                mHandle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        MyDataBaseHelper dbHelper = new MyDataBaseHelper(RegisterActivity.this,"localInfo.db",null,1);
                                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                                        String sql = "delete from userInfo";
                                        db.execSQL(sql);
                                        sql = "insert into userInfo(userName,userPwd) values('"+name+"','"+pwd+"')";
                                        db.execSQL(sql);

                                        OkHttpUtils.isLogin = true;
                                        dialog.dismiss();

                                        lName=name;
                                        lPwd=pwd;

                                        updateOnlinePosition(lName,lPwd,lMac,lPosition,lTime);
                                        OkHttpUtils.isLogin = true;
                                        Intent intent = new Intent(RegisterActivity.this,MainActivity.class);
                                        startActivity(intent);

                                        finish();
                                    }
                                });
                            }else{
                                //注册失败
                                mHandle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        dialog.dismiss();
                                        Toast.makeText(RegisterActivity.this,"当前账号已存在，请重试",Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }else{
                            mHandle.post(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    Toast.makeText(RegisterActivity.this,"当前账号已存在，请重试",Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        dialog.dismiss();
                        e.printStackTrace();
                    }
                }else{
                    mHandle.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(RegisterActivity.this,"Response not success,联网失败，请重试",Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });


    }
    //endregion


    //region 更新在线信息
    private void updateOnlinePosition(String name,String pwd,String mac,String position,String time){
        Log.d("在线更新", "进行在线更新 ");

        //创建Form表单对象
        FormBody formBody = new FormBody.Builder()
                .add("userName",name)
                .add("pwd",pwd)
                .add("macAddress",mac)
                .add("position",position)
                .add("time",time)
                .build();
        //创建一个request
        final Request request = new Request.Builder()
                .url(OkHttpUtils.BASE_URL+"setInfo")
                .post(formBody)
                .build();
        //发起异步请求，并加入回调
        OkHttpUtils.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateOnlinePosition(lName,lPwd,lMac,lPosition,lTime);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){

                    try {
                        String s = response.body().string();
                        final JSONObject jsonObject = new JSONObject(s);
                        if (jsonObject.has("valid")){
                            test = jsonObject.getString("valid");
                            mHandle.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(RegisterActivity.this,test,Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    //endregion

    //region 获取本地信息
    private void getLocalInfo(){
        Cursor cursor;      //保存查询结果
        cursor = db.rawQuery("select * from carInfo", null);
        if(cursor != null && cursor.getCount() != 0){
            //如果不是空表
            cursor.moveToFirst();
            lPosition = cursor.getString(cursor.getColumnIndex("position"));
            lTime = cursor.getString(cursor.getColumnIndex("time"));
            if(lTime.equals("")){
                lTime = " ";
            }
            if(lPosition.equals("")){
                lPosition = " ";
            }
            cursor.close();
        }
        cursor.close();
        //lName = userName.getText().toString().trim();
        //lPwd = userPwd.getText().toString().trim();
        lMac = MacUtils.getImei();
    }
    //endregion



}
