package com.yinyoupoet.seekbyscan;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    ImageView loginBgImg;
    ImageView back;
    Button login;
    TextView register;
    EditText userName;
    EditText userPwd;

    String lName;
    String lPwd;
    String lMac;
    String lPosition;
    String lTime;

    private Handler mHandle;   //在子线程中异步更新主UI
    ProgressDialog dialog;

    private MyDataBaseHelper dbHelper;      //数据库的那个Helper
    SQLiteDatabase db;                      //获取到的数据库对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //初始化
        init();
    }


    private void init(){
        loginBgImg = findViewById(R.id.login_bgimg);
        Glide.with(this).load(R.drawable.login_bgimg).into(loginBgImg);
        login = findViewById(R.id.login_btn_login);
        register = findViewById(R.id.login_tv_register);
        back = findViewById(R.id.backToSeek);
        userName = findViewById(R.id.login_et_userName);
        userPwd = findViewById(R.id.login_et_userPwd);
        dialog = new ProgressDialog(this);

        dbHelper = new MyDataBaseHelper(this,"localInfo.db",null,1);
        db = dbHelper.getWritableDatabase();


        mHandle = new Handler(Looper.getMainLooper());

        //region 返回按钮点击事件
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginActivity.this.finish();
            }
        });
        //endregion

        //region 注册按钮点击事件
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });
        //endregion

        //region 登录按钮点击事件
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
                String name = userName.getText().toString().trim();
                String pwd = userPwd.getText().toString().trim();

                //doLogin()函数不能重用，因为如果返回bool类型的话，由于异步，会导致出现问题
                doLogin(name,pwd);
                //Toast.makeText(LoginActivity.this,"用户名或密码不正确",Toast.LENGTH_LONG).show();
            }
        });
        //endregion

    }

    //region 判断用户名密码是否正确，如果正确，修改本地数据库，并设置登录状态为true，并跳转
    private void doLogin(String name,String pwd){
        FormBody formBody = new FormBody.Builder()
                .add("userName",name)
                .add("pwd",pwd)
                .build();
        //创建一个request
        Request request = new Request.Builder()
                .url(OkHttpUtils.BASE_URL+"login")
                .post(formBody)
                .build();
        Log.e("联网URL", OkHttpUtils.BASE_URL+"login" );
        //发起异步请求，并加入回调
        OkHttpUtils.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandle.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this,"网络连接错误，请重试",Toast.LENGTH_LONG).show();
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
                                mHandle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        OkHttpUtils.isLogin = true;

                                        //region 修改本地数据库
                                        MyDataBaseHelper dbHelper = new MyDataBaseHelper(LoginActivity.this,"localInfo.db",null,1);
                                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                                        String sql = "delete from userInfo";
                                        db.execSQL(sql);
                                        sql = "insert into userInfo(userName,userPwd) values('"+userName.getText().toString().trim()+"','"+userPwd.getText().toString().trim()+"')";
                                        db.execSQL(sql);
                                        //endregion

                                        dialog.dismiss();

                                        lName = userName.getText().toString().trim();
                                        lPwd = userPwd.getText().toString().trim();
                                        updateOnlinePosition(lName,lPwd,lMac,lPosition,lTime);

                                        Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }else{
                                mHandle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        dialog.dismiss();
                                        Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }else{
                            mHandle.post(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_LONG).show();
                                }
                            });
                        }



                    } catch (JSONException e) {
                        e.printStackTrace();
                        mHandle.post(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }else{
                    mHandle.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(LoginActivity.this,"网络连接错误，请重试",Toast.LENGTH_LONG).show();
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
        Request request = new Request.Builder()
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
        lMac = MacUtils.getMacAddr();
    }
    //endregion


}
