package com.yinyoupoet.seekbyscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



import com.bumptech.glide.Glide;
import com.cazaea.sweetalert.SweetAlertDialog;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity{
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    static int REQUEST_CODE_SCAN = 99;
    Button scan;
    TextView position;
    TextView time;
    ImageView user;
    Button map;



    private MyDataBaseHelper dbHelper;      //数据库的那个Helper
    SQLiteDatabase db;                      //获取到的数据库对象
    String userName = "";                     //数据库查询到或者将保存的用户名
    String userPwd = "";                      //数据库查询到或者将保存的密码
    String curPosition = "";
    String curTime = "";
    String macAddress = "";

    //以下三个是联网获取到的信息
    String gPosition = "";
    String gTime = "";
    String gTip = "";

    private Handler mHandle;   //在子线程中异步更新主UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化控件
        init();
        mHandle = new Handler(Looper.getMainLooper());

        //检查是否已有账号，如果有账号就在线获取地址，否则就提示是否需要登录注册再在线获取地址
        checkAccount();

        //region 初始化扫描
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAccess();
            }
        });
        //endregion

    }

    //region 初始化
    private void init(){
        scan = findViewById(R.id.start);
        position = findViewById(R.id.position);
        time = findViewById(R.id.time);
        macAddress = MacUtils.getMacAddr();
        user = findViewById(R.id.userServer);
        map = findViewById(R.id.btn_map);

        user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(OkHttpUtils.isLogin){
                    Intent intent = new Intent(MainActivity.this,UserInfoActivity.class);
                    startActivity(intent);
                }else{
                    Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击地图
                //Intent intent = new Intent(MainActivity.this,NavigatorActivity.class);
                Intent intent = new Intent(MainActivity.this,NaviSecondActivity.class);
                startActivity(intent);
            }
        });

    }
    //endregion

    //region 检查是否已有账号
    private void checkAccount(){
        //初始化本地数据库
        initDataBase();

        //查询数据
        if(QueryUserInfo()){
            //显示上一次车位信息
            getLastPosition();
        }else{
            //如果没有账号信息,就询问是否登录
            showLogin();
            getLastPosition();
        }

    }
    //endregion

    //region 初始化本地数据库
    private void initDataBase(){
        dbHelper = new MyDataBaseHelper(this,"localInfo.db",null,1);
        db = dbHelper.getWritableDatabase();
    }
    //endregion

    //region 从数据库获取用户登录信息,如果未找到数据，则返回false
    private boolean QueryUserInfo(){
        Cursor cursor;      //保存查询结果
        cursor = db.rawQuery("select * from userInfo", null);
        if(cursor != null && cursor.getCount() != 0){
            //如果不是空表
            cursor.moveToFirst();
            userName = cursor.getString(cursor.getColumnIndex("userName"));
            userPwd = cursor.getString(cursor.getColumnIndex("userPwd"));

            //Toast.makeText(this,userName+":"+userPwd,Toast.LENGTH_LONG).show();

            //region 检查用户名密码是否正确，如果正确则点击用户会跳到用户信息页，否则是登录页
            doLogin(userName,userPwd);
            //endregion

            cursor.close();
            return true;
        }
        if(cursor != null) {
            cursor.close();
        }

        Toast.makeText(this,userName+":"+userPwd,Toast.LENGTH_LONG).show();
        doLogin(userName,userPwd);
        return false;
    }
    //endregion

    //region 询问是否登录
    private void showLogin(){
        final SweetAlertDialog sDialog = new SweetAlertDialog(MainActivity.this,SweetAlertDialog.WARNING_TYPE);
        sDialog.setTitleText("是否登录?")
                .setContentText("登陆后可以更好地保存车位信息哦")
                .setConfirmText("登录/注册")
                .setCancelText("暂不登录")
                .showCancelButton(true)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        //跳转到登录页
                        Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                        startActivity(intent);
                        sDialog.dismiss();
                    }
                }).show();
    }
    //endregion

    //region 获取上一次车位信息
    //优先读取本地，并异步联网获取，如果联网获取成功，则更新本地
    private void getLastPosition(){
        getLocalPosition();

        Log.d("beforeGetOnline", "getLastPosition: ");

        //异步联网获取
        getInfoByAccount(userName,userPwd,macAddress);

    }
    //endregion

    //region 从本地获取车位信息
    private void getLocalPosition(){
        Cursor cursor;      //保存查询结果
        cursor = db.rawQuery("select * from carInfo", null);
        if(cursor != null && cursor.getCount() != 0){
            //如果不是空表
            cursor.moveToFirst();
            curPosition = cursor.getString(cursor.getColumnIndex("position"));
            curTime = cursor.getString(cursor.getColumnIndex("time"));
            if(curTime.equals("")){
                curTime = " ";
            }
            if(curPosition.equals("")){
                curPosition = " ";
            }
            position.setText(curPosition);
            time.setText(curTime);
            cursor.close();
        }
        cursor.close();
    }
    //endregion

    //region 扫码完后的回调

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //扫描二维码，回传
        if(requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK){
            if(data != null){
                curPosition = data.getStringExtra(Constant.CODED_CONTENT);
                position.setText(curPosition);

                SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                Date curDate = new Date(System.currentTimeMillis());
                curTime = format.format(curDate);
                time.setText(curTime);

                //更新数据库
                updateDateBase(curPosition,curTime);
            }
        }
    }

    //endregion

    //region 获取权限
    private void getAccess(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},MY_PERMISSIONS_REQUEST_CAMERA);
        }else{
            doScan();
        }
    }
    //endregion

    //region 获取权限的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_CAMERA){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                doScan();
            }else{
                Toast.makeText(this,"请允许权限后重试",Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //endregion

    //region 扫描二维码
    private void doScan(){
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);

        ZxingConfig config = new ZxingConfig();
        config.setShowbottomLayout(true);
        config.setPlayBeep(true);
        config.setShake(false);
        config.setShowAlbum(false);
        config.setShowFlashLight(true);
        intent.putExtra(Constant.INTENT_ZXING_CONFIG,config);
        startActivityForResult(intent,REQUEST_CODE_SCAN);
    }
    //endregion

    //region 通过账号密码和mac地址联网获取信息
    private void getInfoByAccount(String name,String pwd,String mac){
        Log.d("getOnline", "getInfoByAccount: ");
        //创建Form表单对象
        FormBody formBody = new FormBody.Builder()
                .add("userName",name)
                .add("pwd",pwd)
                .add("macAddress",mac)
                .build();
        //创建一个request
        Request request = new Request.Builder()
                .url(OkHttpUtils.BASE_URL+"getInfo")
                .post(formBody)
                .build();
        //发起异步请求，并加入回调
        OkHttpUtils.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //没有网络
                mHandle.post(new Runnable() {
                    @Override
                    public void run() {
                        //如果登陆过的，无法联网才需要提示
                        if(!userName.equals("")) {
                            Toast.makeText(MainActivity.this, "无法连接网络，当前仅显示本地数据", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                //传回了数据
                mHandle.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(MainActivity.this, "在线返回消息", Toast.LENGTH_LONG).show();
                        if(response.isSuccessful()){
                            try {
                                String s = response.body().string();
                                JSONObject jsonObject = new JSONObject(s);
                                if(jsonObject.has("position")) {
                                    gPosition = jsonObject.getString("position");
                                }
                                if(jsonObject.has("time")) {
                                    gTime = jsonObject.getString("time");
                                }
                                if(jsonObject.has("tip")) {
                                    gTip = jsonObject.getString("tip");
                                }
                                mHandle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(gTip!=null){
                                            Toast.makeText(MainActivity.this, gTip, Toast.LENGTH_LONG).show();
                                            //Log.e("返回", "run: "+gTip);
                                        }
                                        if(gPosition!=null && !gPosition.trim().equals("")){
                                            if(gTime!=null && !gTime.trim().equals("")){
                                                position.setText(gPosition);
                                                time.setText(gTime);
                                            }
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }
    //endregion

    //region 更新数据库
    private void updateDateBase(String position,String time){
        String uPosition = position;
        String uTime = time;
        String uMac = macAddress;
        String uName = userName;
        String uPwd = userPwd;

        //更新本地数据
        updateLocalPositionData(uPosition,uTime,uMac);

        //更新在线数据
        updateOnlinePosition(uName,uPwd,uMac,uPosition,uTime);

    }
    //endregion

    //region 更新本地数据库中位置信息
    private void updateLocalPositionData(String position,String time,String mac){
        Log.d("更新数据库", "更新本地数据库");
        String sql = "delete from carInfo";
        db.execSQL(sql);
        sql = "insert into carInfo(position,time,seqNum) values('"+position+"','"+time+"','"+mac+"')";
        db.execSQL(sql);
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
                updateOnlinePosition(userName,userPwd,macAddress,curPosition,curTime);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }
    //endregion



    //region 判断用户名密码是否正确
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
        //发起异步请求，并加入回调
        OkHttpUtils.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandle.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(MainActivity.this,"网络连接错误，请重试",Toast.LENGTH_LONG).show();
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
                                    }
                                });
                            }
                        }

                        mHandle.post(new Runnable() {
                            @Override
                            public void run() {
                                //Toast.makeText(MainActivity.this,"用户名或密码错误",Toast.LENGTH_LONG).show();
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        mHandle.post(new Runnable() {
                            @Override
                            public void run() {
                                //Toast.makeText(MainActivity.this,"用户名或密码错误",Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }else{
                    mHandle.post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(MainActivity.this,"网络连接错误，请重试",Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
    //endregion



}
