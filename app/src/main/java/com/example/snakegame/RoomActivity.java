package com.example.snakegame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snakegame.DDSgenerated.PlayerAuth;
import com.example.snakegame.DDSgenerated.PlayerColorMapping;
import com.example.snakegame.DDSgenerated.PlayerColorMappingSeq;
import com.example.snakegame.DDSgenerated.PlayerColorMappings;
import com.example.snakegame.DDSgenerated.StartGame;
import com.example.snakegame.data.dds.publisher.StartGamePublisher;

import java.util.ArrayList;
import java.util.List;

import com.example.snakegame.DDSgenerated.InRoom;
import com.example.snakegame.DDSgenerated.LeaveRoom;

import com.example.snakegame.data.dds.publisher.InRoomPublisher;
import com.example.snakegame.data.dds.publisher.LeaveRoomPublisher;

import com.example.snakegame.thread.InRoomSubscriberThread;
import com.example.snakegame.thread.PlayerAuthSubscriberThread;
import com.example.snakegame.thread.PlayerColorMappingsSubscriberThread;
import com.example.snakegame.uitls.DataCallback;
import com.example.snakegame.uitls.DataCallbackColorMap;
import com.example.snakegame.uitls.DataCallbackRoom;

public class RoomActivity extends AppCompatActivity implements DataCallbackRoom {
    private TextView tvRoomId, tvPlayerCount;// 房间号 玩家人数
    private Button btnCopyRoomId, btnLeaveRoom, btnStartGame;// 复制按钮，离开房间按钮，开始游戏按钮
    private RecyclerView rvPlayers;//回收视图，列表显示组件，用于高效地显示大量数据
    // 集合复用机制：只创建屏幕可见的少量列表项
    // 当列表项滑出屏幕时，系统会回收这些视图并重用它们来显示新数据
    private PlayerAdapter playerAdapter;//RecyclerView的适配器
    private List<Player> playerList;// 玩家列表（包含房主）
    private SharedPreferences sharedPreferences; // 本地轻量存储
    private String roomId;  // 6位房间号
    private int domain_id; // 根据房间号计算得到的域号
    private boolean isHost; // 普通玩家和房主都会显示该页面，所以需要区分（有无开始按钮）
    private boolean getColors = false;

    // 从本地存储中获得玩家ID和昵称
    private int currentPlayerId;
    private String currentPlayerNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        // 创建并启动监听线程
        new InRoomSubscriberThread(new DataCallback<InRoom>() {
            @Override
            public void onDataReceived(InRoom result) {
                runOnUiThread(() -> {
                    // 以下内容在主线程执行
                    if (result.room_id != null && result.room_id.equals(roomId)) {
                        if(result.room_state == "waiting"){
                            updatePlayers(result);
                        }else if(result.room_state == "playing"){
                            startGame();
                        }
                    }
                });
            }
        }).start();

        new PlayerColorMappingsSubscriberThread(new DataCallback<PlayerColorMappings>() {
            @Override
            public void onDataReceived(PlayerColorMappings result) {
                runOnUiThread(() -> {
                    // 以下内容在主线程执行
                    if (result.room_id != null && result.room_id.equals(roomId)) {
                        savePlayerColorMapping(result);
                    }
                });
            }
        }).start();

        // 发布者的初始化
        InRoomPublisher.initialize();
        LeaveRoomPublisher.initialize();
        
        // 获取本地存储
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        isHost = getIntent().getBooleanExtra("isHost", true);// Intent 是 Android 中用于组件间通信的消息对象,从 Intent 中获取一个布尔值（boolean）参数,判断是否为房主
        roomId = getIntent().getStringExtra("roomId");
        if (roomId == null) {
            // 处理roomId不存在的情况
            roomId = "123456";  // 手动设置默认值
        }

        // 根据房间号计算得到对应域号
        domain_id = encryptRoomIdToDomain(Integer.parseInt(roomId));
        
        // 初始化当前玩家信息
        currentPlayerId = sharedPreferences.getInt("player_id", 13);
        currentPlayerNickname = sharedPreferences.getString("nickname", "nickname");

        // 如果是房主，发送初始的房间信息
        if(isHost){
            createRoom();
            Toast.makeText(this, "我是房主，已发送数据", Toast.LENGTH_SHORT).show();
        }

