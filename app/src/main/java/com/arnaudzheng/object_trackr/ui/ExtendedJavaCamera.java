package com.arnaudzheng.object_trackr.ui;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

/**
 * Created by Xiang on 16/08/2016.
 * Useless
 */

@SuppressWarnings("deprecation")
public class ExtendedJavaCamera extends JavaCameraView {

    public ExtendedJavaCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedJavaCamera(Context context, int cameraId) {
        super(context, cameraId);
    }

    public Camera getCurrentCamera() {
        return mCamera;
    }

}
