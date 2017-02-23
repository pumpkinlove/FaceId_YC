package com.miaxis.faceid_yc.event;

/**
 * Created by xu.nan on 2017/2/15.
 */

public class ToastEvent {
    private String message;

    public ToastEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
