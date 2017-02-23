package com.miaxis.faceid_yc.event;

import cn.cloudwalk.sdk.FaceInfo;

/**
 * Created by xu.nan on 2017/2/17.
 */

public class PassEvent {
    private FaceInfo faceInfo;
    private byte[] pCameraData;

    public PassEvent(FaceInfo faceInfo, byte[] pCameraData) {
        this.faceInfo = faceInfo;
        this.pCameraData = pCameraData;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }

    public byte[] getpCameraData() {
        return pCameraData;
    }

    public void setpCameraData(byte[] pCameraData) {
        this.pCameraData = pCameraData;
    }
}
