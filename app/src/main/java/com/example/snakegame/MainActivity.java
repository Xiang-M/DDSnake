package com.example.snakegame;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.snakegame.data.model.GameWorld;
import com.example.snakegame.data.model.Player;
import com.example.snakegame.data.dds.publisher.ChatPublisher;
import com.example.snakegame.data.dds.publisher.GamePublisher;
import com.example.snakegame.data.dds.subscriber.ChatSubscriber;
import com.example.snakegame.data.dds.subscriber.GameSubscriber;
import com.example.snakegame.presentation.contract.GameContract;
import com.example.snakegame.ui.view.GameSurfaceView;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import com.example.snakegame.data.model.Snake;
import com.example.snakegame.data.model.Point;
import com.example.snakegame.data.model.Food;
import com.example.snakegame.DDSgenerated.*;

public class MainActivity extends AppCompatActivity implements 
GameContract.View {
    
    private GameSurfaceView gameSurfaceView;
    private TextView tvScore;
    private TextView tvTime;
    private TextView tvLeaderboard;
    private TextView tvGameStatus;
    private LinearLayout controlPanel;
    private View loadingView;
    
    // 方向控制按钮
    private Button btnUp, btnDown, btnLeft, btnRight;
    private View directionControls;

    //快捷语句按钮
    private Button btnMessage1, btnMessage2, btnMessage3, btnMessage4, btnMessage5;
    
    private String playerNickname;
    private String playerColor;
    private boolean gameStarted = false;
    
    // 多人模式相关
    private boolean isMultiplayer = false;//暂时改为true，测试多人模式
    private boolean isHost = false;
    private int playerId;
    private int playerCount;


    // ChatPublisher 实例
    private ChatPublisher chatPublisher;
    // DDS 相关
    private GamePublisher gamePublisher;
    private ChatSubscriber chatSubscriber;
    private GameSubscriber gameSubscriber;


    // 多人玩家列表
    private ArrayList<Integer> allPlayerIds;
    private ArrayList<String> allPlayerNicknames;
    private ArrayList<String> allPlayerColors;

    // 添加用于存储游戏状态的字段
    private GameWorld gameWorld;
    private Map<Integer, Snake> snakes = new HashMap<>();
    private List<Food> foods = new ArrayList<>();

    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        // 初始化DomainParticipantFactory
        com.zrdds.domain.DomainParticipantFactoryQos dpfQos = new com.zrdds.domain.DomainParticipantFactoryQos();
        dpfQos.dds_log.file_mask = 0;
        dpfQos.dds_log.console_mask = 0xffff;
        dpfQos.dds_log.file_dir = String.valueOf(getFilesDir());
        com.zrdds.infrastructure.Property_t property = new com.zrdds.infrastructure.Property_t();
        property.name = "sysctl.global.licence";
        property.value = "data:UserName: \nAuth Date: 2020/09/15:19:17:26\nExpire Date: 2025/10/21:19:17:26\nMACS:\nunlimited\nHDS:\nunlimited\nSignature:\nb4b93ac94879a73959465ad0692722934efb100a1069d1e91d4fc14596483cf651496531f7376f389b2a6cea9dc4b276f8cdd3ce171f2c333a5f6061e0033a94889282b1d142ca3709b69e6e88cd24252818bd543c1f66a1ae905bdb8b854e03055a1535fa262570fbefcdb7c05b63f872809cd57f82dfcc72cc495eee824ff0\nLastVerifyDate:2024/11/21:11:01:5022325d7c7825924f6f8c0ab42a65414c";
        dpfQos.property.value.ensure_length(1, 1);
        dpfQos.property.value.set_at(0, property);

        com.zrdds.domain.DomainParticipantFactory factory = com.zrdds.domain.DomainParticipantFactory.get_instance_w_qos(dpfQos);
        if (factory == null) {
            android.util.Log.e("MainActivity", "无法获取DomainParticipantFactory实例");
        } else {
            android.util.Log.i("MainActivity", "✓ DomainParticipantFactory创建成功");

        // 获取登录传入的参数
        playerNickname = getIntent().getStringExtra("nickname");
        playerColor = getIntent().getStringExtra("color");
        
        // 获取多人模式参数
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", true);
        playerCount = getIntent().getIntExtra("playerCount", 1);
        isHost = getIntent().getBooleanExtra("isHost", false);
        playerId = getIntent().getIntExtra("playerId", 0);

        allPlayerIds = getIntent().getIntegerArrayListExtra("all_player_id");
        allPlayerNicknames = getIntent().getStringArrayListExtra("all_player_nickname");
        allPlayerColors = getIntent().getStringArrayListExtra("all_player_color");
        
        if (playerNickname == null) playerNickname = "Player1";
        if (playerColor == null) playerColor = "#FF0000";

        if(playerId==0) playerId=1001;


        initViews();
        // 移除initPresenter()调用，不再使用GamePresenter
        setupDirectionControls();


        // 初始化 ChatPublisher（放在 setupQuickMessageButtons 之前，这样按钮回调可以直接使用）
        chatPublisher = new ChatPublisher(this);
        gamePublisher = new GamePublisher(this, playerId); // 传递玩家ID
        chatSubscriber = new ChatSubscriber(this);
        gameSubscriber = new GameSubscriber(this);
        
        // 设置GameSubscriber监听器
        setupGameSubscriberListeners();

        setupQuickMessageButtons(); // 初始化快捷语句按钮

        startGame();
    }
}

    
    private void initViews() {
        gameSurfaceView = findViewById(R.id.game_surface_view);
        tvScore = findViewById(R.id.tv_score);
        tvTime = findViewById(R.id.tv_time);
        tvLeaderboard = findViewById(R.id.tv_leaderboard);
        tvGameStatus = findViewById(R.id.tv_game_status);
        controlPanel = findViewById(R.id.control_panel);
        loadingView = findViewById(R.id.loading_view);
        
        // 方向控制按钮
        btnUp = findViewById(R.id.btn_up);
        btnDown = findViewById(R.id.btn_down);
        btnLeft = findViewById(R.id.btn_left);
        btnRight = findViewById(R.id.btn_right);
        directionControls = findViewById(R.id.direction_controls);

        // 初始化快捷语句按钮
        btnMessage1 = findViewById(R.id.btn_message_1);
        btnMessage2 = findViewById(R.id.btn_message_2);
        btnMessage3 = findViewById(R.id.btn_message_3);
        btnMessage4 = findViewById(R.id.btn_message_4);
        btnMessage5 = findViewById(R.id.btn_message_5);

        
        // 始终显示时间（定时积分赛模式）
        tvTime.setVisibility(View.VISIBLE);
    }


    private void setupQuickMessageButtons() {
        // 这里按钮点击既显示本地气泡，也通过 chatPublisher 发送 DDS 消息（若 chatPublisher 初始化失败会为 null）
        btnMessage1.setOnClickListener(v -> {
            String msg = "菜！就多练";
            gameSurfaceView.showChatBubble(msg);
            if (chatPublisher != null) {
                chatPublisher.sendChatMessage(msg);
            }
        });

        btnMessage2.setOnClickListener(v -> {
            String msg = "抱歉";
            gameSurfaceView.showChatBubble(msg);
            if (chatPublisher != null) {
                chatPublisher.sendChatMessage(msg);
            }
        });

        btnMessage3.setOnClickListener(v -> {
            String msg = "大哥别杀我";
            gameSurfaceView.showChatBubble(msg);
            if (chatPublisher != null) {
                chatPublisher.sendChatMessage(msg);
            }
        });

        btnMessage4.setOnClickListener(v -> {
            String msg = "哈基米哟~南北绿豆";
            gameSurfaceView.showChatBubble(msg);
            if (chatPublisher != null) {
                chatPublisher.sendChatMessage(msg);
            }
        });

        btnMessage5.setOnClickListener(v -> {
            String msg = "不收徒";
            gameSurfaceView.showChatBubble(msg);
            if (chatPublisher != null) {
                chatPublisher.sendChatMessage(msg);
            }
        });
    }


 // 设置GameSubscriber监听器
