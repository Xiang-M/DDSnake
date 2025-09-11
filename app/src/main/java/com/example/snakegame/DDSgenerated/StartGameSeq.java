package com.example.snakegame.DDSgenerated;

import com.zrdds.infrastructure.ZRSequence;

public class StartGameSeq extends ZRSequence<StartGame> {

    protected Object[] alloc_element(int length) {
        StartGame[] result = new StartGame[length];
        for (int i = 0; i < result.length; ++i) {
             result[i] = new StartGame();
        }
        return result;
    }

    protected Object copy_from_element(Object dstEle, Object srcEle){
        StartGame typedDst = (StartGame)dstEle;
        StartGame typedSrc = (StartGame)srcEle;
        return typedDst.copy(typedSrc);
    }

    public void pull_from_nativeI(long nativeSeq){

    }

    public void push_to_nativeI(long nativeSeq){

    }
}