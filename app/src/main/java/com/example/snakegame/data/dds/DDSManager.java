package com.example.snakegame.data.dds;

import android.content.Context;
import android.util.Log;

import com.example.snakegame.DDSgenerated.*;

import com.zrdds.domain.*;
import com.zrdds.subscription.*;
import com.zrdds.publication.*;
import com.zrdds.topic.*;
import com.zrdds.infrastructure.*;

import java.util.HashMap;
import java.util.Map;

public class DDSManager {
    private static final String TAG = "DDSManager";// 方便识别日志来源
    private static final int DOMAIN_ID = 69;//确定域号
    private static DDSManager instance;//一个管理者实例

    // 在初始化过程中加载库，并将factory初始化
    // 并确定域号，创建域参与者、发布者、订阅者等实体
    // 只进行一次初始化，从而保证以上活动只进行一次

    // 核心DDS组件（所有activity共享）
    public DomainParticipant participant;
    public Publisher publisher;
    public Subscriber subscriber;
    private boolean isCoreInitialized = false;

    // 按需创建的组件
    // typeRegistered的键：类型名称
    // topics，dataWriters，dataReaders的键：主题名称
    private Map<String, Boolean> typeRegistered = new HashMap<>();
    private Map<String, Topic> topics = new HashMap<>();
    private Map<String, DataWriter> dataWriters = new HashMap<>();
    private Map<String, DataReader> dataReaders = new HashMap<>();
    private Map<String, DataReaderListener> dataReaderListeners = new HashMap<>();

    private Context context;

    //默认构造函数，实际无用，专门设置成private，类外无法new一个新的对象
    private DDSManager() {}

    // 公共静态方法，外部调用该方法获取实例
    // 整个应用程序（包含多个activity）共用一个DDSManager实例
    public static synchronized DDSManager getInstance() {
        if (instance == null) {
            instance = new DDSManager();
        }
        return instance;
    }

