package com.example.snakegame.DDSgenerated;

import com.zrdds.infrastructure.ZRSequence;

public class PlayerColorMappingSeq extends ZRSequence<PlayerColorMapping> {

    protected Object[] alloc_element(int length) {
        PlayerColorMapping[] result = new PlayerColorMapping[length];
        for (int i = 0; i < result.length; ++i) {
             result[i] = new PlayerColorMapping();
        }
        return result;
    }

    protected Object copy_from_element(Object dstEle, Object srcEle){
        PlayerColorMapping typedDst = (PlayerColorMapping)dstEle;
        PlayerColorMapping typedSrc = (PlayerColorMapping)srcEle;
        return typedDst.copy(typedSrc);
    }

    public void pull_from_nativeI(long nativeSeq){

    }

    public void push_to_nativeI(long nativeSeq){

    }
}