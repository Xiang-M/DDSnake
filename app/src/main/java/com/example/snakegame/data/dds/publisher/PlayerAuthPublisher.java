package com.example.snakegame.data.dds.publisher;

import android.util.Log;

import com.example.snakegame.DDSgenerated.PlayerAuth;
import com.example.snakegame.DDSgenerated.PlayerAuthDataWriter;
import com.example.snakegame.DDSgenerated.PlayerAuthTypeSupport;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.infrastructure.InstanceHandle_t;
import com.zrdds.infrastructure.ReturnCode_t;
import com.zrdds.infrastructure.StatusKind;
import com.zrdds.publication.DataWriter;
import com.zrdds.publication.Publisher;
import com.zrdds.topic.Topic;

import com.example.snakegame.data.dds.DDSManager;

public class PlayerAuthPublisher {
    private static final String TAG = "PlayerAuthPublisher";
    private static DomainParticipant dp;
    private static Publisher pub;
    private static Topic tp;
    private static PlayerAuthDataWriter writer;
    private static DDSManager ddsManager;
    private static ReturnCode_t rtn;

    public PlayerAuthPublisher(){
        initialize();
    }

    public static void initialize(){
        ddsManager = DDSManager.getInstance();
        dp = ddsManager.participant;
        pub = ddsManager.publisher;

        // 注册 PlayerAuth 类型
        PlayerAuthTypeSupport ts = (PlayerAuthTypeSupport) PlayerAuthTypeSupport.get_instance();
        rtn = ts.register_type(dp, null);
        if (rtn != ReturnCode_t.RETCODE_OK) {
            Log.d(TAG, "register type failed");
            return;
        }
        Log.d(TAG, "register type succeeded");

        // 创建 Topic
        tp = dp.create_topic(
                "PLAYERAUTH_REQUEST",
                ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (tp == null) {
            Log.d(TAG, "create topic failed");
            return;
        }
        Log.d(TAG, "create topic succeeded");

        // 创建 DataWriter
        DataWriter dw = pub.create_datawriter(
                tp,
                Publisher.DATAWRITER_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (dw == null) {
            Log.d(TAG, "create data writer failed");
            return;
        }
        writer = (PlayerAuthDataWriter) dw;
        Log.d(TAG, "create data writer succeeded");
    }

    public static void sendData(PlayerAuth data) {
        rtn = writer.write(data, InstanceHandle_t.HANDLE_NIL_NATIVE);
        if (rtn != ReturnCode_t.RETCODE_OK)
        {
            Log.d(TAG, "write failed");
        }
        Log.d(TAG, "write succeeded");
    }
}