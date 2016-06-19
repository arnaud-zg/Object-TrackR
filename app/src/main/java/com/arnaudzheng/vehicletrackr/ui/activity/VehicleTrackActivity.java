package com.arnaudzheng.vehicletrackr.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.arnaudzheng.vehicletrackr.R;
import com.arnaudzheng.vehicletrackr.utils.DetectionBasedTracker;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
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

public class VehicleTrackActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "VehicleTrackActivity";

    @Bind(R.id.vehicle_track_bMain) Button _vehicle_track_bMain;

    /*
    ** OpenCV
     */
    private static final Scalar     OBJECT_RECT_COLOR       = new Scalar(0, 255, 0, 255);
    public static final int         JAVA_DETECTOR           = 0;
    public static final int         NATIVE_DETECTOR         = 1;

    // private MenuItem                mobject;
    // private MenuItem                mfork;
    // private MenuItem                mItemType;
    // private MenuItem                mMyCascade;

    private Mat                     mRgba                   = null;
    private Mat                     mGray                   = null;
    private File                    mCascadeFile            = null;
    private CascadeClassifier       mJavaDetector           = null;
    private DetectionBasedTracker   mNativeDetector         = null;


    private int                     mDetectorType           = JAVA_DETECTOR;
    private String[]                mDetectorName           = null;

    boolean                         objectIsLoaded          = false;
    boolean                         forkIsLoaded            = false;
    boolean                         firstForkLoaded         = false;
    boolean                         myCascadeIsLoaded       = false;

    // private float                  mRelativeFaceSize   = 0.2f;
    // private int                    mAbsoluteFaceSize   = 0;

    private InputStream             is                      = null;

    private CameraBridgeViewBase    mOpenCvCameraView       = null;
    private String                  currentCascade          = "vehicle.xml";
    // private String                  path                    = "";

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

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
    }

    public void startNewActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
        finish();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void setCurrentCascade(String CascadeName){ currentCascade = CascadeName; }

    public String getCurrentCascade(){ return currentCascade; }

    public VehicleTrackActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
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
        MatOfRect objectDetect = new MatOfRect();
        try {
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "vehicle.xml");
            is = getResources().openRawResource(R.raw.vehicle);
            if(!firstForkLoaded) {
                mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                if (mJavaDetector.empty()) {
                    Log.e(TAG, "Failed to load cascade classifier");
                    mJavaDetector = null;
                } else
                    Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }
            firstForkLoaded = true;
            // Load vehicle cascade
            if(getCurrentCascade().equals("vehicle.xml") && !objectIsLoaded) {
                is = getResources().openRawResource(R.raw.vehicle);
                mCascadeFile = new File(cascadeDir, "vehicle.xml");
                mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                objectIsLoaded = true;
                forkIsLoaded = false;
                myCascadeIsLoaded = false;
                if (mJavaDetector.empty()) {
                    Log.e(TAG, "Failed to load cascade classifier");
                    mJavaDetector = null;
                } else
                    Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            cascadeDir.delete();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }


        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, objectDetect, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(50, 50), new Size(600,600));
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, objectDetect);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] objectArray = objectDetect.toArray();
        for (int i = 0; i < objectArray.length; i++)
            Imgproc.rectangle(mRgba, objectArray[i].tl(), objectArray[i].br(), OBJECT_RECT_COLOR, 3);
        return mRgba;
    }

}
