package com.example.snakegame.data.dds.subscriber;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.snakegame.DDSgenerated.ChatMsgTypeSupport;
import com.example.snakegame.DDSgenerated.PlayerColorMappings;
import com.example.snakegame.DDSgenerated.PlayerColorMappingsDataReader;
import com.example.snakegame.DDSgenerated.PlayerColorMappingsDataWriter;
import com.example.snakegame.DDSgenerated.PlayerColorMappingsSeq;

import com.example.snakegame.DDSgenerated.PlayerColorMappingsTypeSupport;
import com.zrdds.domain.DomainParticipant;
import com.zrdds.domain.DomainParticipantFactory;
import com.zrdds.domain.DomainParticipantQos;
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
import com.zrdds.publication.Publisher;
import com.zrdds.subscription.DataReader;
import com.zrdds.subscription.DataReaderListener;
import com.zrdds.subscription.DataReaderQos;
import com.zrdds.subscription.Subscriber;
import com.zrdds.topic.Topic;

import com.example.snakegame.uitls.DataCallback;


public class PlayerColorMappingsSubscriber {
    private Context context;
    private final String TAG = "PlayerColorMappingsSubscriber";
    private int domain_id = 6;
    private DomainParticipant dp;
    private Subscriber sub;
    private Topic tp;
    private DataReader reader;
    private PlayerColorMappingsDataReaderListener listener;
    private ReturnCode_t rtn;
    private DataCallback callback;

    public void setCallback(DataCallback callback) {
        this.callback = callback;
    }

    public PlayerColorMappingsSubscriber(Context context) {
        this.context = context;
        initDDS();
    }

    private void initDDS(){
        try {
            // 创建 DomainParticipant
            dp = DomainParticipantFactory.get_instance().create_participant(
                    domain_id,
                    DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            if (dp == null) {
                Log.e( TAG, "创建 DomainParticipant 失败");
                return;
            }

            // 注册类型
            ChatMsgTypeSupport ts = (ChatMsgTypeSupport) ChatMsgTypeSupport.get_instance();
            ReturnCode_t ret = ts.register_type(dp, null);
            if (ret != ReturnCode_t.RETCODE_OK) {
                Log.e(TAG, "注册类型失败");
                return;
            }

            // 创建 Topic
            tp = dp.create_topic(
                    "COLORMAP",
                    ts.get_type_name(),
                    DomainParticipant.TOPIC_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            // 创建 Subscriber
            sub = dp.create_subscriber(
                    DomainParticipant.SUBSCRIBER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );

            // 创建 DataReader -> 订阅消息
            listener = new PlayerColorMappingsDataReaderListener();
            reader = sub.create_datareader(
                    tp,
                    Subscriber.DATAREADER_QOS_DEFAULT,
                    listener, // 回调监听
                    StatusKind.DATA_AVAILABLE_STATUS
            );

            Log.d(TAG, "DDS 初始化成功");

        } catch (Exception e) {
            Log.e(TAG, "DDS 初始化失败", e);
        }
    }

    private class PlayerColorMappingsDataReaderListener implements DataReaderListener {
        public void on_data_available(DataReader dataReader) {
            PlayerColorMappingsDataReader dr = (PlayerColorMappingsDataReader) (dataReader);
            PlayerColorMappingsSeq dataSeq = new PlayerColorMappingsSeq();
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
                PlayerColorMappings receivedData = dataSeq.get_at(i);
                // 只有当callback传入之后才看传输的数据
                if(callback != null)
                {
                    callback.onDataReceived(receivedData);
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
}
