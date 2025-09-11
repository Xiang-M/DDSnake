package com.example.snakegame;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.snakegame.DDSgenerated.JoinRoom;

import com.example.snakegame.uitls.DataCallbackJoinRoom;

import com.example.snakegame.data.dds.publisher.JoinRoomPublisher;
import com.example.snakegame.data.dds.subscriber.JoinRoomSubscriber;

public class JoinRoomActivity extends AppCompatActivity implements DataCallbackJoinRoom {
    private static final String TAG = "JoinRoomActivity";
    private String roomId = "";
    private AlertDialog dialog;
    private EditText etRoomId;
    private Button btnCancel;
    private Button btnJoin;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);

        // JOINROOM发布端初始化
        JoinRoomPublisher.initialize();
        // JOINROOMREPLY订阅端初始化
        JoinRoomSubscriber.initialize();

        showJoinRoomDialog();// 展示弹窗以及交互
    }
    
    private void showJoinRoomDialog() {
        // 用于创建弹窗（AlertDialog） 的构建器（Builder）对象。它的作用是配置和创建一个自定义或标准样式的对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 将XML布局文件转换成一个实际的View对象，用于在对话框中显示自定义的界面布局。
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_join_room, null);
        builder.setView(dialogView);
        
        etRoomId = dialogView.findViewById(R.id.et_room_id); // 编辑的文本提示框
        btnCancel = dialogView.findViewById(R.id.btn_cancel); // 取消按钮
        btnJoin = dialogView.findViewById(R.id.btn_join); // 加入按钮
        
        dialog = builder.create(); // 创建弹窗

        // 点击取消按钮
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // 关闭并销毁对话框
                finish();
            }
        });

        // 点击加入按钮
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JoinRoom();
            }
        });
        
        dialog.show();
    }

    private boolean isSixDigits(String str){
        return str != null && str.length() == 6;
    }

    // 加入房间方法
    public void JoinRoom(){
        roomId = etRoomId.getText().toString().trim(); // 获取用户输入的房间号

        // 验证输入房间号是否为6位
        if (!isSixDigits(roomId)) {
            Toast.makeText(JoinRoomActivity.this, "请输入6位房间号", Toast.LENGTH_SHORT).show();
            return;
        }

        // 准备好发送的数据
        // 创建数据
        JoinRoom data = new JoinRoom();
        data.player_id = sharedPreferences.getInt("player_id", 0);
        data.player_nickname = sharedPreferences.getString("nickname","nickname");
        data.room_id = roomId;
        data.status = "REQUEST";

        // 发送加入房间的申请
        JoinRoomPublisher.sendData(data);

        // 接收回复结果
        JoinRoomSubscriber.receiveData(data,this);
    }

    // 成功加入房间，就进入RoomActivity，并关闭弹窗
    public void joinRoomSuccess(){
        // 进入房间
        Intent intent = new Intent(JoinRoomActivity.this, RoomActivity.class);
        intent.putExtra("isHost", false);
        intent.putExtra("roomId", roomId);
        startActivity(intent);
        dialog.dismiss();
        finish();
    }
    // 加入房间失败，就弹出提示消息
    public void joinRoomFailure(){
        Toast.makeText(JoinRoomActivity.this, "当前房间不可进，请确认房间号输入正确", Toast.LENGTH_SHORT).show();
    }
    public void onError(Exception e){
        Log.d(TAG,"数据接收失败，错误: " + e.getMessage());
    }
}
