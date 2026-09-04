package com.example.snakegame.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.snakegame.DDSgenerated.Leaderboard;
import com.example.snakegame.DDSgenerated.LeaderboardEntry;
import com.example.snakegame.data.model.GameWorld;
import com.example.snakegame.data.model.Snake;
import com.example.snakegame.data.model.Food;
import com.example.snakegame.data.model.Point;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import com.example.snakegame.DDSgenerated.InRoom;
import com.example.snakegame.data.model.Player;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private GameWorld gameWorld;
    
    //快捷语句按钮
    

    
    // 游戏区域配置
    private int gridCols; // 列数
    private int gridRows; // 行数
    private int cellSize; // 每个格子的大小（方形）
    private int offsetX, offsetY; // 居中偏移

    // 在GameSurfaceView中添加缓存机制
    private GameWorld lastGameWorld;
    private boolean needsRedraw = true;

    // 聊天气泡相关变量
    private ChatBubble activeChatBubble;
    private long chatBubbleStartTime;
    private static final long CHAT_BUBBLE_DURATION = 2500; // 气泡显示时长（毫秒）

    // 添加排行榜数据
    private Leaderboard leaderboard;
    // 本地玩家信息用于在画布上渲染（id->nickname/color）
    private List<Player> leaderboardPlayers = new ArrayList<>();

//-------------------------------------------------------------------

    public GameSurfaceView(Context context) {
        super(context);
        init();
    }
    
    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public GameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        // 不再加载图片，使用代码绘制
    }
    
    // 移除图片加载方法，使用代码绘制
    
    public void updateGameWorld(GameWorld gameWorld) {
        android.util.Log.d("GameSurfaceView", "接收到游戏世界更新");
        this.gameWorld = gameWorld;
        calculateGridLayout();
        
        // 直接绘制，不使用动画
        draw();
        
        this.lastGameWorld = gameWorld;
        android.util.Log.d("GameSurfaceView", "游戏世界更新完成");
    }
    
        // ...existing code...
        private void calculateGridLayout() {
            if (gameWorld == null || getWidth() == 0 || getHeight() == 0) return;
            
            // 计算最优的格子大小和行列数
            int baseGridSize = gameWorld.getGridSize(); // 基准大小，比如20
            
            // 根据屏幕比例计算行列数
            float screenRatio = (float) getWidth() / getHeight();
            
            if (screenRatio > 1.0f) {
                // 横屏或接近方形 - 列数更多
                gridCols = (int) (baseGridSize * screenRatio);
                gridRows = baseGridSize;
            } else {
                // 竖屏 - 行数更多
                gridCols = baseGridSize;
                gridRows = (int) (baseGridSize / screenRatio);
            }
            
            // 确保最小值
            gridCols = Math.max(gridCols, 15);
            gridRows = Math.max(gridRows, 15);
            
            // 计算每个格子的大小（保持方形）
            int cellSizeByWidth = getWidth() / gridCols;
            int cellSizeByHeight = getHeight() / gridRows;
            cellSize = Math.min(cellSizeByWidth, cellSizeByHeight);
            
            // 计算实际游戏区域大小和居中偏移
            int gameAreaWidth = gridCols * cellSize;
            int gameAreaHeight = gridRows * cellSize;
            offsetX = (getWidth() - gameAreaWidth) / 2;
            offsetY = (getHeight() - gameAreaHeight) / 2;
    
            // 新增：同步视图的行列数到 GameWorld，保证 updateViewToCenter 使用正确的值
            if (gameWorld != null) {
                gameWorld.setGridCols(gridCols);
                gameWorld.setGridRows(gridRows);
            }
        }
    // ...existing code...
    
    private void draw() {
        if (surfaceHolder == null) {
            android.util.Log.e("GameSurfaceView", "surfaceHolder为null");
            return;
        }
        
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                try {
                    drawGame(canvas);
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            } else {
                android.util.Log.e("GameSurfaceView", "无法获取canvas");
            }
        } else {
            android.util.Log.e("GameSurfaceView", "Surface无效");
        }
    }
    
    private void drawGame(Canvas canvas) {
        // 清空画布 - 使用深灰色背景
        canvas.drawColor(Color.parseColor("#2A2A2A"));

        if (gameWorld == null || cellSize == 0) {
            android.util.Log.e("GameSurfaceView", "gameWorld为null或cellSize为0");
            return;
        }

        // 绘制地图边界
        drawMapBoundary(canvas);

        // 绘制食物
        drawFood(canvas);

        // 绘制玩家的蛇
        drawSnake(canvas, gameWorld.getMySnake(), true);

        // 绘制其他玩家的蛇
        if (gameWorld.getOtherSnakes() != null) {
            android.util.Log.d("GameSurfaceView", "绘制其他玩家的蛇，数量: " + gameWorld.getOtherSnakes().size());
            for (Snake snake : gameWorld.getOtherSnakes()) {
                if (snake != null && snake.isAlive()) {
                    drawSnake(canvas, snake, false);
                }
            }
        } else {
            android.util.Log.d("GameSurfaceView", "没有其他玩家的蛇");
        }

        // 绘制聊天气泡
        drawChatBubble(canvas);

        // 绘制排行榜
        drawLeaderboard(canvas);
    }
    
    private void drawGrid(Canvas canvas) {
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(1);
        
        // 绘制垂直线
        for (int i = 0; i <= gridCols; i++) {
            int x = offsetX + i * cellSize;
            canvas.drawLine(x, offsetY, x, offsetY + gridRows * cellSize, paint);
        }
        
        // 绘制水平线
        for (int i = 0; i <= gridRows; i++) {
            int y = offsetY + i * cellSize;
            canvas.drawLine(offsetX, y, offsetX + gridCols * cellSize, y, paint);
        }
    }
    
    private void drawMapBoundary(Canvas canvas) {
        if (gameWorld == null) return;
        
        paint.setColor(Color.RED);
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);
        
        // 计算世界地图边界在当前视野中的位置
        int worldCols = gameWorld.getWorldMapCols();
        int worldRows = gameWorld.getWorldMapRows();
        int viewOffsetX = gameWorld.getViewOffsetX();
        int viewOffsetY = gameWorld.getViewOffsetY();
        
        // 左边界
        if (viewOffsetX <= 0) {
            int x = offsetX - viewOffsetX * cellSize;
            canvas.drawLine(x, offsetY, x, offsetY + gridRows * cellSize, paint);
        }
        
        // 右边界
        if (viewOffsetX + gridCols >= worldCols) {
            int x = offsetX + (worldCols - viewOffsetX) * cellSize;
            canvas.drawLine(x, offsetY, x, offsetY + gridRows * cellSize, paint);
        }
        
        // 上边界
        if (viewOffsetY <= 0) {
            int y = offsetY - viewOffsetY * cellSize;
            canvas.drawLine(offsetX, y, offsetX + gridCols * cellSize, y, paint);
        }
        
        // 下边界
        if (viewOffsetY + gridRows >= worldRows) {
            int y = offsetY + (worldRows - viewOffsetY) * cellSize;
            canvas.drawLine(offsetX, y, offsetX + gridCols * cellSize, y, paint);
        }
        
        // 恢复画笔样式
        paint.setStyle(Paint.Style.FILL);
    }
    