        initViews();// 将布局上的内容与类内字段相连
        setupRecyclerView();// 设置回收视图，和类内字段playerList相连
        setupListeners(); // 响应用户操作
    }
    
    private void initViews() {
        tvRoomId = findViewById(R.id.tv_room_id); //房间号
        tvPlayerCount = findViewById(R.id.tv_player_count); // 玩家数量
        btnCopyRoomId = findViewById(R.id.btn_copy_room_id); // 复制房间号按钮
        btnLeaveRoom = findViewById(R.id.btn_leave_room); // 离开房间按钮
        btnStartGame = findViewById(R.id.btn_start_game); // 开始游戏按钮
        rvPlayers = findViewById(R.id.rv_players); // 列表视图

        tvRoomId.setText(roomId);
    }
    
    private void setupRecyclerView() {
        playerList = new ArrayList<>();// 1. 创建一个空的玩家列表
        playerAdapter = new PlayerAdapter(playerList); // 2. 创建适配器，将数据(playerList)与RecyclerView连接起来
        rvPlayers.setLayoutManager(new LinearLayoutManager(this)); // 3. 设置RecyclerView的布局管理器为线性布局（垂直列表）
        rvPlayers.setAdapter(playerAdapter); // 4. 将适配器设置给RecyclerView
    }
    
    private void setupListeners() {
        // 复制房间号
        btnCopyRoomId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("房间号", roomId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(RoomActivity.this, "房间号已复制: " + roomId, Toast.LENGTH_SHORT).show();
            }
        });

        // 离开房间
        btnLeaveRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 发送离开房间消息
                LeaveRoom data = new LeaveRoom();
                data.room_id = roomId;
                data.player_id = currentPlayerId;
                LeaveRoomPublisher.sendData(data);
                finish();
            }
        });

        // 开始游戏
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 发送开始游戏的信息
                StartGame data = new StartGame();
                data.room_id = roomId;
                StartGamePublisher.sendData(data);
            }
        });
    }

    // 根据房间号计算域号
    private static int encryptRoomIdToDomain(int roomId) {
        int baseDomain = 80; //

        int range = 100; //
        // 可用域号数量（即 80~179）
        int val = (roomId * 1315423911) ^ (roomId >>> 16); //
        return baseDomain + Math.abs(val % range);
    }

    // 创建房间发送初始信息，仅对isHost
    private void createRoom(){
        InRoom data = new InRoom();
        data.room_id = roomId;
        Toast.makeText(this, data.room_id, Toast.LENGTH_SHORT).show();
        data.player_id = 22;
        Toast.makeText(this, Integer.toString(data.player_id), Toast.LENGTH_SHORT).show();
        data.player_nickname = "xm";
        Toast.makeText(this, data.player_nickname, Toast.LENGTH_SHORT).show();
        data.room_state = "empty";
        Toast.makeText(this, data.room_state, Toast.LENGTH_SHORT).show();
        InRoomPublisher.sendData(this,data);
    }

    // 更新玩家列表
    public void updatePlayers(InRoom result){
        // 更新玩家列表（清空，再全部重新填充）
        synchronized (playerList) {
            playerList.clear();
            playerList.add(new Player(result.player_id, result.player_nickname,true));
            for (int i = 0; i < result.player_nicknames.length(); i++) {
                playerList.add(new Player(result.player_ids.get_at(i), result.player_nicknames.get_at(i),false));
            }
        }
        // 更新UI
        updateUI();
    }

    // 开始游戏
    public void startGame(){
        // 确认玩家颜色映射信息已收到
        while(!getColors){}
        // 把三个list准备好
        ArrayList<Integer> all_player_id = new ArrayList<>();
        ArrayList<String> all_player_nickname = new ArrayList<>();
        ArrayList<String> all_player_color = new ArrayList<>();
        for (Player player : playerList) {
            all_player_id.add(player.getId());
            all_player_nickname.add(player.getNickname());
            all_player_color.add(player.getColor());
        }
        // 进入游戏页面，加上属性：域号，房间号，三个list
        Intent intent = new Intent(RoomActivity.this, MainActivity.class);
        intent.putExtra("domain_id", domain_id);
        intent.putExtra("room_id", roomId);
        intent.putExtra("all_player_id", all_player_id);
        intent.putExtra("all_player_nickname",all_player_nickname);
        intent.putExtra("all_player_color",all_player_color);
        startActivity(intent);
        // 关闭当前页面
        finish();
    }

    public void savePlayerColorMapping(PlayerColorMappings mapping){
        // 将玩家的颜色信息存储
        PlayerColorMappingSeq maps = mapping.maps;
        for (int i = 0; i < maps.length(); i++) {
            PlayerColorMapping playerColor = maps.get_at(i);
            setPlayerColorById(playerColor.player_id, playerColor.color);
        }
        getColors = true;
    }

    public void setPlayerColorById(int targetId, String newColor) {
        for (Player player : playerList) {
            if (player.getId() == targetId) {
                player.setColor(newColor);
                return; // 找到后立即返回
            }
        }
        // 如果没有找到对应id的玩家
        System.out.println("未找到ID为 " + targetId + " 的玩家");
    }

    // 更新页面
    private void updateUI() {
        tvPlayerCount.setText("房间玩家 (" + playerList.size() + "/6)");
        playerAdapter.notifyDataSetChanged();//通知适配器底层数据已更改，需要重新绑定所有可见的列表项
        
        // 只有房主显示开始游戏按钮
        btnStartGame.setVisibility(isHost ? View.VISIBLE : View.GONE);
    }

    // 内部类：玩家数据模型
    public static class Player {
        private int id;
        private String nickname;
        private boolean isHost;
        private String color = "black";
        
        public Player(int id, String nickname, boolean isHost) {
            this.id = id;
            this.nickname = nickname;
            this.isHost = isHost;
        }

        // Setters
        public void setColor(String color_){
            color = color_;
        }
        
        // Getters
        public int getId(){ return id; }
        public String getNickname() { return nickname; }
        public boolean isHost() { return isHost; }
        public String getColor() { return color; }
    }

    @Override
    public void sendDataSuccess() {
        Toast.makeText(this, "真的发送了", Toast.LENGTH_SHORT).show();
    }
}
