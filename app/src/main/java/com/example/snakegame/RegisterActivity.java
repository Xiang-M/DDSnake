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
import com.example.snakegame.uitls.DataCallbackLogin;
import com.example.snakegame.uitls.DataCallbackRegister;

import com.example.snakegame.thread.PlayerAuthSubscriberThread;


public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "registerActivity";
    private EditText etNickname, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 创建并启动监听线程
        new PlayerAuthSubscriberThread(new DataCallbackLogin() {
            @Override
            public void onDataReceived(PlayerAuth result) {
                runOnUiThread(() -> {
                    // 以下内容在主线程执行
                    checkData(result);
                });
            }
        }).start();

        PlayerAuthPublisher.initialize();

        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etNickname = findViewById(R.id.et_nickname);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);
    }
    
    private void setupListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
        
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 返回登录页面
            }
        });
    }
    
    private void register() {
        String nickname = etNickname.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        if (nickname.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6) {
            Toast.makeText(this, "密码长度至少6位", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建数据
        PlayerAuth data = new PlayerAuth();
        data.nickname = nickname;
        data.password = password;
        data.auth_type = "REGISTER";

        // 发布数据
        PlayerAuthPublisher.sendData(data);
    }

    private void checkData(PlayerAuth result) {
        if(result.nickname.equals(etNickname.getText().toString().trim()) && result.auth_type.equals("REGISTER_SUCCESS")){
            Toast.makeText(this, "注册成功！请登录", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}