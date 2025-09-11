package com.example.snakegame.uitls;

public interface DataCallbackJoinRoom {
    void joinRoomSuccess();
    void joinRoomFailure();
    void onError(Exception e);
}
