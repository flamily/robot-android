package net.flamily.irs.robot.image_capture;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;


public class CaptureImageReceiver extends BroadcastReceiver {

    //Also declare the interface in your BroadcastReceiver as static
    private static ICaptureImageReceiver iCaptureImageReceiver;
    private String TAG = "ImageCaptureReceiver";
    private String INTENT_ACTION = "ImageCaptureAction";
    private Camera mCamera;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Log.d(TAG, "Picture length : " + data.length);
            iCaptureImageReceiver.sendImage(data, true);
            camera.release();
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (INTENT_ACTION.equals(intent.getAction())) {
            try {
                takePhoto();
            } catch (RuntimeException e) {
                iCaptureImageReceiver.sendImage(null, false);
            }
        }
    }

    public void registerCallback(ICaptureImageReceiver iCaptureImageReceiver) {
        Log.e(TAG, "callback registered");
        CaptureImageReceiver.iCaptureImageReceiver = iCaptureImageReceiver;

    }


    public void takePhoto() {
        if (Camera.getNumberOfCameras() >= 2) {

            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        if (Camera.getNumberOfCameras() < 2) {

            mCamera = Camera.open();
        }
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);


        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Parameters parameters = mCamera.getParameters();
        mCamera.setParameters(parameters);
        mCamera.startPreview();

        mCamera.takePicture(null, null, mPicture);
    }

    public interface ICaptureImageReceiver {
        void sendImage(byte[] data, boolean success);
    }

}

