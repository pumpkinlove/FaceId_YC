package com.miaxis.faceid_yc.event;

/**
 * Created by xu.nan on 2017/2/17.
 */

public class FingerPassEvent {
    private int index;

    public FingerPassEvent(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
