package com.example.snakegame.DDSgenerated;


public class PlayerColorMapping{
    public int player_id = 0;// @ID(0)
    public String color = "";// @ID(1)

    public PlayerColorMapping(){

    }

    public PlayerColorMapping(PlayerColorMapping other){
        this();
        copy(other);
    }

    public Object copy(Object src) {
        PlayerColorMapping typedSrc = (PlayerColorMapping)src;
        this.player_id =  typedSrc.player_id;
        this.color =  typedSrc.color;
        return this;
    }
}