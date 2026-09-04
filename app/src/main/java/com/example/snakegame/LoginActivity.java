package com.example.snakegame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.snakegame.DDSgenerated.PlayerAuth;
import com.example.snakegame.data.dds.publisher.PlayerAuthPublisher;
import com.example.snakegame.uitls.DataCallback;

import com.example.snakegame.thread.PlayerAuthSubscriberThread;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText etNickname, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 检查是否已登录
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);// 整个应用共享
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            Log.i(TAG,"已登录，直接进入主页面");
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // 创建并启动监听线程
        new PlayerAuthSubscriberThread(new DataCallback<PlayerAuth>() {
            @Override
            public void onDataReceived(PlayerAuth result) {
                runOnUiThread(() -> {
                    // 以下内容在主线程执行
                    checkData(result);
                });
            }
        }).start();
        // 创建订阅者实体
        PlayerAuthPublisher.initialize();
        
        initViews();
        setupListeners();
        Log.i(TAG,"布局成功");
    }
    
    private void initViews() {
        etNickname = findViewById(R.id.et_nickname);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);
    }
    
    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        
        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }
    
    private void login() {
        String nickname = etNickname.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (nickname.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入昵称和密码", Toast.LENGTH_SHORT).show();
            return;
        }
        // 创建数据
        PlayerAuth data = new PlayerAuth();
        data.nickname = nickname;
        data.password = password;
        data.auth_type = "LOGIN";
        // 发布数据
        PlayerAuthPublisher.sendData(data);
    }

    // 实现回调函数
    private void checkData(PlayerAuth result){
        if(result.nickname.equals(etNickname.getText().toString().trim()) &&
                result.auth_type.equals("LOGIN_SUCCESS")){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.putInt("player_id", result.player_id);
            editor.putString("nickname", result.nickname);
            editor.putString("password", result.password);
            editor.apply();
            Log.d(TAG,"数据接收成功");
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }else if(result.nickname.equals(etNickname.getText().toString().trim()) &&
                result.auth_type.equals("LOGIN_FAIL")){
            Toast.makeText(this, "登录失败，请确认昵称和密码匹配", Toast.LENGTH_SHORT).show();
        }
    }
}

