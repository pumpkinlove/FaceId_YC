package com.miaxis.faceid_yc.event;

import cn.cloudwalk.sdk.FaceInfo;

/**
 * Created by xu.nan on 2017/2/16.
 */

public class FaceDetectedEvent {
    private FaceInfo[] faceInfos;

    public FaceDetectedEvent(FaceInfo[] faceInfos) {
        this.faceInfos = faceInfos;
    }

    public FaceInfo[] getFaceInfo() {
        return faceInfos;
    }

    public void setFaceInfo(FaceInfo[] faceInfos) {
        this.faceInfos = faceInfos;
    }
}
