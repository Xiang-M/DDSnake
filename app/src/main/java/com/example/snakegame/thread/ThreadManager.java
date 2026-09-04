package com.example.snakegame.thread;

import com.example.snakegame.DDSgenerated.InRoom;
import com.example.snakegame.DDSgenerated.JoinRoom;
import com.example.snakegame.DDSgenerated.PlayerColorMappings;
import com.example.snakegame.uitls.DataCallback;

// 一个全局的管理器（单例）
public class ThreadManager {
    private static InRoomSubscriberThread inRoomSubscriberThread;
    private static JoinRoomSubscriberThread joinRoomSubscriberThread;
    private static PlayerColorMappingsSubscriberThread playerColorMappingsSubscriberThread;

    public static void initialize(){
        inRoomSubscriberThread = new InRoomSubscriberThread();
        joinRoomSubscriberThread = new JoinRoomSubscriberThread();
        playerColorMappingsSubscriberThread = new PlayerColorMappingsSubscriberThread();

        inRoomSubscriberThread.run();
        joinRoomSubscriberThread.run();
        playerColorMappingsSubscriberThread.run();
    }

    public static void setCallbackToInRoomSub(DataCallback<InRoom> callback){
        inRoomSubscriberThread.setCallback(callback);
    }

    public static void setCallbackToJoinRoomSub(DataCallback<JoinRoom> callback){
        joinRoomSubscriberThread.setCallback(callback);
    }

    public static void setCallbackToPlayerColorMappingsSub(DataCallback<PlayerColorMappings> callback){
        playerColorMappingsSubscriberThread.setCallback(callback);
    }
}