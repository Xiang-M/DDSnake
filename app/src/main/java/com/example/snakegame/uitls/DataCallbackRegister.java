package com.example.snakegame.uitls;

import com.example.snakegame.DDSgenerated.PlayerAuth;

public interface DataCallbackRegister {
    void onDataReceived(PlayerAuth result);
    void onError(Exception e);
}