private void drawFood(Canvas canvas) {
    if (gameWorld == null || gameWorld.getFoods() == null) {
        android.util.Log.d("GameSurfaceView", "没有食物数据");
        return;
    }
    
    android.util.Log.d("GameSurfaceView", "绘制食物，数量: " + gameWorld.getFoods().size());
    
    // 创建食物列表的副本以避免并发修改异常
    List<Food> foodsCopy;
    try {
        foodsCopy = new ArrayList<>(gameWorld.getFoods());
    } catch (Exception e) {
        android.util.Log.e("GameSurfaceView", "复制食物列表时出错: " + e.getMessage());
        return;
    }
    
    for (Food food : foodsCopy) {
        Point worldPos = food.getPosition();
        if (worldPos != null) { // 移除视野检查，确保所有食物都能被绘制
            // 转换为视野坐标
            Point viewPos = new Point(worldPos.getX() - gameWorld.getViewOffsetX(), 
                                      worldPos.getY() - gameWorld.getViewOffsetY());
            
            float centerX = offsetX + viewPos.getX() * cellSize + cellSize / 2f;
            float centerY = offsetY + viewPos.getY() * cellSize + cellSize / 2f;
            
            // 根据食物类型绘制不同的形状
            switch (food.getType()) {
                case APPLE:
                    // 红色圆形苹果
                    paint.setColor(Color.parseColor("#FF4444"));
                    float appleRadius = cellSize / 3f;
                    canvas.drawCircle(centerX, centerY, appleRadius, paint);
                    
                    // 绘制苹果的叶子（绿色小矩形）
                    paint.setColor(Color.parseColor("#4CAF50"));
                    float leafSize = cellSize / 8f;
                    canvas.drawRect(centerX - leafSize/2, centerY - appleRadius - leafSize, 
                                   centerX + leafSize/2, centerY - appleRadius, paint);
                    break;
                    
                case GOOD_FOOD:
                    // 金色五角星
                    paint.setColor(Color.parseColor("#FFD700"));
                    drawStar(canvas, centerX, centerY, cellSize / 3f, paint);
                    break;
                    
                case BAD_FOOD:
                    // 紫色骷髅头
                    paint.setColor(Color.parseColor("#9C27B0"));
                    float skullRadius = cellSize / 3f;
                    canvas.drawCircle(centerX, centerY, skullRadius, paint);
                    
                    // 绘制眼睛
                    paint.setColor(Color.BLACK);
                    float eyeRadius = cellSize / 12f;
                    canvas.drawCircle(centerX - skullRadius/2, centerY - skullRadius/3, eyeRadius, paint);
                    canvas.drawCircle(centerX + skullRadius/2, centerY - skullRadius/3, eyeRadius, paint);
                    
                    // 绘制嘴巴
                    canvas.drawRect(centerX - skullRadius/3, centerY + skullRadius/4, 
                                   centerX + skullRadius/3, centerY + skullRadius/2, paint);
                    break;
            }
        }
    }
}
    // 绘制五角星的辅助方法
    private void drawStar(Canvas canvas, float centerX, float centerY, float radius, Paint paint) {
        paint.setAntiAlias(true);
        
        // 简化的星星绘制：绘制一个实心五角星
        Path starPath = new Path();
        
        // 计算五个外部点和五个内部点
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * i / 5.0;
            float r = (i % 2 == 0) ? radius : radius * 0.5f;
            float x = centerX + (float)(r * Math.cos(angle - Math.PI / 2));
            float y = centerY + (float)(r * Math.sin(angle - Math.PI / 2));
            
            if (i == 0) {
                starPath.moveTo(x, y);
            } else {
                starPath.lineTo(x, y);
            }
        }
        starPath.close();
        
        canvas.drawPath(starPath, paint);
    }
    
    private void drawSnake(Canvas canvas, Snake snake, boolean isMySnake) {
    if (snake == null || snake.getBodyPoints() == null) {
        android.util.Log.d("GameSurfaceView", "蛇或蛇的身体点为null");
        return;
    }
    
    android.util.Log.d("GameSurfaceView", "绘制蛇: " + snake.getNickname() + 
                      " 身体点数: " + snake.getBodyPoints().size() + 
                      " 是否存活: " + snake.isAlive());

    List<Point> bodyPoints = snake.getBodyPoints();
    
    for (int i = 0; i < bodyPoints.size(); i++) {
        Point worldPoint = bodyPoints.get(i);
        
        // 转换为视野坐标
        Point viewPoint = gameWorld.worldToViewport(worldPoint);
        
        // 检查是否在视野范围内
        if (viewPoint.getX() >= -1 && viewPoint.getX() <= gridCols + 1 && 
            viewPoint.getY() >= -1 && viewPoint.getY() <= gridRows + 1) {
            drawSnakeSegment(canvas, viewPoint, snake.getColor(), i == 0, isMySnake, snake);
        }
    }
}
    

    // 专门为动画蛇头设计的绘制方法，直接使用浮点坐标
    private void drawSnakeSegmentWithFloatCoords(Canvas canvas, float worldX, float worldY, String color, boolean isHead, boolean isMySnake) {
        // 转换为视野坐标（浮点数）
        float viewX = worldX - gameWorld.getViewOffsetX();
        float viewY = worldY - gameWorld.getViewOffsetY();
        
        // 检查是否在视野范围内
        if (viewX < -1 || viewX > gridCols || viewY < -1 || viewY > gridRows) {
            return; // 超出视野范围，不绘制
        }
        
        // 计算精确的像素位置
        float pixelX = offsetX + viewX * cellSize;
        float pixelY = offsetY + viewY * cellSize;
        
        if (isHead) {
            // 绘制蛇头 - 使用抗锯齿
            paint.setColor(Color.parseColor(color));
            paint.setAlpha(255);
            paint.setAntiAlias(true);
            canvas.drawRoundRect(
                pixelX + 1,
                pixelY + 1,
                pixelX + cellSize - 1,
                pixelY + cellSize - 1,
                cellSize * 0.2f, cellSize * 0.2f,
                paint
            );
        } else {
            // 绘制蛇身
            paint.setColor(Color.parseColor(color));
            if (isMySnake) {
                paint.setAlpha(180);
            } else {
                paint.setAlpha(150);
            }
            paint.setAntiAlias(true);
            
            canvas.drawRoundRect(
                pixelX + 3,
                pixelY + 3,
                pixelX + cellSize - 3,
                pixelY + cellSize - 3,
                cellSize * 0.15f, cellSize * 0.15f,
                paint
            );
            paint.setAlpha(255);
        }
    }

    private void drawSnakeSegment(Canvas canvas, Point viewPoint, String color, boolean isHead, boolean isMySnake, Snake snake) {
        // 计算精确的像素位置，减少浮点误差
        float pixelX = offsetX + viewPoint.getX() * cellSize;
        float pixelY = offsetY + viewPoint.getY() * cellSize;
        
        if (isHead) {
            // 绘制蛇头 - 使用抗锯齿
            paint.setColor(Color.parseColor(color));
            paint.setAlpha(255);
            paint.setAntiAlias(true);
            canvas.drawRoundRect(
                pixelX + 1,
                pixelY + 1,
                pixelX + cellSize - 1,
                pixelY + cellSize - 1,
                cellSize * 0.2f, cellSize * 0.2f,
                paint
            );
            
            // 在蛇头上方绘制玩家昵称
            drawPlayerNickname(canvas, snake, pixelX, pixelY);
        } else {
            // 绘制蛇身
            paint.setColor(Color.parseColor(color));
            if (isMySnake) {
                paint.setAlpha(180);
            } else {
                paint.setAlpha(150);
            }
            paint.setAntiAlias(true);
            
            canvas.drawRoundRect(
                pixelX + 3,
                pixelY + 3,
                pixelX + cellSize - 3,
                pixelY + cellSize - 3,
                cellSize * 0.15f, cellSize * 0.15f,
                paint
            );
            paint.setAlpha(255);
        }
    }
    
    private boolean isPointInBounds(Point point) {
        return point.getX() >= 0 && point.getX() < gridCols && 
               point.getY() >= 0 && point.getY() < gridRows;
    }
    
    // 获取当前网格大小供游戏逻辑使用
    public int getGridCols() {
        return gridCols;
    }
    
    public int getGridRows() {
        return gridRows;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();
            
            // 检查是否点击了快捷语句按钮 - 圆形按钮
            float buttonCenterX = getWidth() - 70; // 更新按钮位置
            float buttonCenterY = getHeight() / 2f;
            float buttonRadius = 65; // 更新按钮半径
            
            // 计算点击位置到按钮中心的距离
            float distance = (float) Math.sqrt(
                Math.pow(touchX - buttonCenterX, 2) + Math.pow(touchY - buttonCenterY, 2)
            );
            
        }
        
        if (gameWorld == null || !gameWorld.isGameRunning()) {
            return false; // 返回false让MainActivity处理点击开始游戏
        }
        
        // 游戏运行中不处理任何触摸事件，只使用方向键控制
        return false;
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Surface创建时的处理
        android.util.Log.d("GameSurfaceView", "Surface创建");
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Surface改变时的处理，重新计算布局
        android.util.Log.d("GameSurfaceView", "Surface改变: width=" + width + ", height=" + height);
        calculateGridLayout();
        draw(); // 重新绘制
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface销毁时的处理，无需清理图片资源
        android.util.Log.d("GameSurfaceView", "Surface销毁");
    }
    
    private void cleanupResources() {
        // 移除图片资源清理，改为代码绘制
    }
    
    /**
     * 根据蛇的移动方向绘制玩家昵称
     */
    private void drawPlayerNickname(Canvas canvas, Snake snake, float pixelX, float pixelY) {
        if (snake == null || snake.getNickname() == null || snake.getNickname().isEmpty()) {
            return;
        }
        
        String nickname = snake.getNickname();
        String direction = snake.getDirection();
        
        // 设置文字画笔 - 调整文字大小，保持可读性
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(cellSize * 0.5f); // 稍微减小文字大小避免过大
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); // 使用粗体字
        textPaint.setShadowLayer(3, 1, 1, Color.BLACK); // 阴影效果
        
        // 根据蛇的移动方向计算昵称显示位置，昵称显示在蛇头前进方向上
        float textX = pixelX + cellSize / 2f; // 水平居中（默认）
        float textY = pixelY - cellSize * 0.8f; // 默认在上方
        
        // 根据移动方向调整昵称位置 - 昵称显示在蛇头前进的方向上，保持一致的距离
        float offset = cellSize * 0.4f; // 统一的偏移距离
        if (direction != null) {
            switch (direction) {
                case "UP":
                    // 向上移动时，昵称显示在蛇头上方
                    textX = pixelX + cellSize / 2f;
                    textY = pixelY - offset; // 蛇头上方，统一距离
                    break;
                case "DOWN":
                    // 向下移动时，昵称显示在蛇头下方
                    textX = pixelX + cellSize / 2f;
                    textY = pixelY + cellSize + offset; // 蛇头下方，统一距离
                    break;
                case "LEFT":
                    // 向左移动时，昵称显示在蛇头左侧
                    textX = pixelX - offset; // 蛇头左侧，统一距离
                    textY = pixelY + cellSize * 0.7f; // 垂直稍微偏下
                    break;
                case "RIGHT":
                    // 向右移动时，昵称显示在蛇头右侧
                    textX = pixelX + cellSize + offset; // 蛇头右侧，统一距离
                    textY = pixelY + cellSize * 0.7f; // 垂直稍微偏下
                    break;
                default:
                    // 默认在上方
                    textX = pixelX + cellSize / 2f;
                    textY = pixelY - offset;
                    break;
            }
        }
        
        // 绘制背景框
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAlpha(140); // 适中的背景不透明度
        
        // 测量文字宽度和高度
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textWidth = textPaint.measureText(nickname);
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        
        // 绘制背景矩形
        float padding = cellSize * 0.08f; // 适中的内边距
        canvas.drawRoundRect(
            textX - textWidth / 2f - padding,
            textY - textHeight + fontMetrics.bottom - padding,
            textX + textWidth / 2f + padding,
            textY + fontMetrics.bottom + padding,
            padding, padding,
            backgroundPaint
        );
        
        // 绘制昵称文字
        canvas.drawText(nickname, textX, textY, textPaint);
    }
    
   
    
    /**
     * 聊天气泡数据类
     */
    private static class ChatBubble {
        String message;
        long timestamp;
        
        ChatBubble(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }


    /**
     * 玩家聊天气泡数据类
     */
    private static class PlayerChatBubble {
        int playerId;
        String message;
        long timestamp;
        
        PlayerChatBubble(int playerId, String message) {
            this.playerId = playerId;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    // 存储所有玩家的聊天气泡
    private List<PlayerChatBubble> playerChatBubbles = new ArrayList<>();
    
    
    /**
     * 显示聊天气泡
     */
    public void showChatBubble(String message) {
        activeChatBubble = new ChatBubble(message);
        chatBubbleStartTime = System.currentTimeMillis();
        invalidate(); // 触发重绘
    }


    /**
     * 显示指定玩家的聊天气泡
     */
    public void showPlayerChatBubble(int playerId, String message) {
        // 添加新的玩家气泡
        playerChatBubbles.add(new PlayerChatBubble(playerId, message));
        invalidate(); // 触发重绘
    }

   // ... existing code ...
    // 绘制聊天气泡
    private void drawChatBubble(Canvas canvas) {
        long currentTime = System.currentTimeMillis();
        
        // 绘制本地玩家的聊天气泡
        if (activeChatBubble != null) {
            if (currentTime - chatBubbleStartTime > CHAT_BUBBLE_DURATION) {
                activeChatBubble = null; // 气泡过期，清除
            } else if (gameWorld != null && gameWorld.getMySnake() != null) {
                Snake mySnake = gameWorld.getMySnake();
                List<Point> bodyPoints = mySnake.getBodyPoints();
                if (bodyPoints != null && !bodyPoints.isEmpty()) {
                    Point head = bodyPoints.get(0);
                    // 转换为视野坐标
                    Point viewPoint = new Point(head.getX() - gameWorld.getViewOffsetX(), 
                                                head.getY() - gameWorld.getViewOffsetY());
                    float screenX = offsetX + viewPoint.getX() * cellSize;
                    float screenY = offsetY + viewPoint.getY() * cellSize;
                    
                    // 调整气泡位置，确保在蛇头上方
                    drawChatBubbleAt(canvas, activeChatBubble.message, screenX + cellSize / 2f, screenY - cellSize * 2.0f);
                }
            }
        }
        
        // 绘制其他玩家的聊天气泡
        Iterator<PlayerChatBubble> iterator = playerChatBubbles.iterator();
        while (iterator.hasNext()) {
            PlayerChatBubble bubble = iterator.next();
            
            // 检查气泡是否过期
            if (currentTime - bubble.timestamp > CHAT_BUBBLE_DURATION) {
                iterator.remove(); // 过期则移除
                continue;
            }
            
            // 查找对应的玩家蛇
            if (gameWorld != null && gameWorld.getOtherSnakes() != null) {
                for (Snake snake : gameWorld.getOtherSnakes()) {
                    if (snake != null && snake.getPlayerId() == bubble.playerId && snake.isAlive()) {
                        List<Point> bodyPoints = snake.getBodyPoints();
                        if (bodyPoints != null && !bodyPoints.isEmpty()) {
                            Point head = bodyPoints.get(0);
                            // 转换为视野坐标
                            Point viewPoint = new Point(head.getX() - gameWorld.getViewOffsetX(), 
                                                        head.getY() - gameWorld.getViewOffsetY());
                            float screenX = offsetX + viewPoint.getX() * cellSize;
                            float screenY = offsetY + viewPoint.getY() * cellSize;
                            
                            // 调整气泡位置，确保在蛇头上方
                            drawChatBubbleAt(canvas, bubble.message, screenX + cellSize / 2f, screenY - cellSize * 2.0f);
                        }
                        break; // 找到对应蛇后跳出循环
                    }
                }
            }
        }
    }
// ... existing code ...


    // 在指定位置绘制聊天气泡
    private void drawChatBubbleAt(Canvas canvas, String message, float x, float y) {
        Paint bubblePaint = new Paint();
        bubblePaint.setAntiAlias(true);
        bubblePaint.setColor(Color.parseColor("#00BCD4")); // 气泡背景颜色（青色）
        bubblePaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE); // 气泡文字颜色
        textPaint.setTextSize(cellSize * 0.5f); // 调整文字大小
        textPaint.setTextAlign(Paint.Align.CENTER);

        // 测量文字宽度和高度
        float textWidth = textPaint.measureText(message);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textHeight = fontMetrics.bottom - fontMetrics.top;

        // 增加气泡的内边距
        float padding = cellSize * 0.4f;

        // 绘制气泡背景
        canvas.drawRoundRect(
            x - textWidth / 2 - padding,
            y - textHeight / 2 - padding,
            x + textWidth / 2 + padding,
            y + textHeight / 2 + padding,
            padding, padding,
            bubblePaint
        );

        // 绘制气泡文字
        canvas.drawText(message, x, y - (fontMetrics.ascent + fontMetrics.descent) / 2, textPaint);
    }

    // 更新排行榜数据的方法
    public void updateLeaderboard(Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
        invalidate(); // 触发重绘
    }

    // 根据 InRoom 更新玩家显示（不修改样式，仅填充玩家元数据）
    public void updatePlayersFromInRoom(InRoom inRoom) {
        if (inRoom == null) return;
        leaderboardPlayers.clear();
        try {
            // InRoom 结构中可能包含 parallel arrays: player_ids, nicknames, colors
            int count = 0;
            if (inRoom.player_ids != null) count = inRoom.player_ids.length();
            for (int i = 0; i < count; i++) {
                Player p = new Player();
                p.setPlayerId((int) inRoom.player_ids.get_at(i));
                if (inRoom.player_nicknames != null && i < inRoom.player_nicknames.length()) {
                    p.setNickname(inRoom.player_nicknames.get_at(i));
                }
                // InRoom doesn't include colors in current IDL; keep default color
                leaderboardPlayers.add(p);
            }
        } catch (Throwable t) {
            // 忽略解析错误
        }
        invalidate();
    }

    // 绘制排行榜
    private void drawLeaderboard(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        int x = offsetX + 20;
        int y = offsetY + 50;

        canvas.drawText("排行榜:", x, y, paint);
        y += 50;

        // 优先使用 DDS Leaderboard，如果没有则使用由 InRoom 提供的玩家列表
        if (leaderboard != null && leaderboard.entries != null) {
            for (int i = 0; i < leaderboard.entries.length(); i++) {
                LeaderboardEntry entry = leaderboard.entries.get_at(i);
                String txt = (i+1) + ". " + entry.nickname + " (" + entry.score + ")";
                canvas.drawText(txt, x, y, paint);
                y += 40;
            }
        } else if (leaderboardPlayers != null && !leaderboardPlayers.isEmpty()) {
            int rank = 1;
            for (Player p : leaderboardPlayers) {
                String name = p.getNickname() != null ? p.getNickname() : ("Player" + p.getPlayerId());
                String txt = rank + ". " + name;
                canvas.drawText(txt, x, y, paint);
                y += 40;
                rank++;
            }
        }
    }
}