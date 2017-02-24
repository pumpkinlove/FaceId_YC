package com.miaxis.faceid_yc.event;

import android.graphics.Bitmap;

/**
 * Created by xu.nan on 2017/2/15.
 */

public class NewIdEvent {

    private String name;
    private String idNum;
    private String gender;
    private String race;
    private String birthday;
    private String address;
    private String regOrg;
    private String validTime;
    private Bitmap bitmap;

    private boolean hasFinger;//是否有指纹

    private String fingerPosition1;
    private String fingerPosition2;

    private byte[] finger0;
    private byte[] finger1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdNum() {
        return idNum;
    }

    public void setIdNum(String idNum) {
        this.idNum = idNum;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegOrg() {
        return regOrg;
    }

    public void setRegOrg(String regOrg) {
        this.regOrg = regOrg;
    }

    public String getValidTime() {
        return validTime;
    }

    public void setValidTime(String validTime) {
        this.validTime = validTime;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getFingerPosition1() {
        return fingerPosition1;
    }

    public void setFingerPosition1(String fingerPosition1) {
        this.fingerPosition1 = fingerPosition1;
    }

    public boolean isHasFinger() {
        return hasFinger;
    }

    public void setHasFinger(boolean hasFinger) {
        this.hasFinger = hasFinger;
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

    public String getFingerPosition2() {
        return fingerPosition2;
    }

    public void setFingerPosition2(String fingerPosition2) {
        this.fingerPosition2 = fingerPosition2;
    }
}
