package com.example.snakegame.data.dds;

import android.content.Context;
import android.util.Log;

import com.zrdds.domain.*;
import com.zrdds.subscription.*;
import com.zrdds.publication.*;
import com.zrdds.topic.*;
import com.zrdds.infrastructure.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DDSManager {
    private static final String TAG = "DDSManager";
    // 使用ConcurrentHashMap来存储不同域号的实例，确保线程安全
    private static final Map<Integer, DDSManager> instances = new ConcurrentHashMap<>();

    // 核心DDS组件（所有activity共享）
    public DomainParticipant participant;
    public Publisher publisher;
    public Subscriber subscriber;
    private boolean isCoreInitialized = false;

    // 当前实例的域号
    private final int domainId;

    private Context context;

    // 私有构造函数，接收域号参数
    private DDSManager(int domainId) {
        this.domainId = domainId;
    }

    // 公共静态方法，根据域号获取实例
    public static synchronized DDSManager getInstance(int domainId) {
        // 如果该域号的实例不存在，则创建新的实例
        if (!instances.containsKey(domainId)) {
            instances.put(domainId, new DDSManager(domainId));
            Log.d(TAG, "Created new DDSManager instance for domain: " + domainId);
        }
        return instances.get(domainId);
    }

    // 重载方法，使用默认域号（保持向后兼容性）
    public static synchronized DDSManager getInstance() {
        return getInstance(69); // 默认域号
    }

    // 获取当前实例的域号
    public int getDomainId() {
        return domainId;
    }

    // 初始化DDS
    public void initializeCore(Context context) {
        if (isCoreInitialized) {
            Log.d(TAG, "DDS core already initialized for domain: " + domainId);
            return;
        }

        this.context = context.getApplicationContext();

        try {
            // 仅适用于XIANGMAN电脑热点的本地网络，否则需修改ip段
            DomainParticipantQos dpQos = new DomainParticipantQos();
            DomainParticipantFactory.get_instance().get_default_participant_qos(dpQos);
//            dpQos.metatraffic_receive_addresses.addresses.ensure_length(1, 1);
//            dpQos.metatraffic_receive_addresses.addresses.set_at(0, "udpv4://192.168.137.1//0");
//            dpQos.usertraffic_receive_addresses.addresses.ensure_length(1, 1);
//            dpQos.usertraffic_receive_addresses.addresses.set_at(0, "udpv4://192.168.137.1//0");
            // 1. 创建域参与者，使用当前实例的域号
            participant = DomainParticipantFactory.get_instance().create_participant(
                    domainId, // 使用实例的域号
                    dpQos,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (participant == null) {
                throw new RuntimeException("Failed to create domain participant for domain: " + domainId);
            }
            Log.d(TAG, "Domain participant created successfully for domain: " + domainId);

            // 2. 创建发布者
            publisher = participant.create_publisher(
                    DomainParticipant.PUBLISHER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (publisher == null) {
                throw new RuntimeException("Failed to create publisher for domain: " + domainId);
            }
            Log.d(TAG, "Publisher created successfully for domain: " + domainId);

            // 3. 创建订阅者
            subscriber = participant.create_subscriber(
                    DomainParticipant.SUBSCRIBER_QOS_DEFAULT,
                    null,
                    StatusKind.STATUS_MASK_NONE
            );
            if (subscriber == null) {
                throw new RuntimeException("Failed to create subscriber for domain: " + domainId);
            }
            Log.d(TAG, "Subscriber created successfully for domain: " + domainId);

            isCoreInitialized = true;

        } catch (Exception e) {
            Log.e(TAG, "Core DDS initialization failed for domain " + domainId + ": " + e.getMessage(), e);
            shutdown();
            throw new RuntimeException("DDS core initialization failed for domain: " + domainId, e);
        }
    }

    // 检查核心是否初始化
    public boolean isCoreInitialized() {
        return isCoreInitialized;
    }


    // 完整关闭当前实例
    public void shutdown() {
        if (participant != null) {
            participant.delete_contained_entities();
            DomainParticipantFactory.get_instance().delete_participant(participant);
            participant = null;
        }

        // 从实例映射中移除当前实例
        instances.remove(domainId);
        isCoreInitialized = false;
        Log.d(TAG, "DDS completely shutdown for domain: " + domainId);
    }

    // 静态方法：关闭所有域号的实例
    public static void shutdownAll() {
        synchronized (instances) {
            for (DDSManager instance : instances.values()) {
                instance.shutdown();
            }
            instances.clear();
            Log.d(TAG, "All DDS instances shutdown");
        }
    }

    // 静态方法：获取当前存在的实例数量
    public static int getInstanceCount() {
        return instances.size();
    }

    // 静态方法：检查特定域号的实例是否存在
    public static boolean hasInstance(int domainId) {
        return instances.containsKey(domainId);
    }
}