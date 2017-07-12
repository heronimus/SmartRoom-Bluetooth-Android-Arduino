package com.kucik.smartroom.model;

/**
 * Created by Heronimus on 5/31/2017.
 */

public class SmartRoom {
    private String temp;
    private String humid;
    private String light;
    private String motion;
    private String fire;

    public SmartRoom(String tempIn,String humidIn, String lightIn,String motionIn,String fireIn){
        temp=tempIn;
        humid=humidIn;
        light=lightIn;
        motion=motionIn;
        fire=fireIn;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getHumid() {
        return humid;
    }

    public void setHumid(String humid) {
        this.humid = humid;
    }

    public String getLight() {
        return light;
    }

    public void setLight(String light) {
        this.light = light;
    }

    public String getMotion() {
        return motion;
    }

    public void setMotion(String motion) {
        this.motion = motion;
    }

    public String getFire() {
        return fire;
    }

    public void setFire(String fire) {
        this.fire = fire;
    }
}
