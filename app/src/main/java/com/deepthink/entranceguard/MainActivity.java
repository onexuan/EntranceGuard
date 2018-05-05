package com.deepthink.entranceguard;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.deepthink.entranceguard.utils.MTCNNFaceInfos;
import com.deepthink.entranceguard.utils.MTCNNUtils;
import com.deepthink.entranceguard.utils.SimpleStorage;
import com.deepthink.entranceguard.view.FaceDetectView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.video.Video;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

public class MainActivity extends Activity implements View.OnClickListener, FaceDetectView.OnCameraListener, FaceDetectView.OnPictureSaved {
    private final String TAG = this.getClass().getName();
    private final static int MSG_CATCH_FACE = 0x0000;
    private final static int MSG_MATCH_FACE = 0x0001;
    private final static int MSG_MATCH_INFO = 0x0002;

    private FaceDetectView mCameraView = null;
    private ImageView mRegister = null;
    private ImageView mBorder = null;
    private ImageView mMatched = null;
    private TextView mInfo = null;

    private SimpleStorage mData = null;
    private UIHandler mHandler = new UIHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = (FaceDetectView) findViewById(R.id.detect_view);
        mCameraView.setCameraListener(this);
        mCameraView.setSavedCallback(this);

        mRegister = (ImageView) findViewById(R.id.register);
        mRegister.setOnClickListener(this);

        mBorder = (ImageView) findViewById(R.id.border);
        mBorder.setVisibility(View.INVISIBLE);

        mMatched = (ImageView) findViewById(R.id.matched);
        mMatched.setVisibility(View.INVISIBLE);

        mInfo = (TextView) findViewById(R.id.info);
        mInfo.setVisibility(View.INVISIBLE);
        mInfo.setText("");

        mData = new SimpleStorage((Application) getApplication());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register:
                mCameraView.takePicture();
                break;
        }
    }

    @Override
    public Object onPreview(Bitmap src, byte[] data, int width, int height) {
        Matrix matrix = new Matrix();
        matrix.setRotate(270);
        Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);

        MTCNNFaceInfos faceInfos = MTCNNUtils.getInstance().faceDetect(bmp);
        Rect[] rects = new Rect[faceInfos.list.size()];

        int mainFaceIndex = -1;
        int faceArea = 0;
        for (int i = 0; i < faceInfos.list.size(); i++) {
            MTCNNFaceInfos.MTCNNFaceInfo faceInfo = faceInfos.list.get(i);
            rects[i] = new Rect(faceInfo.left, faceInfo.top, faceInfo.right, faceInfo.bottom);
            int area = rects[i].width() * rects[i].height();
            if (faceArea < area) {
                faceArea = area;
                mainFaceIndex = i;
            }
            Log.e(TAG, "onPreview: face angle " + faceInfo.angle);
        }

        if (mainFaceIndex >= 0) {
            Rect rect = rects[mainFaceIndex];
            Message msg = new Message();
            msg.what = MSG_CATCH_FACE;
            msg.arg1 = faceArea;
            if (faceArea > 0) msg.obj = rect;
            mHandler.sendMessage(msg);

            int x = rects[mainFaceIndex].left;
            int y = rects[mainFaceIndex].top;
            int faceWidth = rects[mainFaceIndex].width();
            int faceHeight = rects[mainFaceIndex].height();
            Bitmap face_bitmap = Bitmap.createBitmap(bmp, x, y, faceWidth, faceHeight);

            float[] features = MTCNNUtils.getInstance().getFaceFeatures(face_bitmap);

            Vector<SimpleStorage.FaceRegister> registers = mData.getFaces();
            int matchedIndex = 0;
            float matchedValue = 0;
            for (int i = 0; i < registers.size(); i++) {
                SimpleStorage.FaceRegister one = registers.get(i);
                float val = 0;
                for (int j = 0; j < features.length && j < one.features.length; j++) {
                    val += features[j] * one.features[j];
                }
                if (matchedValue < val) {
                    matchedValue = val;
                    matchedIndex = i;
                }
            }
            if (matchedValue > 0    ) {
                Bitmap img = registers.get(matchedIndex).img;
                Message msg1 = new Message();
                msg1.what = MSG_MATCH_FACE;
                msg1.obj = img;
                mHandler.sendMessage(msg1);
            }

            int[] info = MTCNNUtils.getInstance().getFaceInfo(face_bitmap);
            int age = info[0];
            int gender = info[1];

            Message msg2 = new Message();
            msg2.what = MSG_MATCH_INFO;
            msg2.arg1 = age;
            msg2.arg2 = gender;
            mHandler.handleMessage(msg2);
        }

        return null;
    }

    @Override
    public void onPictureSaved(Bitmap src) {
        Matrix matrix = new Matrix();
        matrix.setRotate(270);
        Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);

