package com.example.snakegame.data.dds.publisher;

import android.util.Log;

import com.example.snakegame.DDSgenerated.JoinRoom;
import com.example.snakegame.DDSgenerated.JoinRoomDataWriter;
import com.example.snakegame.DDSgenerated.JoinRoomTypeSupport;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.domain.DomainParticipantQos;
import com.zrdds.infrastructure.InstanceHandle_t;
import com.zrdds.infrastructure.ReturnCode_t;
import com.zrdds.infrastructure.StatusKind;
import com.zrdds.publication.DataWriter;
import com.zrdds.publication.Publisher;
import com.zrdds.topic.Topic;

public class JoinRoomPublisher {
    private static final String TAG = "JoinRoomPublisher";
    private static int DOMAIN_ID = 6;
    private static DomainParticipant dp;
    private static Publisher pub;
    private static Topic tp;
    private static JoinRoomDataWriter writer;
    private static ReturnCode_t rtn;

    public static void initialize(){
        DomainParticipantQos dpQos = new DomainParticipantQos();
        DomainParticipantFactory.get_instance().get_default_participant_qos(dpQos);
//        dpQos.metatraffic_receive_addresses.addresses.ensure_length(1, 1);
//        dpQos.metatraffic_receive_addresses.addresses.set_at(0, "udpv4://192.168.137.0//0");
//        dpQos.usertraffic_receive_addresses.addresses.ensure_length(1, 1);
//        dpQos.usertraffic_receive_addresses.addresses.set_at(0, "udpv4://192.168.137.0//0");
        // 创建域参与者
        dp = DomainParticipantFactory.get_instance().create_participant(
                DOMAIN_ID,
                dpQos,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (dp == null) {
            throw new RuntimeException("Failed to create domain participant");
        }
        Log.d(TAG, "Domain participant created successfully");

        // 创建发布者
        pub = dp.create_publisher(
                DomainParticipant.PUBLISHER_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (pub == null) {
            throw new RuntimeException("Failed to create publisher");
        }
        Log.d(TAG, "Publisher created successfully");

        // 注册 JoinRoom 类型
        JoinRoomTypeSupport ts = (JoinRoomTypeSupport) JoinRoomTypeSupport.get_instance();
        rtn = ts.register_type(dp, null);
        if (rtn != ReturnCode_t.RETCODE_OK) {
            Log.d(TAG, "register type failed");
            return;
        }
        Log.d(TAG, "register type succeeded");

        // 创建 Topic
        tp = dp.create_topic(
                "JOINROOM",
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
        writer = (JoinRoomDataWriter) dw;
        Log.d(TAG, "create data writer succeeded");
    }

    public static void sendData(JoinRoom data) {
        rtn = writer.write(data, InstanceHandle_t.HANDLE_NIL_NATIVE);
        if (rtn != ReturnCode_t.RETCODE_OK)
        {
            Log.d(TAG, "write failed");
        }
        Log.d(TAG, "write succeeded");
    }
}