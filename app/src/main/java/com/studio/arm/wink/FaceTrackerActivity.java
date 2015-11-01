/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.studio.arm.wink;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.studio.arm.R;
import com.studio.arm.wink.ui.camera.CameraSourcePreview;
import com.studio.arm.wink.ui.camera.GraphicOverlay;
import com.studio.arm.wink.ui.camera.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.util.VKUtil;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity {

    private static final String[] sMyScope = new String[]{
            //VKScope.FRIENDS,
            VKScope.WALL,
            VKScope.PHOTOS,
            //VKScope.NOHTTPS,
            //VKScope.MESSAGES,
            //VKScope.DOCS
    };

    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private static Handler handler;
    private ImageView eye;
    private AnimationDrawable mAnimationDrawable;
    private ImageButton settings;
    private ImageButton rotateCamera;
    private Boolean flag=false;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final int RC_HANDLE_WRITE_PERM = 3;
    private static final int RC_HANDLE_READ_PERM = 4;


    private final String FACE_DETECT = "1";
    private final String FACE_NONE = "2";
    private static final String FILENAME = "setting_file";

    private SharedPreferences sharedPref;
    public static final String PREF_FILE_NAME = "PrefFile";


    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);


        //VK
        if (VKAccessToken.currentToken()==null || VKAccessToken.currentToken().accessToken==null) {
            VKSdk.login(this, sMyScope);
        }
        //new ProgressTask().execute();

        setContentView(R.layout.main);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        eye = (ImageView) findViewById(R.id.imageEye);
        eye.setBackgroundResource(R.drawable.eyeanim);
        mAnimationDrawable = (AnimationDrawable)eye.getBackground();
        settings = (ImageButton) findViewById(R.id.imageSetting);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(FaceTrackerActivity.this, Setting.class);
                startActivity(intent);
                overridePendingTransition(R.animator.push_down_in, R.animator.push_down_out);
            }
        });


        rotateCamera = (ImageButton) findViewById(R.id.imageRotateCamera);
        rotateCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //mCameraSource.release();
                flag=!flag;
                if(mCameraSource!=null)
                    mCameraSource.release();
                createCameraSource(flag?CameraSource.CAMERA_FACING_BACK: CameraSource.CAMERA_FACING_FRONT);
                startCameraSource();
                Message msg = new Message();
                msg.obj = FACE_NONE;
                handler.sendMessage(msg);
            }
        });

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(CameraSource.CAMERA_FACING_FRONT);
            startCameraSource();
        } else {
            requestCameraPermission();
        }
        int rmw = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rmw != PackageManager.PERMISSION_GRANTED) {
            requestMemoryWritePermission();
        }
        int rmr = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (rmr != PackageManager.PERMISSION_GRANTED) {
            requestMemoryWritePermission();
        }

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String text = (String) msg.obj;

                try {
                if (text.equals(FACE_DETECT)) {
                    eye.setBackgroundResource(R.drawable.eyeanim);
                    mAnimationDrawable = (AnimationDrawable) eye.getBackground();
                    mAnimationDrawable.start();
                }
                if (text.equals(FACE_NONE)) {
                    mAnimationDrawable.stop();
                    eye.setBackgroundResource(R.drawable.eye3);
                }
                } catch (NullPointerException e){}
            }
        };

        sharedPref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);

    }





    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void requestMemoryWritePermission() {
        Log.w(TAG, "Memory permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_WRITE_PERM);
            return;
        }

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_READ_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_WRITE_PERM);
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_READ_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_write_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource(int source) {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(false)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }
//        Display display = getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int width = size.x;
//        int height = size.y;
//
//        int orientation = context.getResources().getConfiguration().orientation;
//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mCameraSource = new CameraSource.Builder(context, detector)
//                    .setRequestedPreviewSize(height, width)
//                    .setFacing(source)
//                    .setRequestedFps(15.0f)
//                    .build();
//            Log.d("ScereenSize", "h: "+height+" w: "+width);
//        }
//        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            mCameraSource = new CameraSource.Builder(context, detector)
//                    .setRequestedPreviewSize(height, width)
//                    .setFacing(source)
//                    .setRequestedFps(15.0f)
//                    .build();
//            Log.d("ScereenSize", "h: "+height+" w: "+width);
//        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(1080, 1080)
                .setFacing(source)
                .setRequestedFps(30.0f)
                .build();
        //Log.d("ScereenSize", "h: "+height+" w: "+width);

    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(mCameraSource!=null)
//            mCameraSource.release();
//        createCameraSource(flag?CameraSource.CAMERA_FACING_BACK: CameraSource.CAMERA_FACING_FRONT);
        startCameraSource();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }



    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource(CameraSource.CAMERA_FACING_FRONT);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                // Пользователь успешно авторизовался
            }
            @Override
            public void onError(VKError error) {
                // Произошла ошибка авторизации (например, пользователь запретил авторизацию)
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void takePicture() {
        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                File picture;

                @Override
                public void onPictureTaken(byte[] bytes) {
                    try {
                        Thread.currentThread().sleep((long) (sharedPref.getFloat(FILENAME, 1f) * 1000));

                        mPreview.stop();
                        picture = null;
                        try {
                            picture = FaceFile.savePicture(bytes, "jpg");
                        } catch (IOException e){
                            e.printStackTrace();
                            return;
                        }

                        new AlertDialog.Builder(FaceTrackerActivity.this)
                                .setTitle("Share")
                                .setMessage("Share to")
                                .setPositiveButton("Snapster", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent shareIntent = new Intent().setPackage("com.vk.snapster");
                                        shareIntent.setType("image/jpeg");
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(picture));
                                        startActivity(Intent.createChooser(shareIntent, ""));
                                    }
                                })
                                .setNegativeButton("VK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        
                                        new ProgressTask().execute(picture);
                                        createCameraSource(flag?CameraSource.CAMERA_FACING_BACK: CameraSource.CAMERA_FACING_FRONT);
                                        startCameraSource();
                                        Message msg = new Message();
                                        msg.obj = FACE_NONE;
                                        handler.sendMessage(msg);
                                    }
                                })
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        createCameraSource(flag?CameraSource.CAMERA_FACING_BACK: CameraSource.CAMERA_FACING_FRONT);
                                        startCameraSource();
                                        Message msg = new Message();
                                        msg.obj = FACE_NONE;
                                        handler.sendMessage(msg);
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } catch (InterruptedException e) {
                    } catch (RuntimeException r) {
                        Log.d("Errorrrrrrrr", "RuntimeError");
                    }
                }
            });

    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;
        private static final double WINK = 0.5;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            Message msg = new Message();
            msg.obj = FACE_DETECT;
            handler.sendMessage(msg);
            float leftEye = face.getIsLeftEyeOpenProbability();
            float rightEye = face.getIsRightEyeOpenProbability();
            if (Math.abs(leftEye - rightEye) >= WINK) {
                    takePicture();
            }
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);

        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            Message msg = new Message();
            msg.obj = FACE_NONE;
            handler.sendMessage(msg);
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            Message msg = new Message();
            msg.obj = FACE_NONE;
            handler.sendMessage(msg);
            mOverlay.remove(mFaceGraphic);
        }
    }



}
