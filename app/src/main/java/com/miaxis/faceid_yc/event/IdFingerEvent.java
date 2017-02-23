package com.miaxis.faceid_yc.event;

/**
 * Created by xu.nan on 2017/1/17.
 */

public class IdFingerEvent {

    private boolean flag;

    private String fingerPosition1;
    private String fingerPosition2;

    private byte[] finger0;
    private byte[] finger1;

    public IdFingerEvent() {
    }

    public IdFingerEvent(boolean flag, byte[] finger0, byte[] finger1) {
        this.flag = flag;
        this.finger0 = finger0;
        this.finger1 = finger1;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public byte[] getFinger0() {
        return finger0;
    }

    public void setFinger0(byte[] finger0) {
        this.finger0 = finger0;
    }

    public byte[] getFinger1() {
        return finger1;
    }

    public void setFinger1(byte[] finger1) {
        this.finger1 = finger1;
    }

    public String getFingerPosition1() {
        return fingerPosition1;
    }

    public void setFingerPosition1(String fingerPosition1) {
        this.fingerPosition1 = fingerPosition1;
    }

    public String getFingerPosition2() {
        return fingerPosition2;
    }

    public void setFingerPosition2(String fingerPosition2) {
        this.fingerPosition2 = fingerPosition2;
    }
}
