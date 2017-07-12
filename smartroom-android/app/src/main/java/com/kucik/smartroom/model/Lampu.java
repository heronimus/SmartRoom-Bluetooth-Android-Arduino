package com.kucik.smartroom.model;

/**
 * Created by Heronimus on 5/31/2017.
 */

public class Lampu {
    private String lamp1;
    private String lamp2;
    private String lampAC;

    public Lampu(String lamp1In,String lamp2In,String lampACIn){
        lamp1=lamp1In;
        lamp2=lamp2In;
        lampAC=lampACIn;
    }

    public String getLamp1() {
        return lamp1;
    }

    public void setLamp1(String lamp1) {
        this.lamp1 = lamp1;
    }

    public String getLamp2() {
        return lamp2;
    }

    public void setLamp2(String lamp2) {
        this.lamp2 = lamp2;
    }

    public String getLampAC() {
        return lampAC;
    }

    public void setLampAC(String lampAC) {
        this.lampAC = lampAC;
    }
}
