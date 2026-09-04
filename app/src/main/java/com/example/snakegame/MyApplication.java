package com.example.snakegame;

import android.app.Application;
import android.util.Log;

import com.example.snakegame.data.dds.DDSManager;
import com.zrdds.domain.*;
import com.zrdds.subscription.*;
import com.zrdds.publication.*;
import com.zrdds.topic.*;
import com.zrdds.infrastructure.*;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static boolean isLibraryLoaded = false;

    @Override
    public void onCreate() {
        super.onCreate();// 1. 首先调用父类初始化
        loadDDSLibrary(); // 2. 加载DDS库
        factoryInit(); // 3. 工厂模式初始化
        DDSManager.getInstance().initializeCore(this);// 4. 将登录/注册的通信域内的基础实体创建
    }

    private void loadDDSLibrary() {
        if (!isLibraryLoaded) {
            try {
                System.loadLibrary("ZRDDS_JAVA");
                isLibraryLoaded = true;
                Log.i(TAG, "DDS library loaded successfully");
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Failed to load DDS library: " + e.getMessage());
            }
        }
    }

    private void factoryInit(){
        Log.i(TAG, "开始初始化ZRDDS...");
        DomainParticipantFactoryQos dpfQos = new DomainParticipantFactoryQos();
        dpfQos.dds_log.file_mask = 0;
        dpfQos.dds_log.console_mask = 0xffff;
        dpfQos.dds_log.file_dir = String.valueOf(getFilesDir());
        Property_t property = new Property_t();
        property.name = "sysctl.global.licence";
        property.value = "data:UserName: \nAuth Date: 2020/09/15:19:17:26\nExpire Date: 2025/10/21:19:17:26\nMACS:\nunlimited\nHDS:\nunlimited\nSignature:\nb4b93ac94879a73959465ad0692722934efb100a1069d1e91d4fc14596483cf651496531f7376f389b2a6cea9dc4b276f8cdd3ce171f2c333a5f6061e0033a94889282b1d142ca3709b69e6e88cd24252818bd543c1f66a1ae905bdb8b854e03055a1535fa262570fbefcdb7c05b63f872809cd57f82dfcc72cc495eee824ff0\nLastVerifyDate:2024/11/21:11:01:5022325d7c7825924f6f8c0ab42a65414c";
        dpfQos.property.value.ensure_length(1, 1);
        dpfQos.property.value.set_at(0, property);

        DomainParticipantFactory factory = DomainParticipantFactory.get_instance_w_qos(dpfQos);
        if (factory == null) {
            Log.e(TAG, "无法获取DomainParticipantFactory实例");
            return;
        }
        Log.i(TAG, "✓ DomainParticipantFactory创建成功");
    }

}
