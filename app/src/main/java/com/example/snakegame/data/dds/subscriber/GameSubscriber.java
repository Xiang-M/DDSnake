package com.example.snakegame.data.dds.subscriber;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import com.example.snakegame.data.model.Point;
import com.example.snakegame.DDSgenerated.*; // 导入 IDL 生成的类
import com.example.snakegame.data.model.Snake;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.infrastructure.*;
import com.zrdds.subscription.*;
import com.zrdds.topic.Topic;

import java.util.ArrayList;
import java.util.List;

/**
 * GameSubscriber - 使用 DataReaderListener 的具体实现
 */
public class GameSubscriber {
    private static final String TAG = "GameSubscriber";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    private DomainParticipant participant;
    private Subscriber subscriber;

    // Topics
    private Topic gameStateTopic, itemTopic, getFoodTopic, leaderBoardTopic, collisionTopic, systemMsgTopic;

    // DataReaders
    private GameStateDataReader gameStateReader;
    private ItemDataReader itemReader;
    private GetFoodDataReader getFoodReader;
    private LeaderboardDataReader leaderBoardReader;
    private CollisionDataReader collisionReader;
    private SystemMsgDataReader systemMsgReader;

    private int domain_id;

    // 定义各种监听器接口
    public interface OnLeaderboardReceivedListener {
        void onLeaderboardReceived(Leaderboard leaderboard);
    }

    public interface OnGameStateReceivedListener {
        void onGameStateReceived(GameState gameState);
    }

    public interface OnItemReceivedListener {
        void onItemReceived(Item item);
    }

    public interface OnGetFoodReceivedListener {
        void onGetFoodReceived(GetFood getFood);
    }

    public interface OnCollisionReceivedListener {
        void onCollisionReceived(Collision collision);
    }

    public interface OnSystemMsgReceivedListener {
        void onSystemMsgReceived(SystemMsg systemMsg);
    }

    // 监听器实例
    private OnLeaderboardReceivedListener leaderboardListener;
    private OnGameStateReceivedListener gameStateListener;
    private OnItemReceivedListener itemListener;
    private OnGetFoodReceivedListener getFoodListener;
    private OnCollisionReceivedListener collisionListener;
    private OnSystemMsgReceivedListener systemMsgListener;

    // 设置监听器的方法
    public void setOnLeaderboardReceivedListener(OnLeaderboardReceivedListener listener) {
        this.leaderboardListener = listener;
    }

    public void setOnGameStateReceivedListener(OnGameStateReceivedListener listener) {
        this.gameStateListener = listener;
    }

    public void setOnItemReceivedListener(OnItemReceivedListener listener) {
        this.itemListener = listener;
    }

    public void setOnGetFoodReceivedListener(OnGetFoodReceivedListener listener) {
        this.getFoodListener = listener;
    }

    public void setOnCollisionReceivedListener(OnCollisionReceivedListener listener) {
        this.collisionListener = listener;
    }

    public void setOnSystemMsgReceivedListener(OnSystemMsgReceivedListener listener) {
        this.systemMsgListener = listener;
    }

    public GameSubscriber(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        this.domain_id = sharedPreferences.getInt("domain_id", 0);
        initDDS();
    }

