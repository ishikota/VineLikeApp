/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.ikota.vinelikeapp.ui;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.ikota.vinelikeapp.R;
import com.ikota.vinelikeapp.util.CameraHelper;
import com.ikota.vinelikeapp.util.CombineVideoTask;
import com.ikota.vinelikeapp.util.VineHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements TextureView.SurfaceTextureListener{

    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;

    private boolean isRecording = false;

    private long startRecordingTime;
    private long currentRecordingTime;

    private View mHeaderOverlay;
    private CamcorderProfile mProfile;
    private ArrayList<File> mMovies = new ArrayList<>();

    private Handler mHandler;
    private Timer mTimer;
    private TimerTask mTimerTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        mHeaderOverlay = findViewById(R.id.header_overlay);

        mPreview = (TextureView) findViewById(R.id.surface_view);
        mPreview.setSurfaceTextureListener(this);
        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (isRecording) {
//                    mMediaRecorder.stop();  // stop the recording
//                    isRecording = false;
//                } else {
//                    new MediaPrepareTask().execute(null, null, null);
//                }
//                int currentWidth = mHeaderOverlay.getWidth();
//                int targetWidth = mHeaderOverlay.getWidth() + 100;
//                ValueAnimator widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth);
//                widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                        int val = (Integer) valueAnimator.getAnimatedValue();
//                        ViewGroup.LayoutParams layoutParams = mHeaderOverlay.getLayoutParams();
//                        layoutParams.width = val;
//                        mHeaderOverlay.setLayoutParams(layoutParams);
//                    }
//                });
//                widthAnimator.setDuration(1000);
//                widthAnimator.start();
            }
        });

        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRecordingTime = System.currentTimeMillis();
                        mTimerTask = new UpdateProgressTask();
                        mTimer = new Timer(true);
                        mTimer.schedule(mTimerTask, 100, 100);
                        new MediaPrepareTask().execute(null, null, null);
                        Log.d("time", String.format("startRecordingTime:%d(mills)",startRecordingTime));
                        break;
                    case MotionEvent.ACTION_UP:
                        mTimer.cancel();
                        mTimer = null;
                        mMediaRecorder.stop();  // stop the recording
                        break;
                }
                return false;
            }
        });

        findViewById(R.id.action_combine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CombineVideoTask(MainActivity.this, mMovies).execute();
            }
        });
    }

    private void updateProgress() {
        long current_time = System.currentTimeMillis();
        currentRecordingTime += current_time - startRecordingTime;
        ViewGroup.LayoutParams layoutParams = mHeaderOverlay.getLayoutParams();
        layoutParams.width = VineHelper.calcProgressSize(currentRecordingTime);
        mHeaderOverlay.setLayoutParams(layoutParams);
        Log.d("time", String.format("finishRecordingTime:%d(mills)", current_time));
        Log.d("time", String.format("pressed time:%f(s)"           , (current_time - startRecordingTime)/1000.0));
        Log.d("time", String.format("currentRecordingTime:%f(s)"   , currentRecordingTime/1000.0));
        startRecordingTime = current_time;
    }

    private class UpdateProgressTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateProgress();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();  // if we are using MediaRecorder, release it first
        releaseCamera();  // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();  // clear recorder configuration
            mMediaRecorder.release();  // release the recorder object
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        setupCamera();  // adjust camera orientation to fit portrait screen
        try {
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        // never changes in this app. (Orientation is fixed to portrait)
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // do nothing here
    }

    @SuppressWarnings("deprecation")
    private void setupCamera() {
        mCamera = CameraHelper.getDefaultCameraInstance();
        mCamera.setDisplayOrientation(90);
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording profile.
        mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mProfile.videoFrameWidth = optimalSize.width;
        mProfile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        parameters.setPreviewSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mCamera.setParameters(parameters);
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                isRecording = true;
            } else {
                releaseMediaRecorder();  // prepare didn't work, release the camera
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                MainActivity.this.finish();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean prepareVideoRecorder(){

        // mProfile should be set in setupCamera method.
        assert mProfile != null;

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setOrientationHint(90);

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(mProfile);

        // Step 4: Set output file
        File mediaFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        assert mediaFile != null;
        mMediaRecorder.setOutputFile(mediaFile.toString());
        CameraHelper.registerToMediaScanner(MainActivity.this, mediaFile);
        mMovies.add(mediaFile);

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("prepareVideoRecorder", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("prepareVideoRecorder", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

}