package com.example.snakegame.data.dds.publisher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.snakegame.DDSgenerated.ChatMsg;
import com.example.snakegame.DDSgenerated.ChatMsgDataWriter;
import com.example.snakegame.DDSgenerated.ChatMsgDataWriter;
import com.example.snakegame.DDSgenerated.ChatMsgTypeSupport;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.infrastructure.InstanceHandle_t;
import com.zrdds.infrastructure.ReturnCode_t;
import com.zrdds.infrastructure.StatusKind;
import com.zrdds.publication.DataWriter;
import com.zrdds.publication.Publisher;
import com.zrdds.topic.Topic;

public class ChatPublisher {

    private Context context;
    private SharedPreferences sharedPreferences;

    // DDS相关
    private DomainParticipant participant;
    private Publisher publisher;
    private Topic chatTopic;
    private ChatMsgDataWriter chatWriter;

    private int playerId;
    private int domain_id;

    public ChatPublisher(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);

        // 从SharedPreferences获取player_id和domain_id
        this.playerId = sharedPreferences.getInt("player_id",-1);
        this.domain_id = sharedPreferences.getInt("domain_id", 0);

        if (playerId == -1) {
            Toast.makeText(context, "玩家未登录，无法发送聊天消息", Toast.LENGTH_SHORT).show();
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
                System.out.println("create participant failed");
                return;
            }

            // 注册类型
            ChatMsgTypeSupport ts = (ChatMsgTypeSupport) ChatMsgTypeSupport.get_instance();
            ReturnCode_t ret = ts.register_type(participant, null);
            if (ret != ReturnCode_t.RETCODE_OK) {
                System.out.println("register type failed");
                return;
            }

            // 创建 Topic
            chatTopic = participant.create_topic(
                    "FRONTCHATMSG",  // topic 名字你可以改
                    ts.get_type_name(),
                    DomainParticipant.TOPIC_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            // 创建 Publisher
            publisher = participant.create_publisher(
                    DomainParticipant.PUBLISHER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            // 创建 DataWriter
            chatWriter = (ChatMsgDataWriter) publisher.create_datawriter(
                    chatTopic,
                    Publisher.DATAWRITER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

             Log.d("ChatPublisher", "DDS 初始化成功");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendChatMessage(String content) {
        if (chatWriter == null) {
            Toast.makeText(context, "DDS未初始化，无法发送聊天消息", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ChatMsg msg = new ChatMsg();
            msg.player_id = playerId; // IDL里是 long，但Java生成的通常是 int
            msg.content = content;
            msg.timestamp = (int) (System.currentTimeMillis() & 0xFFFFFFFF);

            ReturnCode_t ret = chatWriter.write(msg, null);
            if (ret != ReturnCode_t.RETCODE_OK) {
                System.out.println("发送聊天消息失败: " + ret);
            }

                Log.d( "ChatPublisher","发送聊天消息");

        } catch (Exception e) {
            Toast.makeText(context, "发送聊天消息异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static void cleanup(DomainParticipant dp, Publisher pub, DataWriter writer, Topic tp) {
        System.out.println("正在清理 ChatPublisher DDS 资源...");
        try {
            if (pub != null && writer != null) {
                try {
                    pub.delete_datawriter(writer);
                    System.out.println("DataWriter 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 DataWriter 出错: " + t.getMessage());
                    t.printStackTrace();
                }
                writer = null;
            }

            if (dp != null && pub != null) {
                try {
                    dp.delete_publisher(pub);
                    System.out.println("Publisher 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 Publisher 出错: " + t.getMessage());
                    t.printStackTrace();
                }
                pub = null;
            }

            if (dp != null && tp != null) {
                try {
                    dp.delete_topic(tp);
                    System.out.println("Topic 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 Topic 出错: " + t.getMessage());
                    t.printStackTrace();
                }
                tp = null;
            }

            if (dp != null) {
                try {
                    DomainParticipantFactory.get_instance().delete_participant(dp);
                    System.out.println("DomainParticipant 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 DomainParticipant 出错: " + t.getMessage());
                    t.printStackTrace();
                }
                dp = null;
            }

        } finally {
            System.out.println("ChatPublisher DDS 资源清理完成");
        }
    }
}
