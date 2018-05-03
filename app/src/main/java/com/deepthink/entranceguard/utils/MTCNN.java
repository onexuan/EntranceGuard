package com.deepthink.entranceguard.utils;

/**
 * Created by wby on 2018/03/21.
 */

public class MTCNN {
    //人脸检测模型导入
    //input:model path
    public native boolean FaceDetectionModelInit(String faceDetectionModelPath);

    //人脸检测
    //return:faceInfo,
    //faceINfo[0] is the face number
    //faceInfo[14*i+1] = finalBbox[i].x1;//left
    //faceInfo[14*i+2] = finalBbox[i].y1;//top
    //faceInfo[14*i+3] = finalBbox[i].x2;//right
    //faceInfo[14*i+4] = finalBbox[i].y2;//bottom
    //i is the i th face
    public native int[] FaceDetect(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);
    public native int[] FaceInfo(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);
    public native float[] FaceFeature(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);

    //人脸检测模型反初始化
    public native boolean FaceDetectionModelUnInit();

    static {
        System.loadLibrary("mtcnn");
    }
}