private void setupGameSubscriberListeners() {
    // 设置GameState监听器
    gameSubscriber.setOnGameStateReceivedListener(new GameSubscriber.OnGameStateReceivedListener() {
        @Override
        public void onGameStateReceived(GameState gameState) {
            // 处理游戏状态更新
            handleGameStateUpdate(gameState);
        }
    });
    
    // 设置Item监听器（食物）
    gameSubscriber.setOnItemReceivedListener(new GameSubscriber.OnItemReceivedListener() {
        @Override
        public void onItemReceived(Item item) {
            // 处理食物更新
            handleItemUpdate(item);
        }
    });
    
    // 设置GetFood监听器（蛇吃到食物）
    gameSubscriber.setOnGetFoodReceivedListener(new GameSubscriber.OnGetFoodReceivedListener() {
        @Override
        public void onGetFoodReceived(GetFood getFood) {
            // 处理蛇吃到食物的事件
            handleGetFoodUpdate(getFood);
        }
    });
    
    // 设置排行榜监听器
    gameSubscriber.setOnLeaderboardReceivedListener(new GameSubscriber.OnLeaderboardReceivedListener() {
        @Override
        public void onLeaderboardReceived(Leaderboard leaderboard) {
            // 处理排行榜更新
            handleLeaderboardUpdate(leaderboard);
        }
    });
    
    // 设置碰撞监听器
    gameSubscriber.setOnCollisionReceivedListener(new GameSubscriber.OnCollisionReceivedListener() {
        @Override
        public void onCollisionReceived(Collision collision) {
            // 处理碰撞事件
            handleCollisionUpdate(collision);
        }
    });
    
    // 设置系统消息监听器
    gameSubscriber.setOnSystemMsgReceivedListener(new GameSubscriber.OnSystemMsgReceivedListener() {
        @Override
        public void onSystemMsgReceived(SystemMsg systemMsg) {
            // 处理系统消息
            handleSystemMsgUpdate(systemMsg);
        }
    });

    // 设置聊天消息监听器
    chatSubscriber.setOnMessageReceivedListener(new ChatSubscriber.OnChatMessageReceivedListener() {
        @Override
        public void onMessageReceived(ChatMsg msg) {
            // 处理聊天消息
            handleChatMessage(msg);
        }
    });
}
       
