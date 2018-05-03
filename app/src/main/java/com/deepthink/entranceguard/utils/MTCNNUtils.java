package com.deepthink.entranceguard.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.deepthink.entranceguard.Application;
import com.deepthink.entranceguard.utils.MTCNN;
import com.deepthink.entranceguard.utils.MTCNNFaceInfos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


/**
 * Created by yanbo on 2018/3/27.
 */

public class MTCNNUtils {
    private final String TAG = this.getClass().toString();

    private static MTCNNUtils instance = null;

    private MTCNN mtcnn = new MTCNN();

    public static MTCNNUtils getInstance() {
        if (instance == null) {
            instance = new MTCNNUtils();
            instance.init();
        }
        return instance;
    }

    public MTCNNFaceInfos faceDetect(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel) {
//        Log.e(TAG, "检测开始");
        long timeDetectFace = System.currentTimeMillis();
        int[] faceInfo = mtcnn.FaceDetect(imageDate, imageWidth, imageHeight, imageChannel);
        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
//        Log.e(TAG, "人脸检测时间：" + timeDetectFace);
        return new MTCNNFaceInfos(faceInfo);
    }

    public MTCNNFaceInfos faceDetect(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        byte[] imageDate = getPixelsRGBA(bmp);
        return faceDetect(imageDate, width, height, 4);
    }

    public float[] getFaceFeatures(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        byte[] imageDate = getPixelsRGBA(bmp);
        return mtcnn.FaceFeature(imageDate, width, height, 4);
    }

    private void init() {
        try {
            copyBigDataToSD("det1.bin");
            copyBigDataToSD("det2.bin");
            copyBigDataToSD("det3.bin");
            copyBigDataToSD("fage.bin");
            copyBigDataToSD("fgender.bin");
            copyBigDataToSD("y1.bin");
            copyBigDataToSD("det1.param");
            copyBigDataToSD("det2.param");
            copyBigDataToSD("det3.param");
            copyBigDataToSD("fage.param");
            copyBigDataToSD("fgender.param");
            copyBigDataToSD("y1.param");
        } catch (IOException e) {
            e.printStackTrace();
        }

        File sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        String sdPath = sdDir.toString() + "/mtcnn/";
        mtcnn.FaceDetectionModelInit(sdPath);
    }

    private void copyBigDataToSD(String strOutFileName) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        File file = new File(sdDir.toString()+"/mtcnn/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/mtcnn/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i(TAG, "file exists " + strOutFileName);
            return;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/mtcnn/"+ strOutFileName);
        myInput = Application.getApplication().getAssets().open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i(TAG, "end copy file " + strOutFileName);

    }

    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }
}