    private void initDDS() {
        try {
            participant = DomainParticipantFactory.get_instance().create_participant(
                    domain_id,
                    DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (participant == null) {
                Toast.makeText(context, "创建 DomainParticipant 失败", Toast.LENGTH_SHORT).show();
                return;
            }

            subscriber = participant.create_subscriber(
                    DomainParticipant.SUBSCRIBER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (subscriber == null) {
                Toast.makeText(context, "创建 Subscriber 失败", Toast.LENGTH_SHORT).show();
                return;
            }

            subscribeGameState();
            subscribeItem();
            subscribeGetFood();
            subscribeLeaderBoard();
            subscribeCollision();
            subscribeSystemMsg();

            Log.i(TAG, "DDS Subscriber 初始化完成，domain_id=" + domain_id);
        } catch (Throwable t) {
            Log.e(TAG, "initDDS 出错", t);
            Toast.makeText(context, "DDS 初始化失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
            cleanup();
        }
    }

    // -------------------- subscribe helpers --------------------
    private void subscribeGameState() {
        GameStateTypeSupport ts = (GameStateTypeSupport) GameStateTypeSupport.get_instance();
        if (ts.register_type(participant, null) != ReturnCode_t.RETCODE_OK) return;

        gameStateTopic = participant.create_topic("GAMESTATE", ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);

        gameStateReader = (GameStateDataReader) subscriber.create_datareader(
                gameStateTopic, Subscriber.DATAREADER_QOS_DEFAULT, new GameStateListener(this),
                StatusKind.DATA_AVAILABLE_STATUS
        );
    }

    private void subscribeItem() {
        ItemTypeSupport ts = (ItemTypeSupport) ItemTypeSupport.get_instance();
        if (ts.register_type(participant, null) != ReturnCode_t.RETCODE_OK) return;

        itemTopic = participant.create_topic("ITEM", ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);

        itemReader = (ItemDataReader) subscriber.create_datareader(
                itemTopic, Subscriber.DATAREADER_QOS_DEFAULT, new ItemListener(this),
                StatusKind.DATA_AVAILABLE_STATUS
        );
    }

    private void subscribeGetFood() {
        GetFoodTypeSupport ts = (GetFoodTypeSupport) GetFoodTypeSupport.get_instance();
        if (ts.register_type(participant, null) != ReturnCode_t.RETCODE_OK) return;

        getFoodTopic = participant.create_topic("GETFOOD", ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);

        getFoodReader = (GetFoodDataReader) subscriber.create_datareader(
                getFoodTopic, Subscriber.DATAREADER_QOS_DEFAULT, new GetFoodListener(this),
                StatusKind.DATA_AVAILABLE_STATUS
        );
    }

    private void subscribeLeaderBoard() {
        LeaderboardTypeSupport ts = (LeaderboardTypeSupport) LeaderboardTypeSupport.get_instance();
        if (ts.register_type(participant, null) != ReturnCode_t.RETCODE_OK) return;

        leaderBoardTopic = participant.create_topic("LEADERBOARD", ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);

        leaderBoardReader = (LeaderboardDataReader) subscriber.create_datareader(
                leaderBoardTopic, Subscriber.DATAREADER_QOS_DEFAULT, new LeaderBoardListener(this),
                StatusKind.DATA_AVAILABLE_STATUS
        );
    }

    private void subscribeCollision() {
        CollisionTypeSupport ts = (CollisionTypeSupport) CollisionTypeSupport.get_instance();
        if (ts.register_type(participant, null) != ReturnCode_t.RETCODE_OK) return;

        collisionTopic = participant.create_topic("COLLISION", ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);

        collisionReader = (CollisionDataReader) subscriber.create_datareader(
                collisionTopic, Subscriber.DATAREADER_QOS_DEFAULT, new CollisionListener(this),
                StatusKind.DATA_AVAILABLE_STATUS
        );
    }

    private void subscribeSystemMsg() {
        SystemMsgTypeSupport ts = (SystemMsgTypeSupport) SystemMsgTypeSupport.get_instance();
        if (ts.register_type(participant, null) != ReturnCode_t.RETCODE_OK) return;

        systemMsgTopic = participant.create_topic("SYSTEMMSG", ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);

        systemMsgReader = (SystemMsgDataReader) subscriber.create_datareader(
                systemMsgTopic, Subscriber.DATAREADER_QOS_DEFAULT, new SystemMsgListener(this),
                StatusKind.DATA_AVAILABLE_STATUS
        );
    }

    // -------------------- Listeners --------------------
    private static class GameStateListener implements DataReaderListener {
        private final GameSubscriber outer;

        GameStateListener(GameSubscriber outer) {
            this.outer = outer;
        }

        @Override
        public void on_data_available(DataReader reader) {
            try {
                GameStateDataReader dr = (GameStateDataReader) reader;
                GameStateSeq dataSeq = new GameStateSeq();
                SampleInfoSeq infoSeq = new SampleInfoSeq();
                ReturnCode_t rc = dr.take(dataSeq, infoSeq,
                        -1, // LENGTH_UNLIMITED
                        SampleStateKind.ANY_SAMPLE_STATE,
                        ViewStateKind.ANY_VIEW_STATE,
                        InstanceStateKind.ANY_INSTANCE_STATE);

                if (rc == ReturnCode_t.RETCODE_OK) {
                    for (int i = 0; i < dataSeq.length(); i++) {
                        if (infoSeq.get_at(i).valid_data) {
                            GameState gs = dataSeq.get_at(i);
                            Log.d(TAG, "GAMESTATE received: "  + gs.player_id);

                            // 通知业务层
                            if (outer.gameStateListener != null) {
                                outer.gameStateListener.onGameStateReceived(gs);
                            }
                        }
                    }
                }
                dr.return_loan(dataSeq, infoSeq);
            } catch (Throwable t) {
                Log.e(TAG, "GameStateListener error", t);
            }
        }

        @Override public void on_data_arrived(DataReader arg0, Object arg1, SampleInfo arg2) {}
        @Override public void on_requested_deadline_missed(DataReader r, RequestedDeadlineMissedStatus s) {}
        @Override public void on_requested_incompatible_qos(DataReader r, RequestedIncompatibleQosStatus s) {}
        @Override public void on_sample_rejected(DataReader r, SampleRejectedStatus s) {}
        @Override public void on_liveliness_changed(DataReader r, LivelinessChangedStatus s) {}
        @Override public void on_sample_lost(DataReader r, SampleLostStatus s) {}
        @Override public void on_subscription_matched(DataReader r, SubscriptionMatchedStatus s) {}
    }

    private static class ItemListener implements DataReaderListener {
        private final GameSubscriber outer;

        ItemListener(GameSubscriber outer) {
            this.outer = outer;
        }

        @Override
        public void on_data_available(DataReader reader) {
            try {
                ItemDataReader dr = (ItemDataReader) reader;
                ItemSeq dataSeq = new ItemSeq();
                SampleInfoSeq infoSeq = new SampleInfoSeq();
                dr.take(dataSeq, infoSeq, -1,
                        SampleStateKind.ANY_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ANY_INSTANCE_STATE);
                for (int i = 0; i < dataSeq.length(); i++) {
                    if (infoSeq.get_at(i).valid_data) {
                        Item it = dataSeq.get_at(i);

                        ItemType t = ItemType.valueOf(it.item_type.ordinal());
                        String typeName = "UNKNOWN";
                        if (t == ItemType.APPLE) typeName = "APPLE";
                        else if (t == ItemType.GOOD_FOOD) typeName = "GOOD_FOOD";
                        else if (t == ItemType.BAD_FOOD) typeName = "BAD_FOOD";

                        Log.d(TAG, "ITEM received: "  + typeName);

                        // 通知业务层
                        if (outer.itemListener != null) {
                            outer.itemListener.onItemReceived(it);
                        }
                    }
                }
                dr.return_loan(dataSeq, infoSeq);
            } catch (Throwable t) {
                Log.e(TAG, "ItemListener error", t);
            }
        }
        @Override public void on_data_arrived(DataReader arg0, Object arg1, SampleInfo arg2) {}
        @Override public void on_requested_deadline_missed(DataReader r, RequestedDeadlineMissedStatus s) {}
        @Override public void on_requested_incompatible_qos(DataReader r, RequestedIncompatibleQosStatus s) {}
        @Override public void on_sample_rejected(DataReader r, SampleRejectedStatus s) {}
        @Override public void on_liveliness_changed(DataReader r, LivelinessChangedStatus s) {}
        @Override public void on_sample_lost(DataReader r, SampleLostStatus s) {}
        @Override public void on_subscription_matched(DataReader r, SubscriptionMatchedStatus s) {}
    }

    private static class GetFoodListener implements DataReaderListener {
        private final GameSubscriber outer;

        GetFoodListener(GameSubscriber outer) {
            this.outer = outer;
        }

        @Override
        public void on_data_available(DataReader reader) {
            try {
                GetFoodDataReader dr = (GetFoodDataReader) reader;
                GetFoodSeq dataSeq = new GetFoodSeq();
                SampleInfoSeq infoSeq = new SampleInfoSeq();
                dr.take(dataSeq, infoSeq, -1,
                        SampleStateKind.ANY_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ANY_INSTANCE_STATE);
                for (int i = 0; i < dataSeq.length(); i++) {
                    if (infoSeq.get_at(i).valid_data) {
                        GetFood gf = dataSeq.get_at(i);
                        Log.d(TAG, "GETFOOD received: " + gf);

                        // 通知业务层
                        if (outer.getFoodListener != null) {
                            outer.getFoodListener.onGetFoodReceived(gf);
                        }
                    }
                }
                Log.d(TAG, "GETFOOD ");
                dr.return_loan(dataSeq, infoSeq);
            } catch (Throwable t) {
                Log.e(TAG, "GetFoodListener error", t);
            }
        }
        @Override public void on_data_arrived(DataReader arg0, Object arg1, SampleInfo arg2) {}
        @Override public void on_requested_deadline_missed(DataReader r, RequestedDeadlineMissedStatus s) {}
        @Override public void on_requested_incompatible_qos(DataReader r, RequestedIncompatibleQosStatus s) {}
        @Override public void on_sample_rejected(DataReader r, SampleRejectedStatus s) {}
        @Override public void on_liveliness_changed(DataReader r, LivelinessChangedStatus s) {}
        @Override public void on_sample_lost(DataReader r, SampleLostStatus s) {}
        @Override public void on_subscription_matched(DataReader r, SubscriptionMatchedStatus s) {}
    }

    private static class LeaderBoardListener implements DataReaderListener {
        private final GameSubscriber outer;

        LeaderBoardListener(GameSubscriber outer) {
            this.outer = outer;
        }

        @Override
        public void on_data_available(DataReader reader) {
            try {
                LeaderboardDataReader dr = (LeaderboardDataReader) reader;
                LeaderboardSeq dataSeq = new LeaderboardSeq();
                SampleInfoSeq infoSeq = new SampleInfoSeq();

                dr.take(dataSeq, infoSeq, -1,
                        SampleStateKind.ANY_SAMPLE_STATE,
                        ViewStateKind.ANY_VIEW_STATE,
                        InstanceStateKind.ANY_INSTANCE_STATE);

                for (int i = 0; i < dataSeq.length(); i++) {
                    if (infoSeq.get_at(i).valid_data) {
                        Leaderboard lb = dataSeq.get_at(i);

                        // 打印调试日志
                        Log.d(TAG, "LEADERBOARD received, entries=" + lb.entries.length());
                        for (int j = 0; j < lb.entries.length(); j++) {
                            LeaderboardEntry entry = lb.entries.get_at(j);
                            Log.d(TAG, "玩家ID=" + entry.player_id +
                                    ", 昵称=" + entry.nickname +
                                    ", 分数=" + entry.score);
                        }

                        // 通知业务层
                        if (outer.leaderboardListener != null) {
                            outer.leaderboardListener.onLeaderboardReceived(lb);
                        }
                    }
                }
                dr.return_loan(dataSeq, infoSeq);
            } catch (Throwable t) {
                Log.e(TAG, "LeaderBoardListener error", t);
            }
        }

        @Override public void on_data_arrived(DataReader arg0, Object arg1, SampleInfo arg2) {}
        @Override public void on_requested_deadline_missed(DataReader r, RequestedDeadlineMissedStatus s) {}
        @Override public void on_requested_incompatible_qos(DataReader r, RequestedIncompatibleQosStatus s) {}
        @Override public void on_sample_rejected(DataReader r, SampleRejectedStatus s) {}
        @Override public void on_liveliness_changed(DataReader r, LivelinessChangedStatus s) {}
        @Override public void on_sample_lost(DataReader r, SampleLostStatus s) {}
        @Override public void on_subscription_matched(DataReader r, SubscriptionMatchedStatus s) {}
    }

    private static class CollisionListener implements DataReaderListener {
        private final GameSubscriber outer;

        CollisionListener(GameSubscriber outer) {
            this.outer = outer;
        }

        @Override
        public void on_data_available(DataReader reader) {
            try {
                CollisionDataReader dr = (CollisionDataReader) reader;
                CollisionSeq dataSeq = new CollisionSeq();
                SampleInfoSeq infoSeq = new SampleInfoSeq();
                dr.take(dataSeq, infoSeq, -1,
                        SampleStateKind.ANY_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ANY_INSTANCE_STATE);
                for (int i = 0; i < dataSeq.length(); i++) {
                    if (infoSeq.get_at(i).valid_data) {
                        Collision col = dataSeq.get_at(i);
                        Log.d(TAG, "COLLISION received: " + col);

                        // 通知业务层
                        if (outer.collisionListener != null) {
                            outer.collisionListener.onCollisionReceived(col);
                        }
                    }
                }
                dr.return_loan(dataSeq, infoSeq);
            } catch (Throwable t) {
                Log.e(TAG, "CollisionListener error", t);
            }
        }
        @Override public void on_data_arrived(DataReader arg0, Object arg1, SampleInfo arg2) {}
        @Override public void on_requested_deadline_missed(DataReader r, RequestedDeadlineMissedStatus s) {}
        @Override public void on_requested_incompatible_qos(DataReader r, RequestedIncompatibleQosStatus s) {}
        @Override public void on_sample_rejected(DataReader r, SampleRejectedStatus s) {}
        @Override public void on_liveliness_changed(DataReader r, LivelinessChangedStatus s) {}
        @Override public void on_sample_lost(DataReader r, SampleLostStatus s) {}
        @Override public void on_subscription_matched(DataReader r, SubscriptionMatchedStatus s) {}
    }

    private static class SystemMsgListener implements DataReaderListener {
        private final GameSubscriber outer;

        SystemMsgListener(GameSubscriber outer) {
            this.outer = outer;
        }

        @Override
        public void on_data_available(DataReader dataReader) {
            try {
                SystemMsgDataReader dr = (SystemMsgDataReader) dataReader;
                SystemMsgSeq dataSeq = new SystemMsgSeq();
                SampleInfoSeq infoSeq = new SampleInfoSeq();

                ReturnCode_t rtn = dr.take(
                        dataSeq,
                        infoSeq,
                        -1, // LENGTH_UNLIMITED
                        SampleStateKind.ANY_SAMPLE_STATE,
                        ViewStateKind.ANY_VIEW_STATE,
                        InstanceStateKind.ANY_INSTANCE_STATE
                );

                if (rtn == ReturnCode_t.RETCODE_OK) {
                    for (int i = 0; i < infoSeq.length(); ++i) {
                        if (!infoSeq.get_at(i).valid_data) continue;

                        SystemMsg sm = dataSeq.get_at(i);
                        Log.d(TAG, "SYSTEMMSG received: " + sm);

                        // 通知业务层
                        if (outer.systemMsgListener != null) {
                            outer.systemMsgListener.onSystemMsgReceived(sm);
                        }

                        if ("END".equals(sm.msg_type)) {
                            Log.i(TAG, "收到游戏结束消息");
                        }
                    }
                }
                dr.return_loan(dataSeq, infoSeq);
            } catch (Throwable t) {
                Log.e(TAG, "SystemMsgListener error", t);
            }
        }

        @Override public void on_data_arrived(DataReader arg0, Object arg1, SampleInfo arg2) {}
        @Override public void on_requested_deadline_missed(DataReader r, RequestedDeadlineMissedStatus s) {}
        @Override public void on_requested_incompatible_qos(DataReader r, RequestedIncompatibleQosStatus s) {}
        @Override public void on_sample_rejected(DataReader r, SampleRejectedStatus s) {}
        @Override public void on_liveliness_changed(DataReader r, LivelinessChangedStatus s) {}
        @Override public void on_sample_lost(DataReader r, SampleLostStatus s) {}
        @Override public void on_subscription_matched(DataReader r, SubscriptionMatchedStatus s) {}
    }

    // -------------------- cleanup --------------------
    public void cleanup() {
        Log.i(TAG, "正在清理 DDS 资源...");
        try {
            if (subscriber != null) {
                if (gameStateReader != null) subscriber.delete_datareader(gameStateReader);
                if (itemReader != null) subscriber.delete_datareader(itemReader);
                if (getFoodReader != null) subscriber.delete_datareader(getFoodReader);
                if (leaderBoardReader != null) subscriber.delete_datareader(leaderBoardReader);
                if (collisionReader != null) subscriber.delete_datareader(collisionReader);
                if (systemMsgReader != null) subscriber.delete_datareader(systemMsgReader);
            }

            if (participant != null && subscriber != null) participant.delete_subscriber(subscriber);

            if (participant != null) {
                if (gameStateTopic != null) participant.delete_topic(gameStateTopic);
                if (itemTopic != null) participant.delete_topic(itemTopic);
                if (getFoodTopic != null) participant.delete_topic(getFoodTopic);
                if (leaderBoardTopic != null) participant.delete_topic(leaderBoardTopic);
                if (collisionTopic != null) participant.delete_topic(collisionTopic);
                if (systemMsgTopic != null) participant.delete_topic(systemMsgTopic);

                DomainParticipantFactory.get_instance().delete_participant(participant);
                participant = null;
            }
        } catch (Throwable t) {
            Log.w(TAG, "清理 DDS 资源时出错", t);
        } finally {
            Log.i(TAG, "DDS 资源清理完成");
        }
    }
}