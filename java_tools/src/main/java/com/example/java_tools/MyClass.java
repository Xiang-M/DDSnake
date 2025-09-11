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

public class MyClass {
    private static DomainParticipant dp;
    private static Publisher pub;
    private static Topic tp;
    private static PlayerAuthDataWriter writer;
    public static void main(String[] args){
        // 设置库路径
        String libPath = System.getProperty("user.dir") + "/src/main/resources/lib/";
        System.setProperty("java.library.path", libPath);
        loadLibrary();
    }

    // 已加载库标识
    private static boolean hasLoad = false;
    public static void loadLibrary() {
        if (!hasLoad) {
            System.loadLibrary("ZRDDS_JAVA");
            hasLoad = true;
        }
    }
}