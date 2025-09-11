package com.example.snakegame.DDSgenerated;

import com.zrdds.infrastructure.ZRSequence;

public class LeaveRoomSeq extends ZRSequence<LeaveRoom> {

    protected Object[] alloc_element(int length) {
        LeaveRoom[] result = new LeaveRoom[length];
        for (int i = 0; i < result.length; ++i) {
             result[i] = new LeaveRoom();
        }
        return result;
    }

    protected Object copy_from_element(Object dstEle, Object srcEle){
        LeaveRoom typedDst = (LeaveRoom)dstEle;
        LeaveRoom typedSrc = (LeaveRoom)srcEle;
        return typedDst.copy(typedSrc);
    }

    public void pull_from_nativeI(long nativeSeq){

    }

    public void push_to_nativeI(long nativeSeq){

    }
}