    // 初始化DDS
    public void initializeCore(Context context) {
        if (isCoreInitialized) {
            return;
        }

        this.context = context.getApplicationContext();
        // 让当前类持有应用级上下文的引用

        try {
            // 1. 创建域参与者
            participant = DomainParticipantFactory.get_instance().create_participant(
                    DOMAIN_ID,
                    DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (participant == null) {
                throw new RuntimeException("Failed to create domain participant");
            }
            Log.d(TAG, "Domain participant created successfully");

            // 2. 创建发布者
            publisher = participant.create_publisher(
                    DomainParticipant.PUBLISHER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (publisher == null) {
                throw new RuntimeException("Failed to create publisher");
            }
            Log.d(TAG, "Publisher created successfully");

            // 3. 创建订阅者
            subscriber = participant.create_subscriber(
                    DomainParticipant.SUBSCRIBER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (subscriber == null) {
                throw new RuntimeException("Failed to create subscriber");
            }
            Log.d(TAG, "Subscriber created successfully");

        } catch (Exception e) {
            Log.e(TAG, "Core DDS initialization failed: " + e.getMessage(), e);
            shutdown();
            throw new RuntimeException("DDS core initialization failed", e);
        }
    }

    // 按需注册数据类型
    // 外部方法调用时只需提供 typeName typeSupport
    public synchronized boolean registerType(String typeName, Object typeSupport) {
        if (!isCoreInitialized) {
            Log.w(TAG, "Core DDS not initialized, cannot register type: " + typeName);
            return false;
        }

        if (typeRegistered.containsKey(typeName) && typeRegistered.get(typeName)) {
            return true; // 已经注册过
        }

        try {
            ReturnCode_t rtn;
            if (typeSupport instanceof PlayerAuthTypeSupport) {
                rtn = ((PlayerAuthTypeSupport) typeSupport).register_type(participant, null);
            } else if (typeSupport instanceof GameSettingTypeSupport) {
                rtn = ((GameSettingTypeSupport) typeSupport).register_type(participant, null);
            } else if (typeSupport instanceof ChatMsgTypeSupport) {
                rtn = ((ChatMsgTypeSupport) typeSupport).register_type(participant, null);
            } else if (typeSupport instanceof CollisionTypeSupport) {
                rtn = ((CollisionTypeSupport) typeSupport).register_type(participant, null);
            } else {
                Log.e(TAG, "Unknown type support: " + typeName);
                return false;
            }

            if (rtn == ReturnCode_t.RETCODE_OK) {
                typeRegistered.put(typeName, true);
                Log.d(TAG, "Type registered successfully: " + typeName);
                return true;
            } else {
                Log.e(TAG, "Failed to register type: " + typeName + ", return code: " + rtn);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error registering type: " + typeName, e);
            return false;
        }
    }

    // 按需创建主题
    // 外部方法调用时只需提供 topicName typeName typeSupport
    public synchronized Topic getOrCreateTopic(String topicName, String typeName, Object typeSupport) {
        if (!isCoreInitialized) {
            Log.w(TAG, "Core DDS not initialized, cannot create topic: " + topicName);
            return null;
        }

        // 可改进，确保已存在主题其关联数据类型与本次要求统一
        if (topics.containsKey(topicName)) {
            return topics.get(topicName); // 返回已创建的主题
        }

        // 先注册数据类型
        registerType(typeName, typeSupport);

        try {
            Topic topic = participant.create_topic(
                    topicName, typeName,
                    DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE
            );

            if (topic == null) {
                Log.e(TAG, "Failed to create topic: " + topicName);
                return null;
            }

            topics.put(topicName, topic);//放入Map
            Log.d(TAG, "Topic created successfully: " + topicName);
            return topic;

        } catch (Exception e) {
            Log.e(TAG, "Error creating topic: " + topicName, e);
            return null;
        }
    }

    // 按需创建数据写者
    // 外部方法调用时只需提供 topicName typeName typeSupport
    public synchronized DataWriter createDataWriter(String topicName, String typeName, Object typeSupport) {
        if (!isCoreInitialized) {
            Log.w(TAG, "Core DDS not initialized");
            return null;
        }

        if (dataWriters.containsKey(topicName)) {
            return dataWriters.get(topicName);
        }

        Topic topic = getOrCreateTopic(topicName, typeName, typeSupport);
        if (topic == null) {
            return null;
        }

        try {
            DataWriterQos dwQos = new DataWriterQos();
            ReturnCode_t rtn = publisher.get_default_datawriter_qos(dwQos);

            if (rtn != ReturnCode_t.RETCODE_OK) {
                Log.e(TAG, "Failed to get default DataWriter QoS for: " + topicName);
                return null;
            }

            DataWriter dw = publisher.create_datawriter(topic, dwQos, null, StatusKind.STATUS_MASK_NONE);

            if (dw == null) {
                Log.e(TAG, "Failed to create DataWriter for: " + topicName);
                return null;
            }

            dataWriters.put(topicName, dw);
            Log.d(TAG, "DataWriter created successfully for: " + topicName);
            return dw;

        } catch (Exception e) {
            Log.e(TAG, "Error creating DataWriter for: " + topicName, e);
            return null;
        }
    }

    // 按需创建数据读者
    // 外部方法调用时只需提供 topicName typeName typeSupport listener
    // DataReaderListener只是接口，要实现自定义，再创建listener
    public synchronized DataReader createDataReader(String topicName, String typeName, Object typeSupport, DataReaderListener listener) {
        if (!isCoreInitialized) {
            Log.w(TAG, "Core DDS not initialized, cannot create DataReader: " + topicName);
            return null;
        }

        if (dataReaders.containsKey(topicName)) {
            Log.d(TAG, "DataReader already exists for: " + topicName);
            return dataReaders.get(topicName);
        }

        // 获取或创建主题
        Topic topic = getOrCreateTopic(topicName, typeName, typeSupport);
        if (topic == null) {
            Log.e(TAG, "Failed to get topic for DataReader: " + topicName);
            return null;
        }

        try {
            // 设置DataReader QoS
            DataReaderQos drQos = new DataReaderQos();
            ReturnCode_t rtn = subscriber.get_default_datareader_qos(drQos);

            if (rtn != ReturnCode_t.RETCODE_OK) {
                Log.e(TAG, "Failed to get default DataReader QoS for: " + topicName);
                return null;
            }

            // 创建DataReader
            DataReader dr = subscriber.create_datareader(topic, drQos, listener, StatusKind.STATUS_MASK_ALL);

            if (dr == null) {
                Log.e(TAG, "Failed to create DataReader for: " + topicName);
                return null;
            }

            dataReaders.put(topicName, dr);
            dataReaderListeners.put(topicName, listener);

            Log.d(TAG, "DataReader created successfully for: " + topicName);
            return dr;

        } catch (Exception e) {
            Log.e(TAG, "Error creating DataReader for: " + topicName, e);
            return null;
        }
    }

    // TO DO 确定服务质量（当前均为默认）
    // 当主题确定时，writerQoS、readerQoS、topicQoS可以分别确定，所以可以直接get

    // 检查核心是否初始化
    public boolean isCoreInitialized() {
        return isCoreInitialized;
    }

    // 清理特定主题的资源
    // 其实不清理也行，应该不会影响性能
    public synchronized void cleanupTopic(String topicName) {
        dataWriters.remove(topicName);
        dataReaders.remove(topicName);
        dataReaderListeners.remove(topicName);
        topics.remove(topicName);
        Log.d(TAG, "Cleaned up resources for topic: " + topicName);
    }

    // 完整关闭
    public void shutdown() {
        // 清理所有资源
        dataWriters.clear();
        dataReaders.clear();
        dataReaderListeners.clear();
        topics.clear();
        typeRegistered.clear();

        if (participant != null) {
            participant.delete_contained_entities();
            DomainParticipantFactory.get_instance().delete_participant(participant);
        }

        isCoreInitialized = false;
        Log.d(TAG, "DDS completely shutdown");
    }
}
