package com.arnaudzheng.object_trackr.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.arnaudzheng.object_trackr.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.Bind;
import butterknife.ButterKnife;

public class VehicleTrackActivity extends Activity implements CvCameraViewListener2 {

    private static final String             TAG                     = "VehicleTrackActivity";
    @Bind(R.id.vehicle_track_bMain) Button  _vehicle_track_bMain    = null;

    private CameraBridgeViewBase            mOpenCvCameraView       = null;
    private static final Scalar             OBJECT_RECT_COLOR       = new Scalar(0, 255, 0, 255);
    private File                            mCascadeFile            = null;
    private CascadeClassifier               mJavaDetector           = null;
    private Mat                             mGray                   = null;
    private Mat                             mRgba                   = null;
    private String                          pCascadeFile            = "haarcascade_vehicle.xml";

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    public VehicleTrackActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    try {
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_vehicle);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, pCascadeFile);
                        FileOutputStream os = new FileOutputStream(mCascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "failed to load cascade");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade from " + mCascadeFile.getAbsolutePath());
                        cascadeDir.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "failed to load cascade.Exception " + e);
                    }
                    mOpenCvCameraView.enableView();
                } break ;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public void startNewActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vehicle_track);
        ButterKnife.bind(this);
        this._vehicle_track_bMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewActivity(MainActivity.class);
            }
        });
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.vehicle_track_jcvCamera);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(640, 360);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_vehicle, menu);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Mat mrgbat = mRgba.t();
        Mat mgrayt = mGray.t();
        Core.flip(mrgbat,mrgbat,1);
        Core.flip(mgrayt,mgrayt,1);
        Imgproc.resize(mrgbat,mrgbat, mRgba.size()) ;
        Imgproc.resize(mgrayt,mgrayt, mGray.size()) ;
        MatOfRect objects = new MatOfRect();
        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mgrayt, objects, 1.1, 2, 2, new Size(50, 50), new Size(600, 600));
        org.opencv.core.Rect[] objectsArray = objects.toArray();
        for (int i=0; i < objectsArray.length; i++)
            Imgproc.rectangle(mRgba, objectsArray[i].tl(), objectsArray[i].br(), OBJECT_RECT_COLOR, 3);
        return mRgba;
    }

}
