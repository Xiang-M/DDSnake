package com.example.snakegame.DDSgenerated;


public class PlayerColorMappings{
    public String room_id = "";// @ID(0)
    public PlayerColorMappingSeq maps = new PlayerColorMappingSeq();// @ID(1)

    public PlayerColorMappings(){

        this.maps.maximum(255);
    }

    public PlayerColorMappings(PlayerColorMappings other){
        this();
        copy(other);
    }

    public Object copy(Object src) {
        PlayerColorMappings typedSrc = (PlayerColorMappings)src;
        this.room_id =  typedSrc.room_id;
        this.maps.copy(typedSrc.maps);
        return this;
    }
}