// 处理游戏状态更新
private void handleGameStateUpdate(GameState gameState) {
    android.util.Log.d("MainActivity", "接收到GameState消息: player_id=" + gameState.player_id + ", score=" + gameState.score);
    
    // 将DDS的GameState转换为本地的Snake对象
    Snake snake = new Snake();
    snake.setPlayerId(gameState.player_id);
    snake.setNickname(getPlayerNicknameById(gameState.player_id));
    snake.setColor(getPlayerColorById(gameState.player_id));
    snake.setAlive(true); // 假设如果收到GameState消息，蛇就是活着的
    snake.setScore(gameState.score);
    
    // 转换蛇的身体坐标
    List<Point> bodyPoints = new ArrayList<>();
    // 确保两个坐标序列长度一致
    int length = Math.min(gameState.snake_x.length(), gameState.snake_y.length());
    android.util.Log.d("MainActivity", "蛇身体长度: " + length);
    
    for (int i = 0; i < length; i++) {
        // 注意：这里需要使用get_at方法获取序列中的元素
        long x = gameState.snake_x.get_at(i);
        long y = gameState.snake_y.get_at(i);
        bodyPoints.add(new Point((int)x, (int)y));
        // android.util.Log.d("MainActivity", "蛇身体坐标: (" + x + ", " + y + ")");
    }
    snake.setBodyPoints(bodyPoints);
    snake.setGrowing(false); // 根据需要设置
    
    // 更新蛇的数据
    snakes.put(gameState.player_id, snake);
    
    android.util.Log.d("MainActivity", "更新蛇数据完成，当前蛇数量: " + snakes.size() + "，玩家ID: " + gameState.player_id);
    
    // 特别检查是否是自己的蛇
    if (gameState.player_id == playerId) {
        android.util.Log.d("MainActivity", "更新了自己的蛇数据");
    }
    
    // 更新游戏世界并渲染
    updateGameWorld();
}

