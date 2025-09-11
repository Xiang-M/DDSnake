package com.example.snakegame.DDSgenerated;


public class LeaveRoom{
    public int player_id = 0;// @ID(0)
    public String room_id = "";// @ID(1)

    public LeaveRoom(){

    }

    public LeaveRoom(LeaveRoom other){
        this();
        copy(other);
    }

    public Object copy(Object src) {
        LeaveRoom typedSrc = (LeaveRoom)src;
        this.player_id =  typedSrc.player_id;
        this.room_id =  typedSrc.room_id;
        return this;
    }
}