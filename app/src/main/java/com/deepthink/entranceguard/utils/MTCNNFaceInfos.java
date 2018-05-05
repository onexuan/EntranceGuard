package com.deepthink.entranceguard.utils;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yanbo on 2018/3/28.
 */

public class MTCNNFaceInfos {
    private final String TAG = this.getClass().toString();

    private int total = 25;
    public List<MTCNNFaceInfo> list;

    public class MTCNNFaceInfo {
        private final String TAG = this.getClass().toString();

        public int left;
        public int right;
        public int top;
        public int bottom;

        public List<Point> points;
        public List<Point> facePoints;

        public int angle;

        public MTCNNFaceInfo(int left, int right, int top, int bottom, List<Point> points, int angle, List<Point> facePoints) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            this.points = points;
            this.angle = angle;
            this.facePoints = facePoints;
        }
    }

    public MTCNNFaceInfos(int[] faceInfo) {
        list = new ArrayList<>();

        if (faceInfo.length > 1) {
            int faceNum = faceInfo[0];

//            Log.e(TAG, "人脸数目：" + faceNum );

            for(int i=0;i<faceNum;i++) {
                int left = faceInfo[1+total*i];
                int top = faceInfo[2+total*i];
                int right = faceInfo[3+total*i];
                int bottom = faceInfo[4+total*i];
                List<Point> points = new ArrayList<>();
                List<Point> facePoints = new ArrayList<>();
                for(int j = 0; j < 5; j++){
                    int x = faceInfo[total*i+5+j];
                    int y = faceInfo[total*i+5+j+5];
                    points.add(new Point(x, y));
                }
                int angle = faceInfo[total*i+15];

                for (int j = 0; j < 5; j++) {
                    int x = faceInfo[total*i+16+2*j];
                    int y = faceInfo[total*i+16+2*j+1];
                    facePoints.add(new Point(x, y));
                }
                list.add(new MTCNNFaceInfo(left, right, top, bottom, points, angle, facePoints));
            }
        }
        else {
//            Log.e(TAG, "未检测到人脸");
        }
    }
}

