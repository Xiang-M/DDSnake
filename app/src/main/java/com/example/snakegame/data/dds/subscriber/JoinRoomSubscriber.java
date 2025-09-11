package com.example.snakegame.data.dds.subscriber;

import android.util.Log;

import com.example.snakegame.DDSgenerated.JoinRoom;
import com.example.snakegame.DDSgenerated.JoinRoomDataReader;
import com.example.snakegame.DDSgenerated.JoinRoomSeq;
import com.example.snakegame.DDSgenerated.JoinRoomTypeSupport;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.infrastructure.HistoryQosPolicyKind;
import com.zrdds.infrastructure.InstanceStateKind;
import com.zrdds.infrastructure.LivelinessChangedStatus;
import com.zrdds.infrastructure.ReliabilityQosPolicyKind;
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
import com.zrdds.subscription.DataReaderQos;
import com.zrdds.subscription.Subscriber;
import com.zrdds.topic.Topic;

import com.example.snakegame.uitls.DataCallbackJoinRoom;

// 订阅从后端返回的注册/登录确认消息
public class JoinRoomSubscriber {
    private static final String TAG = "JoinRoomSubscriber";
    private static int DOMAIN_ID = 6;
    private static DomainParticipant dp;
    private static Subscriber sub;
    private static Topic tp;
    private static JoinRoomDataReaderListener listener;
    private static JoinRoomDataReader reader;
    public static JoinRoom rightData;
    public static int success = 0;
    private static ReturnCode_t rtn;

    public static void initialize(){
        // 创建域参与者
        dp = DomainParticipantFactory.get_instance().create_participant(
                DOMAIN_ID,
                DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (dp == null) {
            throw new RuntimeException("Failed to create domain participant");
        }
        Log.d(TAG, "Domain participant created successfully");

        // 创建订阅者
        sub = dp.create_subscriber(
                DomainParticipant.SUBSCRIBER_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (sub == null) {
            throw new RuntimeException("Failed to create subscriber");
        }
        Log.d(TAG, "Subscriber created successfully");

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
                "JOINROOMREPLY",
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

        // 创建监听器
        listener = new JoinRoomDataReaderListener();

        // 设置数据读者的qos
        DataReaderQos drQos = new DataReaderQos();
        rtn = sub.get_default_datareader_qos(drQos);
        if (rtn != ReturnCode_t.RETCODE_OK) {
            Log.d(TAG, "get default datareader qos failed");
        }
        drQos.reliability.kind= ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
        drQos.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;

        // 创建数据读者
        DataReader dr = sub.create_datareader(tp, drQos, listener, StatusKind.STATUS_MASK_ALL);
        if (dr == null) {
            Log.d(TAG, "create dr failed");
            return;
        }
        reader = (JoinRoomDataReader)dr;
        Log.d(TAG, "create dr succeeded");
    }

    public static void receiveData(JoinRoom request, DataCallbackJoinRoom callback) {
        rightData = request;
        // 阻塞主线程，通过Listener监听数据到达
        while (true) {
            tSleep(2000);
            Log.d(TAG, "wait for receive data");
            if(success == 1){
                callback.joinRoomSuccess();
                break;
            } else if (success == -1) {
                callback.joinRoomFailure();
                break;
            }
        }
    }

    public static void tSleep(int ti) {
        try {
            Thread.sleep(ti);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class JoinRoomDataReaderListener implements DataReaderListener {
    public void on_data_available(DataReader dataReader) {
        JoinRoomDataReader dr = (JoinRoomDataReader) (dataReader);
        JoinRoomSeq dataSeq = new JoinRoomSeq();
        SampleInfoSeq infoSeq = new SampleInfoSeq();
        ReturnCode_t rtn;
        System.out.println("receive receive receive receive receive receive receive receive ");
        // 取出接收数据
        rtn = dr.take(dataSeq, infoSeq, -1, SampleStateKind.ANY_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ANY_INSTANCE_STATE);
        if (rtn != ReturnCode_t.RETCODE_OK) {
            System.out.println("take data failed");
            return;
        }

        // 遍历读取接收数据
        for (int i = 0; i < infoSeq.length(); ++i) {
            // 在使用数据之前，应检查数据的有效性
            if (!infoSeq.get_at(i).valid_data) {
                continue;
            }
            // 获取接收到的数据
            JoinRoom receivedData = dataSeq.get_at(i);
            // Player_id,room_id一致
            // status为SUCCESS（触发InRoom监听）或FAILURE（弹出提示消息：“当前房间不可进，请确认房间号输入正确”）
            if (receivedData.room_id != null && receivedData.room_id.equals(JoinRoomSubscriber.rightData.room_id)&&
                    receivedData.player_id == JoinRoomSubscriber.rightData.player_id) {
                if(receivedData.status == "SUCCESS"){
                    JoinRoomSubscriber.success = 1;
                }else if(receivedData.status == "FAIURE"){
                    JoinRoomSubscriber.success = -1;
                }
            }
        }

        // 返还数据空间
        rtn = dr.return_loan(dataSeq, infoSeq);
        if (rtn != ReturnCode_t.RETCODE_OK) {
            System.out.println("return loan failed");
        }
    }

    @Override
    public void on_liveliness_changed(DataReader arg0, LivelinessChangedStatus arg1) {
        // TODO 自动生成的方法存根
    }

    @Override
    public void on_requested_deadline_missed(DataReader arg0, RequestedDeadlineMissedStatus arg1) {
        // TODO 自动生成的方法存根
    }

    @Override
    public void on_requested_incompatible_qos(DataReader arg0, RequestedIncompatibleQosStatus arg1) {
        // TODO 自动生成的方法存根
    }

    @Override
    public void on_sample_lost(DataReader arg0, SampleLostStatus arg1) {
        // TODO 自动生成的方法存根
    }

    @Override
    public void on_sample_rejected(DataReader arg0, SampleRejectedStatus arg1) {
        // TODO 自动生成的方法存根
    }

    @Override
    public void on_subscription_matched(DataReader arg0, SubscriptionMatchedStatus arg1) {
        // TODO 自动生成的方法存根
    }

    public void on_data_arrived(DataReader arg0, Object arg1, SampleInfo arg2) {
        // TODO 自动生成的方法存根
    }

}