// 修改handleItemUpdate方法，添加调试日志
private void handleItemUpdate(Item item) {
    android.util.Log.d("MainActivity", "接收到Item消息: x=" + item.x + ", y=" + item.y);
    
    // 将DDS的Item转换为本地的Food对象
    Food food = new Food();
    Point position = new Point(item.x, item.y);
    food.setPosition(position);
    
    // 设置食物类型
    ItemType itemType = ItemType.valueOf(item.item_type.ordinal());

    if (itemType == ItemType.APPLE) food.setType(Food.FoodType.APPLE);
    else if (itemType == ItemType.GOOD_FOOD) food.setType(Food.FoodType.GOOD_FOOD);
    else if (itemType == ItemType.BAD_FOOD) food.setType(Food.FoodType.BAD_FOOD);

    // 添加食物到列表
    foods.add(food);
    
    android.util.Log.d("MainActivity", "添加食物完成，当前食物数量: " + foods.size());
    
    // 更新游戏世界并渲染
    updateGameWorld();
}

// 修改updateGameWorld方法，确保传递给GameSurfaceView的食物列表是安全的
private void updateGameWorld() {
    runOnUiThread(() -> {
        android.util.Log.d("MainActivity", "开始更新游戏世界");
        
        // 创建或更新游戏世界对象
        if (gameWorld == null) {
            gameWorld = new GameWorld();
        }
        
        // 更新游戏世界中的蛇和食物数据
        Snake mySnake = snakes.get(playerId);
        gameWorld.setMySnake(mySnake);
        
        // 创建蛇列表的副本
        List<Snake> snakesCopy;
        synchronized (snakes) {
            snakesCopy = new ArrayList<>(snakes.values());
        }
        gameWorld.setOtherSnakes(snakesCopy);
        
        // 创建食物列表的副本
        List<Food> foodsCopy;
        synchronized (foods) {
            foodsCopy = new ArrayList<>(foods);
        }
        gameWorld.setFoods(foodsCopy);
        
        android.util.Log.d("MainActivity", "游戏世界数据更新完成");
        android.util.Log.d("MainActivity", "我的蛇: " + (mySnake != null ? "存在" : "不存在") + "，玩家ID: " + playerId);
        android.util.Log.d("MainActivity", "其他蛇数量: " + snakes.size());
        android.util.Log.d("MainActivity", "食物数量: " + foods.size());

        // 更新视野位置以蛇头为中心（仅当自己的蛇存在时）
        if (mySnake != null && mySnake.getBodyPoints() != null && !mySnake.getBodyPoints().isEmpty()) {
            Point snakeHead = mySnake.getBodyPoints().get(0);
            gameWorld.updateViewToCenter(snakeHead);
            android.util.Log.d("MainActivity", "更新视野位置: (" + gameWorld.getViewOffsetX() + ", " + gameWorld.getViewOffsetY() + ")");
            android.util.Log.d("MainActivity", "蛇头位置: (" + snakeHead.getX() + ", " + snakeHead.getY() + ")");
        } else {
            android.util.Log.d("MainActivity", "无法更新视野，因为自己的蛇不存在或没有身体点");
        }
        
        // 更新游戏界面
        if (gameSurfaceView != null) {
            android.util.Log.d("MainActivity", "调用GameSurfaceView更新");
            gameSurfaceView.updateGameWorld(gameWorld);
        } else {
            android.util.Log.e("MainActivity", "GameSurfaceView为null");
        }
        
        // 更新分数显示
        if (mySnake != null) {
            tvScore.setText("分数: " + mySnake.getScore());
            android.util.Log.d("MainActivity", "更新分数显示: " + mySnake.getScore());
        }
    });
}

    // 处理聊天消息
    private void handleChatMessage(ChatMsg msg) {
        runOnUiThread(() -> {
            android.util.Log.d("MainActivity", "接收到聊天消息: " + msg.content + " 来自玩家ID: " + msg.player_id);

            // 如果是自己发送的消息，则不需要重复显示
            if (msg.player_id == playerId) {
                return;
            }

            // 在对应玩家的蛇头上显示聊天气泡
            gameSurfaceView.showPlayerChatBubble(msg.player_id, msg.content);
        });
    }


