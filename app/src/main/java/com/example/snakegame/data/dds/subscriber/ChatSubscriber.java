package com.example.snakegame.data.dds.subscriber;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.snakegame.DDSgenerated.ChatMsg;
import com.example.snakegame.DDSgenerated.ChatMsgDataReader;
import com.example.snakegame.DDSgenerated.ChatMsgSeq;
import com.example.snakegame.DDSgenerated.ChatMsgTypeSupport;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.infrastructure.InstanceStateKind;
import com.zrdds.infrastructure.LivelinessChangedStatus;
import com.zrdds.infrastructure.RequestedDeadlineMissedStatus;
import com.zrdds.infrastructure.RequestedIncompatibleQosStatus;
import com.zrdds.infrastructure.ReturnCode_t;
import com.zrdds.infrastructure.SampleInfo;
import com.zrdds.infrastructure.SampleInfoSeq;
import com.zrdds.infrastructure.SampleLostStatus;
import com.zrdds.infrastructure.SampleRejectedStatus;
import com.zrdds.infrastructure.SampleStateKind;
import com.zrdds.infrastructure.StatusKind;
import com.zrdds.infrastructure.SubscriptionMatchedStatus;
import com.zrdds.infrastructure.ViewStateKind;
import com.zrdds.subscription.DataReader;
import com.zrdds.subscription.DataReaderListener;
import com.zrdds.subscription.Subscriber;
import com.zrdds.topic.Topic;

/**
 * DDS 聊天消息订阅类
 */
public class ChatSubscriber {
    private Context context;
    private SharedPreferences sharedPreferences;

    // DDS 相关
    private DomainParticipant participant;
    private Topic chatTopic;
    private DataReader chatReader;

    private long playerId;
    private int domain_id;

    // 业务层监听器
    public interface OnChatMessageReceivedListener {
        void onMessageReceived(ChatMsg msg);
    }
    private static OnChatMessageReceivedListener businessListener;

    public void setOnMessageReceivedListener(OnChatMessageReceivedListener listener) {
        businessListener = listener;
    }

    public ChatSubscriber(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);

        // 从 SharedPreferences 获取 player_id 和 domain_id
        this.playerId = sharedPreferences.getInt("player_id", -1);
        this.domain_id = sharedPreferences.getInt("domain_id", 0);

        if (playerId == -1) {
            Toast.makeText(context, "玩家未登录，无法接收消息", Toast.LENGTH_SHORT).show();
            return;
        }

        initDDS();
    }

    private void initDDS() {
        try {
            // 创建 DomainParticipant
            participant = DomainParticipantFactory.get_instance().create_participant(
                    domain_id,
                    DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            if (participant == null) {
                Log.e("ChatSubscriber", "创建 DomainParticipant 失败");
                return;
            }

            // 注册类型
            ChatMsgTypeSupport ts = (ChatMsgTypeSupport) ChatMsgTypeSupport.get_instance();
            ReturnCode_t ret = ts.register_type(participant, null);
            if (ret != ReturnCode_t.RETCODE_OK) {
                Log.e("ChatSubscriber", "注册类型失败");
                return;
            }

            // 创建 Topic
            chatTopic = participant.create_topic(
                    "BACKCHATMSG",
                    ts.get_type_name(),
                    DomainParticipant.TOPIC_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            // 创建 Subscriber
            Subscriber subscriber = participant.create_subscriber(
                    DomainParticipant.SUBSCRIBER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            // 创建 DataReader -> 订阅消息
            chatReader = subscriber.create_datareader(
                    chatTopic,
                    Subscriber.DATAREADER_QOS_DEFAULT,
                    new ChatMsgListener(), // 回调监听
                    StatusKind.DATA_AVAILABLE_STATUS
            );

            Log.d("ChatSubscriber", "DDS 初始化成功");

        } catch (Exception e) {
            Log.e("ChatSubscriber", "DDS 初始化失败", e);
        }
    }

    /**
     * DDS DataReader 回调监听器
     */
    private static class ChatMsgListener implements DataReaderListener {
        @Override
        public void on_data_available(DataReader dataReader) {
            try {
                ChatMsgDataReader dr = (ChatMsgDataReader) dataReader;
                ChatMsgSeq dataSeq = new ChatMsgSeq();
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

                        ChatMsg msg = dataSeq.get_at(i);
                        Log.d("ChatSubscriber", "接收到消息: " + msg.content);

                        // 转交给业务层
                        if (businessListener != null) {
                            businessListener.onMessageReceived(msg);
                        }
                    }
                }
                dr.return_loan(dataSeq, infoSeq);
            } catch (Throwable t) {
                Log.e("ChatMsgListener", "处理消息失败", t);
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
}
