package com.example.snakegame.uitls;

import com.example.snakegame.DDSgenerated.PlayerAuth;

public interface DataCallback<T> {
    void onDataReceived(T result);
}
