package com.miaxis.faceid_yc.event;

/**
 * Created by xu.nan on 2017/2/15.
 */

public class InfoEvent {
    private int code;
    private String info;

    public InfoEvent(int code, String info) {
        this.code = code;
        this.info = info;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
