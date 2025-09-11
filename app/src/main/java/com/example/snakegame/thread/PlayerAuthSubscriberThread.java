package com.example.snakegame.thread;

import com.example.snakegame.DDSgenerated.PlayerAuth;
import com.example.snakegame.DDSgenerated.PlayerAuthDataReader;
import com.example.snakegame.DDSgenerated.PlayerAuthSeq;

import android.util.Log;

import com.example.snakegame.DDSgenerated.PlayerAuth;
import com.example.snakegame.DDSgenerated.PlayerAuthDataReader;
import com.example.snakegame.DDSgenerated.PlayerAuthSeq;
import com.example.snakegame.DDSgenerated.PlayerAuthTypeSupport;
import com.example.snakegame.data.dds.DDSManager;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
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

import com.example.snakegame.uitls.DataCallbackLogin;

public class PlayerAuthSubscriberThread extends Thread {
    private volatile boolean isRunning = true;
    private DataCallbackLogin callback;

    public PlayerAuthSubscriberThread(DataCallbackLogin callback){
        this.callback = callback;
    }

    @Override
    public void run() {
        // 创建域参与者
        DomainParticipant dp = DomainParticipantFactory.get_instance().create_participant(
                69,
                DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (dp == null) {
            throw new RuntimeException("Failed to create domain participant");
        }

        // 创建订阅者
        Subscriber sub = dp.create_subscriber(
                DomainParticipant.SUBSCRIBER_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (sub == null) {
            throw new RuntimeException("Failed to create subscriber");
        }

        // 注册 PlayerAuth 类型
        PlayerAuthTypeSupport ts = (PlayerAuthTypeSupport) PlayerAuthTypeSupport.get_instance();
        ReturnCode_t rtn = ts.register_type(dp, null);
        if (rtn != ReturnCode_t.RETCODE_OK) {
            throw new RuntimeException("Failed to register type");
        }

        // 创建 Topic
        Topic tp = dp.create_topic(
                "PLAYERAUTH_RESULT",
                ts.get_type_name(),
                DomainParticipant.TOPIC_QOS_DEFAULT,
                null,
                StatusKind.STATUS_MASK_NONE
        );
        if (tp == null) {
            throw new RuntimeException("Failed to create topic");
        }

        // 创建监听器
        LoginDataReaderListener listener = new LoginDataReaderListener();

        // 设置读者质量
        DataReaderQos drQos = Subscriber.DATAREADER_QOS_DEFAULT;
        drQos.reliability.kind= ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;

        // 创建数据读者
        DataReader dr = sub.create_datareader(tp, drQos, listener, StatusKind.STATUS_MASK_ALL);
        if (dr == null) {
            throw new RuntimeException("Failed to create dataReader");
        }

        // 一直循环
        while (isRunning) {}
    }

    class LoginDataReaderListener implements DataReaderListener {
        public void on_data_available(DataReader dataReader) {
            PlayerAuthDataReader dr = (PlayerAuthDataReader) (dataReader);
            PlayerAuthSeq dataSeq = new PlayerAuthSeq();
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
                PlayerAuth receivedData = dataSeq.get_at(i);
                callback.onDataReceived(receivedData);
            }

            // 返还数据空间
            rtn = dr.return_loan(dataSeq, infoSeq);
            if (rtn != ReturnCode_t.RETCODE_OK) {
                System.out.println("return loan failed");
                return;
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

    public void stopListening() {
        isRunning = false;
    }

    private String receiveMessage() {
        // 实现你的消息接收逻辑
        return "新消息";
    }
}
