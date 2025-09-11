package com.example.snakegame.DDSgenerated;

import com.zrdds.infrastructure.ZRSequence;

public class PlayerColorMappingsSeq extends ZRSequence<PlayerColorMappings> {

    protected Object[] alloc_element(int length) {
        PlayerColorMappings[] result = new PlayerColorMappings[length];
        for (int i = 0; i < result.length; ++i) {
             result[i] = new PlayerColorMappings();
        }
        return result;
    }

    protected Object copy_from_element(Object dstEle, Object srcEle){
        PlayerColorMappings typedDst = (PlayerColorMappings)dstEle;
        PlayerColorMappings typedSrc = (PlayerColorMappings)srcEle;
        return typedDst.copy(typedSrc);
    }

    public void pull_from_nativeI(long nativeSeq){

    }

    public void push_to_nativeI(long nativeSeq){

    }
}