package com.example.snakegame.DDSgenerated;


public class StartGame{
    public String room_id = "";// @ID(0)

    public StartGame(){

    }

    public StartGame(StartGame other){
        this();
        copy(other);
    }

    public Object copy(Object src) {
        StartGame typedSrc = (StartGame)src;
        this.room_id =  typedSrc.room_id;
        return this;
    }
}