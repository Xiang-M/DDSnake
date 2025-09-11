package com.example.snakegame.DDSgenerated;


public class JoinRoom{
    public int player_id = 0;// @ID(0)
    public String room_id = "";// @ID(1)
    public String player_nickname = "";// @ID(2)
    public String status = "";// @ID(3)

    public JoinRoom(){

    }

    public JoinRoom(JoinRoom other){
        this();
        copy(other);
    }

    public Object copy(Object src) {
        JoinRoom typedSrc = (JoinRoom)src;
        this.player_id =  typedSrc.player_id;
        this.room_id =  typedSrc.room_id;
        this.player_nickname =  typedSrc.player_nickname;
        this.status =  typedSrc.status;
        return this;
    }
}