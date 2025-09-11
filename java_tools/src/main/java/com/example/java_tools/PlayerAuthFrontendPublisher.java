package com.example.java_tools;

import com.example.java_tools.SnakeGame.PlayerAuth;
import com.example.java_tools.SnakeGame.PlayerAuthDataWriter;
import com.example.java_tools.SnakeGame.PlayerAuthTypeSupport;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.infrastructure.ReturnCode_t;
import com.zrdds.infrastructure.StatusKind;
import com.zrdds.publication.DataWriter;
import com.zrdds.publication.Publisher;
import com.zrdds.topic.Topic;

public class PlayerAuthFrontendPublisher {
    private static DomainParticipant dp;
    private static Publisher pub;
    private static Topic tp;
    private static PlayerAuthDataWriter writer;

    public static void main(String[] args) {
        loadLibrary();
        ReturnCode_t rtn;

        int domain_id = 80;

        // 1. 创建 DomainParticipant
        dp = DomainParticipantFactory.get_instance().create_participant(
                domain_id,
                DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (dp == null) {
            System.out.println("create dp failed");
            return;
        }

        // 2. 注册 PlayerAuth 类型
        PlayerAuthTypeSupport ts = (PlayerAuthTypeSupport) PlayerAuthTypeSupport.get_instance();
        rtn = ts.register_type(dp, null);
        if (rtn != ReturnCode_t.RETCODE_OK) {
            System.out.println("register type failed");
            return;
        }

        // 3. 创建 Topic
        tp = dp.create_topic(
                "PLAYERAUTH",
                ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (tp == null) {
            System.out.println("create topic failed");
            return;
        }

        // 4. 创建 Publisher
        pub = dp.create_publisher(
                DomainParticipant.PUBLISHER_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (pub == null) {
            System.out.println("create publisher failed");
            return;
        }

        // 5. 创建 DataWriter
        DataWriter dw = pub.create_datawriter(
                tp,
                Publisher.DATAWRITER_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (dw == null) {
            System.out.println("create datawriter failed");
            return;
        }
        writer = (PlayerAuthDataWriter) dw;

        System.out.println("前端模拟 Publisher 启动，准备发送认证请求...");

        // ===== 模拟多个玩家认证请求 =====
        sendAuthRequest("Mary", "passwordMary", "LOGIN");
        /*for (int i = 0; i < 5; i++) {  // 模拟5个玩家的认证请求
            String nickname = "Player" + (i + 6);
            String password = "password" + (i + 6);
            String authType = (i % 2 == 0) ? "REGISTER" : "LOGIN";  // 偶数注册，奇数登录

            sendAuthRequest(nickname, password, authType);

            try {
                Thread.sleep(1000);  // 每隔1秒发送一个请求
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        // 程序需要持续运行来发送更多请求
        // 主线程循环等待，以保持进程活跃
        while (true) {
            try {
                Thread.sleep(1000);  // 每秒循环一次，模拟请求
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送认证请求
     * @param nickname 用户昵称
     * @param password 用户密码
     * @param authType "REGISTER" 或 "LOGIN"
     */
    public static void sendAuthRequest(String nickname, String password, String authType) {
        PlayerAuth msg = new PlayerAuth();
        msg.nickname = nickname;  // 用户昵称
        msg.password = password;  // 用户密码
        msg.auth_type = authType; // "REGISTER" 或 "LOGIN"

        // 发送到 DDS
        ReturnCode_t ret = writer.write(msg, null);
        if (ret.equals(ReturnCode_t.RETCODE_OK)) {
            System.out.println("发送认证请求成功: " + authType + " -> " + nickname);
        } else {
            System.out.println("发送认证请求失败: " + ret);
        }
    }

    // 已加载库标识
    private static boolean hasLoad = false;

    // 加载DDS库
    public static void loadLibrary() {
        if (!hasLoad) {
            System.loadLibrary("ZRDDS_JAVA");
            hasLoad = true;
        }
    }
}