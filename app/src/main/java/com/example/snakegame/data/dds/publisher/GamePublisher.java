package com.example.snakegame.data.dds.publisher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.snakegame.DDSgenerated.PlayerMove;
import com.example.snakegame.DDSgenerated.PlayerMoveDataWriter;
import com.example.snakegame.DDSgenerated.PlayerMoveTypeSupport;
import com.zrdds.infrastructure.ReturnCode_t;
import com.zrdds.infrastructure.StatusKind;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.publication.DataWriter;
import com.zrdds.publication.Publisher;
import com.zrdds.topic.Topic;

public class GamePublisher {

    private static final String TAG = "GamePublisher";

    private Context context;
    private SharedPreferences sharedPreferences;

    // DDS相关
    private DomainParticipant participant;
    private Publisher publisher;
    private Topic moveTopic;
    private PlayerMoveDataWriter moveWriter;

    private long playerId;
    private int domain_id;

    public GamePublisher(Context context, int playerId) { // 修改构造函数，接收玩家ID
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        this.playerId = playerId; // 保存玩家ID
        this.domain_id = sharedPreferences.getInt("domain_id", 0);
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
            PlayerMoveTypeSupport ts = (PlayerMoveTypeSupport) PlayerMoveTypeSupport.get_instance();
            ReturnCode_t ret = ts.register_type(participant, null);
            if (ret != ReturnCode_t.RETCODE_OK) {
                System.out.println("register type failed");
                return;
            }

            // 创建 Topic
            moveTopic = participant.create_topic(
                    "PLAYERMOVE",
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
            moveWriter = (PlayerMoveDataWriter) publisher.create_datawriter(
                    moveTopic,
                    Publisher.DATAWRITER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            Log.i(TAG, "DDS Publisher 初始化完成，domain_id=" + domain_id);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 提供给外部调用的发送接口
     */
    public void sendMove(String direction) {
        if (moveWriter == null) {
            Toast.makeText(context, "DDS未初始化，无法发送移动", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PlayerMove move = new PlayerMove();
            move.player_id = (int) this.playerId; // 使用实例变量中的玩家ID
            move.direction = direction;
            move.timestamp = (int) (System.currentTimeMillis() & 0xFFFFFFFF);

            ReturnCode_t ret = moveWriter.write(move, null);
            if (ret != ReturnCode_t.RETCODE_OK) {
                System.out.println("发送移动消息失败，ReturnCode: " + ret);
            } else {
                Log.d(TAG, "成功发送移动消息: " + direction + " 玩家ID: " + this.playerId);
            }
        } catch (Exception e) {
            Toast.makeText(context, "发送移动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static void cleanup(DomainParticipant dp, Publisher pub, DataWriter writer, Topic tp) {
        System.out.println("正在清理 DDS 资源...");
        try {
            if (pub != null && writer != null) {
                try {
                    pub.delete_datawriter(writer);
                    System.out.println("DataWriter 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 DataWriter 时出错: " + t.getMessage());
                    t.printStackTrace();
                }
                writer = null;
            }

            if (dp != null && pub != null) {
                try {
                    dp.delete_publisher(pub);
                    System.out.println("Publisher 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 Publisher 时出错: " + t.getMessage());
                    t.printStackTrace();
                }
                pub = null;
            }

            if (dp != null && tp != null) {
                try {
                    dp.delete_topic(tp);
                    System.out.println("Topic 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 Topic 时出错: " + t.getMessage());
                    t.printStackTrace();
                }
                tp = null;
            }

            if (dp != null) {
                try {
                    DomainParticipantFactory.get_instance().delete_participant(dp);
                    System.out.println("DomainParticipant 已删除");
                } catch (Throwable t) {
                    System.err.println("删除 DomainParticipant 时出错: " + t.getMessage());
                    t.printStackTrace();
                }
                dp = null;
            }

        } finally {
            System.out.println("DDS 资源清理完成");
        }
    }
}
