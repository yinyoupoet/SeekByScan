package com.yinyoupoet.seekbyscan;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class UserInfoActivity extends AppCompatActivity {
    Button exchange;
    TextView userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        init();
    }

    private void init(){
        //region 初始化toolbar
        Toolbar register_toolbar = (Toolbar) findViewById(R.id.info_toolbar);
        register_toolbar.setTitle("个人中心");
        register_toolbar.setNavigationIcon(R.drawable.chatlist_return);
        register_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //endregion

        userName = findViewById(R.id.info_name);
        exchange = findViewById(R.id.info_btn_login);

        //region 从数据库读取用户名
        MyDataBaseHelper dbHelper = new MyDataBaseHelper(UserInfoActivity.this,"localInfo.db",null,1);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor;      //保存查询结果
        cursor = db.rawQuery("select * from userInfo", null);
        if(cursor != null && cursor.getCount() != 0){
            //如果不是空表
            cursor.moveToFirst();
            String name = cursor.getString(cursor.getColumnIndex("userName"));
            userName.setText(name);
            cursor.close();
        }
        cursor.close();
        //endregion

        exchange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserInfoActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

}

