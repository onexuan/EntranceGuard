package com.deepthink.entranceguard;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.deepthink.entranceguard.utils.MTCNNUtils;
import com.deepthink.entranceguard.utils.SimpleStorage;
import com.deepthink.entranceguard.view.FaceDetectView;

public class MainActivity extends Activity implements View.OnClickListener, FaceDetectView.OnCameraListener, FaceDetectView.OnPictureSaved {
    private static String TAG = MainActivity.class.getName();

    private FaceDetectView mCameraView = null;
    private ImageView mRegister = null;

    private SimpleStorage mData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = (FaceDetectView) findViewById(R.id.detect_view);
        mCameraView.setCameraListener(this);
        mCameraView.setSavedCallback(this);

        mRegister = (ImageView) findViewById(R.id.register);
        mRegister.setOnClickListener(this);

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
    public Object onPreview(Bitmap bmp, byte[] data, int width, int height) {
        return null;
    }

    @Override
    public void onPictureSaved(Bitmap bmp) {
        float[] features = MTCNNUtils.getInstance().getFaceFeatures(bmp);
        mData.save(bmp, features);
    }
}
