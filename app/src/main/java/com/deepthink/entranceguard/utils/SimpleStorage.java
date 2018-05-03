package com.deepthink.entranceguard.utils;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.deepthink.entranceguard.Application;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by yanbo on 2018/5/3.
 */

public class SimpleStorage {
    private final String TAG = this.getClass().toString();
    private final String KEY = "data";

    private Application mApplication = null;
    private Vector<FaceRegister> mFaces = new Vector<>();

    public class FaceRegister {
        public Bitmap img;
        public float[] features;
    }

    public SimpleStorage(Application application) {
        mApplication = application;
        mFaces.clear();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplication);
        String json = sharedPreferences.getString(KEY, "[]");

        try {
            JSONArray data = new JSONArray(json);
            for (int i = 0; i < data.length(); i++) {
                FaceRegister fr = new FaceRegister();
                JSONObject obj = data.getJSONObject(i);
                fr.img = convertStringToIcon(obj.getString("img"));
                JSONArray jsonList = obj.getJSONArray("features");
                fr.features = new float[jsonList.length()];
                for (int j = 0; j < jsonList.length(); j++) {
                    fr.features[j] = (float)jsonList.getDouble(i);
                }
                mFaces.add(fr);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void save(Bitmap face, float[] features) {
        FaceRegister fr = new FaceRegister();
        fr.img = face;
        fr.features = features;
        mFaces.add(fr);

        try {
            JSONArray data = new JSONArray();
            for (int i = 0; i < mFaces.size(); i++) {
                JSONObject one = new JSONObject();
                one.put("img", convertIconToString(fr.img));
                JSONArray list = new JSONArray();
                for (int j = 0; j < features.length; j++) {
                    list.put(features[j]);
                }
                one.put("features", list);
                data.put(one);
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplication);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY, data.toString());
            editor.commit();

            Log.e(TAG, "save: " + data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Vector<FaceRegister> getFaces() {
        return mFaces;
    }

    public static String convertIconToString(Bitmap bitmap)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] appicon = baos.toByteArray();
        return Base64.encodeToString(appicon, Base64.DEFAULT);

    }

    public static Bitmap convertStringToIcon(String st)
    {
        Bitmap bitmap = null;
        try
        {
            byte[] bitmapArray;
            bitmapArray = Base64.decode(st, Base64.DEFAULT);
            bitmap =
                    BitmapFactory.decodeByteArray(bitmapArray,0, bitmapArray.length);
            return bitmap;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static Drawable bitmap2drawable(Bitmap bitmap) {
        return new BitmapDrawable(bitmap);
    }

    public static Bitmap drawable2bitmap(Drawable drawable) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();

        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }
}
