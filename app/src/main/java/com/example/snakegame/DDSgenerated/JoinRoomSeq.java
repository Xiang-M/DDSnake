package com.example.snakegame.DDSgenerated;

import com.zrdds.infrastructure.ZRSequence;

public class JoinRoomSeq extends ZRSequence<JoinRoom> {

    protected Object[] alloc_element(int length) {
        JoinRoom[] result = new JoinRoom[length];
        for (int i = 0; i < result.length; ++i) {
             result[i] = new JoinRoom();
        }
        return result;
    }

    protected Object copy_from_element(Object dstEle, Object srcEle){
        JoinRoom typedDst = (JoinRoom)dstEle;
        JoinRoom typedSrc = (JoinRoom)srcEle;
        return typedDst.copy(typedSrc);
    }

    public void pull_from_nativeI(long nativeSeq){

    }

    public void push_to_nativeI(long nativeSeq){

    }
}