//        Resources r = this.getApplication().getResources();
//        bmp = BitmapFactory.decodeResource(r, R.drawable.test);

        MTCNNFaceInfos faceInfos = MTCNNUtils.getInstance().faceDetect(bmp);
        if (faceInfos.list.size() > 0) {
            Rect[] rects = new Rect[faceInfos.list.size()];

            int mainFaceIndex = 0;
            int faceArea = 0;
            for (int i = 0; i < faceInfos.list.size(); i++) {
                MTCNNFaceInfos.MTCNNFaceInfo faceInfo = faceInfos.list.get(i);
                rects[i] = new Rect(faceInfo.left, faceInfo.top, faceInfo.right, faceInfo.bottom);
                int area = rects[i].width() * rects[i].height();
                if (faceArea < area) {
                    faceArea = area;
                    mainFaceIndex = i;
                }
                Log.e(TAG, "onPreview: face angle " + faceInfo.angle);
            }
            int x = rects[mainFaceIndex].left;
            int y = rects[mainFaceIndex].top;
            int faceWidth = rects[mainFaceIndex].width();
            int faceHeight = rects[mainFaceIndex].height();
            Bitmap face_bitmap = Bitmap.createBitmap(bmp, x, y, faceWidth, faceHeight);

            float[] features = MTCNNUtils.getInstance().getFaceFeatures(face_bitmap);
            mData.save(face_bitmap, features);
            Toast.makeText(MainActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
        }

    }

//    private Bitmap align(Bitmap bmp, MTCNNFaceInfos.MTCNNFaceInfo info) {
//        Point[] src = new Point[5];
//        Point[] dst = new Point[5];
//
//        src[0] = new Point(38.2946, 51.6963);
//        src[1] = new Point(73.5318, 51.5014);
//        src[2] = new Point(56.0252, 71.7366);
//        src[3] = new Point(41.5493, 92.3655);
//        src[4] = new Point(70.7299, 92.2041);
//
//        for (int i = 0; i < 5; i++) {
//            dst[i] = new Point(info.facePoints.get(i).x, info.facePoints.get(i).y);
//        }
//
//    }

    public byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CATCH_FACE) {
                float area = msg.arg1;
                if (area > 0) {
                    Rect oldRect = (Rect) msg.obj;
                    Camera.Size size = mCameraView.getCameraSize();
                    Rect rect = new Rect(size.height - oldRect.bottom, oldRect.left, size.height - oldRect.top, oldRect.right);
                    ViewGroup.MarginLayoutParams margin9 = new ViewGroup.MarginLayoutParams(
                            mBorder.getLayoutParams());
                    margin9.setMargins(rect.left, rect.top, 0, 0);
                    RelativeLayout.LayoutParams layoutParams9 = new RelativeLayout.LayoutParams(margin9);
                    layoutParams9.height = rect.height();
                    layoutParams9.width = rect.width();
                    mBorder.setLayoutParams(layoutParams9);
                    mBorder.setVisibility(View.VISIBLE);
                }
                else {
                    mBorder.setVisibility(View.INVISIBLE);
                }
            } else if (msg.what == MSG_MATCH_FACE) {
                Bitmap img = (Bitmap)msg.obj;
                mMatched.setImageBitmap(img);
                mMatched.setVisibility(View.VISIBLE);
            } else if (msg.what == MSG_MATCH_INFO) {
                int age = msg.arg1;
                int gender = msg.arg2;
                String str = "";
                if (gender > 0) {
                    str += "男";
                } else {
                    str += "女";
                }

                str += " " + age * 10 + "~" + (age + 1) * 10 + "岁";
                mInfo.setText(str);
                mInfo.setVisibility(View.VISIBLE);
            }
        }
    }
}