// 添加handleGetFoodUpdate方法
private void handleGetFoodUpdate(GetFood getFood) {
    // 从食物列表中移除被吃掉的食物
    foods.removeIf(food -> 
        food.getPosition().getX() == getFood.x && 
        food.getPosition().getY() == getFood.y);
    
    // 更新吃到食物的蛇的状态（只标记刚刚吃到了食物）
    Snake snake = snakes.get(getFood.player_id);
    if (snake != null) {
        snake.setJustAte(true);
    }
    
    // 更新游戏世界并渲染
    updateGameWorld();
}


// 修改handleCollisionUpdate方法
private void handleCollisionUpdate(Collision collision) {
    // 获取所有玩家的ID和碰撞状态
    int playerCount = collision.player_ids.length();
    
    for (int i = 0; i < playerCount; i++) {
        long playerId = collision.player_ids.get_at(i);
        boolean isCollided = collision.collisions.get_at(i);
        
        if (isCollided) {
            // 更新碰撞的蛇的状态
            Snake snake = snakes.get((int) playerId);
            if (snake != null) {
                snake.setAlive(false);
            }
        }
    }
    
    // 更新游戏世界并渲染
    updateGameWorld();
}
    // 处理排行榜更新
    private void handleLeaderboardUpdate(Leaderboard leaderboard) {
        // 更新UI中的排行榜显示
        updateLeaderboardUI(leaderboard);
    }
    
    
    // 处理系统消息
    private void handleSystemMsgUpdate(SystemMsg systemMsg) {
        // 根据消息类型处理不同系统消息
        if ("END".equals(systemMsg.msg_type)) {
            // 游戏结束消息
            runOnUiThread(() -> {
                tvGameStatus.setText("游戏结束");
                tvGameStatus.setVisibility(View.VISIBLE);
            });
        }
    }
    
    // 根据玩家ID获取昵称
    private String getPlayerNicknameById(int playerId) {
        if (allPlayerIds != null && allPlayerNicknames != null) {
            int index = allPlayerIds.indexOf(playerId);
            if (index >= 0 && index < allPlayerNicknames.size()) {
                return allPlayerNicknames.get(index);
            }
        }
        return "Player" + playerId;
    }
    
    // 根据玩家ID获取颜色
    private String getPlayerColorById(int playerId) {
        if (allPlayerIds != null && allPlayerColors != null) {
            int index = allPlayerIds.indexOf(playerId);
            if (index >= 0 && index < allPlayerColors.size()) {
                return allPlayerColors.get(index);
            }
        }
        return "#FF0000";
    }
    

    // 更新排行榜UI
    private void updateLeaderboardUI(Leaderboard leaderboard) {
        runOnUiThread(() -> {
            StringBuilder leaderboardText = new StringBuilder("排行榜:\n");
            for (int i = 0; i < leaderboard.entries.length(); i++) {
                LeaderboardEntry entry = leaderboard.entries.get_at(i);
                leaderboardText.append(entry.nickname)
                              .append(": ")
                              .append(entry.score)
                              .append("\n");
            }
            tvLeaderboard.setText(leaderboardText.toString());
        });
    }
    
    private void setupDirectionControls() {
        // 方向控制始终显示
        directionControls.setVisibility(View.VISIBLE);

        // 设置方向按钮点击事件，只发送方向命令，不处理游戏逻辑
        btnUp.setOnClickListener(v -> {
            if (gameStarted) {
                if (gamePublisher != null) {
                    gamePublisher.sendMove("UP");
                }
            }
        });

        btnDown.setOnClickListener(v -> {
            if (gameStarted) {
                if (gamePublisher != null) {
                    gamePublisher.sendMove("DOWN");
                }
            }
        });

        btnLeft.setOnClickListener(v -> {
            if (gameStarted) {
                if (gamePublisher != null) {
                    gamePublisher.sendMove("LEFT");
                }
            }
        });

        btnRight.setOnClickListener(v -> {
            if (gameStarted) {
                if (gamePublisher != null) {
                    gamePublisher.sendMove("RIGHT");
                }
            }
        });

    }
    

    private void startGame() {
        gameStarted = true;
        
        // 隐藏状态提示
        tvGameStatus.setVisibility(View.GONE);
        
        try {
            // 根据模式启动游戏
            if (isMultiplayer && playerId != 0) {
                // 多人模式：使用测试玩家列表
                if (allPlayerIds != null && allPlayerNicknames != null && allPlayerColors != null) {
                    android.util.Log.d("MainActivity", "使用玩家列表启动多人游戏模式");

                } else {
                    android.util.Log.d("MainActivity", "使用单个玩家启动多人游戏模式");
                }
            } else {
                // 单人模式：使用默认ID
                android.util.Log.d("MainActivity", "启动单人游戏模式");
            }
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "游戏启动失败", e);
            Toast.makeText(this, "游戏启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            gameStarted = false;
        }
    }
    
    @Override
    public void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void hideLoading() {
        loadingView.setVisibility(View.GONE);
    }
    
    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onGameWorldUpdated(GameWorld gameWorld) {
        gameSurfaceView.updateGameWorld(gameWorld);
        
        // 更新分数显示
        if (gameWorld.getMySnake() != null) {
            tvScore.setText("Score: " + gameWorld.getMySnake().getScore());
        }
        
        // 更新排行榜
        if (gameWorld.getLeaderboard() != null) {
            updateLeaderboard(gameWorld.getLeaderboard());
        }
    }
    
    @Override
    public void onGameStarted() {
        tvGameStatus.setVisibility(View.GONE);
    }
    
    @Override
    public void onGameEnded() {
        gameStarted = false;
        tvGameStatus.setText("游戏结束！点击重新开始");
        tvGameStatus.setVisibility(View.VISIBLE);
        Toast.makeText(this, "游戏结束！", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void showChatMessage(String playerName, String message) {
        Toast.makeText(this, playerName + ": " + message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void updateLeaderboard(List<Player> leaderboard) {
        if (leaderboard != null && !leaderboard.isEmpty()) {
            // 过滤掉重复的玩家和无效数据
            Set<String> addedPlayers = new HashSet<>();
            StringBuilder sb = new StringBuilder();
            int rank = 1;
            
            for (Player player : leaderboard) {
                // 检查玩家是否有效且未重复
                if (player != null && 
                    player.getNickname() != null && 
                    !player.getNickname().isEmpty() &&
                    !addedPlayers.contains(player.getNickname()) &&
                    rank <= 3) {
                    
                    sb.append(rank).append(". ")
                      .append(player.getNickname())
                      .append(" (").append(player.getScore()).append(")");
                    
                    if (rank < 3 && rank < leaderboard.size()) {
                        sb.append("\n");
                    }
                    
                    addedPlayers.add(player.getNickname());
                    rank++;
                }
            }
            
            // 如果没有有效的排行榜数据，显示等待信息
            if (sb.length() == 0) {
                tvLeaderboard.setText("等待玩家...");
            } else {
                tvLeaderboard.setText(sb.toString());
            }
        } else {
            tvLeaderboard.setText("等待玩家...");
        }
    }

    @Override
    public void onTimeUpdate(int remainingTimeSeconds) {
        runOnUiThread(() -> {
            int minutes = remainingTimeSeconds / 60;
            int seconds = remainingTimeSeconds % 60;
            tvTime.setText(String.format("时间: %02d:%02d", minutes, seconds));
        });
    }

    @Override
    public void onTimedGameEnded(String winnerMessage) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("定时积分赛结束")
                    .setMessage(winnerMessage)
                    .setPositiveButton("再来一局", (dialog, which) -> {
                        // 重新开始游戏
                        startGame();
                    })
                    .setNegativeButton("返回主页", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    @Override
    public void onPlayerDiedInTimedMode(String playerName, int finalScore) {
        runOnUiThread(() -> {
            tvGameStatus.setText(playerName + " 死亡，得分固定为 " + finalScore + "，已转化为食物");
            tvGameStatus.setVisibility(View.VISIBLE);
            
            // 3秒后隐藏状态信息
            new Handler().postDelayed(() -> {
                tvGameStatus.setVisibility(View.GONE);
            }, 3000);
        });
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理DDS资源
        try {
            chatPublisher = null;
            gamePublisher = null;
            chatSubscriber = null;
            gameSubscriber = null;

        } catch (Throwable t) {
            // ignore
        }
        
        // 等待一点时间确保资源完全释放
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}