package com.example.snakegame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class HomeActivity extends AppCompatActivity {
    
    private TextView tvWelcome;
    private Button btnCreateRoom, btnJoinRoom, btnSingleGame, btnLogout;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        
        // 检查登录状态
        if (!sharedPreferences.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        initViews();
        setupListeners();
        updateWelcomeText();
    }
    
    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        btnCreateRoom = findViewById(R.id.btn_create_room);
        btnJoinRoom = findViewById(R.id.btn_join_room);
        btnSingleGame = findViewById(R.id.btn_single_game);
        btnLogout = findViewById(R.id.btn_logout);
    }
    
    private void setupListeners() {
        // 创建房间按钮
        // 点击进入RoomActivity，即展示玩家列表的等待界面
        // 以房主身份
        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, RoomActivity.class);
                intent.putExtra("isHost", true);
                intent.putExtra("roomId", generateSixDigitNumber());// 随机生成6位房间号
                startActivity(intent);
            }
        });

        // 加入房间按钮
        // 点击进入JoinRoomActivity，即填写房间号的等待界面
        btnJoinRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, JoinRoomActivity.class));
            }
        });

        // 单人游戏
        btnSingleGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启动定时积分赛游戏
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
            }
        });
        
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到主页面时更新欢迎文字，以防昵称被修改
        updateWelcomeText();
    }
    
    private void updateWelcomeText() {
        String nickname = sharedPreferences.getString("nickname", "玩家");
        tvWelcome.setText("欢迎回来，" + nickname + "！");
    }
    
    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public static String generateSixDigitNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        // 生成6位随机数字
        for (int i = 0; i < 6; i++) {
            int digit = random.nextInt(10); // 生成0-9的随机数
            sb.append(digit);
        }

        return sb.toString();
    }
}
