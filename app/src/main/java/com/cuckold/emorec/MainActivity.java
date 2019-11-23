package com.cuckold.emorec;




import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 34;
    private static final int PERMISSIONS_COUNT = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takePhoto = findViewById(R.id.takePhoto);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();

            }
        });
    }
    private boolean arePermissionsDenied(){
        for (int i = 0; i < PERMISSIONS_COUNT; i++){
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }


    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                          int[]grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            if (arePermissionsDenied()){
                ((ActivityManager)(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }
    }
    private boolean isCameraInitialized;

    private Camera mCamera = null;

    private static SurfaceHolder myHolder;

    private static CameraPreview mPreview;

    private FrameLayout preview;

    private Button flashB;

    private static OrientationEventListener orientationEventListener = null;

    private static boolean fM;

    private final int REQUEST_IMAGE_CAPTURE = 1;

    String currentPhotoPath;

    private File createImageFile() throws IOException{
        //Create an image filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
          imageFileName, /* prefix */
          ".jpg", /* suffix */
                storageDir /* directory */
        );
        //Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null){
            //Creating a file where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }catch (IOException ex) {
                //error
            }
            //Continues only when the file was successfully created
            if(photoFile != null){
                Uri photoUri = FileProvider.getUriForFile(this, "com.cuckold.emorec.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if (!isCameraInitialized){
            mCamera = Camera.open();
            mPreview = new CameraPreview(this, mCamera);
            preview = findViewById(R.id.camera_preview);
            preview.addView(mPreview);
            rotateCamera();
            flashB = findViewById(R.id.flash);
            if (hasFlash()){
                flashB.setVisibility(View.VISIBLE);
            }else{
                flashB.setVisibility(View.GONE);
            }
            final Button switchCameraButton = findViewById(R.id.switchCamera);
            switchCameraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCamera.release();
                    switchCamera();
                    rotateCamera();
                    try {
                        mCamera.setPreviewDisplay(myHolder);
                    }catch (Exception e){

                    }
                    mCamera.startPreview();
                    if(hasFlash()){
                        flashB.setVisibility(View.VISIBLE);
                    }else{
                        flashB.setVisibility(View.GONE);
                    }
                }
            });

            orientationEventListener = new OrientationEventListener(this) {
                @Override
                public void onOrientationChanged(int orientation) {
                    rotateCamera();
                }
            };
            orientationEventListener.enable();
            preview.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (whichCamera){
                        if(fM){
                            p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }else{
                            p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        }
                        try {
                            mCamera.setParameters(p);
                        }catch (Exception e){

                        }
                        fM = !fM;
                    }
                    return true;
                }
            });
        }
    }
    private void switchCamera(){
        if(whichCamera){
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }else{
            mCamera = Camera.open();
        }
        whichCamera = !whichCamera;
    }

    @Override
    protected void onPause(){
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera(){
        if(mCamera != null){
            preview.removeView(mPreview);
            mCamera.release();
            orientationEventListener.disable();
            mCamera = null;
            whichCamera = !whichCamera;
        }
    }

    private static List<String> camEffects;

    private static boolean hasFlash(){
        camEffects = p.getSupportedColorEffects();
        final List<String> flashModes = p.getSupportedFlashModes();
        if(flashModes == null){
            return false;
        }
        for(String flashMode:flashModes){
            if(Camera.Parameters.FLASH_MODE_ON.equals(flashMode)){
                return true;
            }
        }
        return false;
    }

    private static int rotation;

    private static boolean whichCamera = true;

    private static Camera.Parameters p;
    private void rotateCamera(){
        if (mCamera != null){
            rotation = this.getWindowManager().getDefaultDisplay().getRotation();
            if (rotation == 0){
                rotation = 90;
            }else if(rotation == 1){
                rotation = 0;
            }else if (rotation == 2){
                rotation = 270;
            }else{
                rotation = 180;
            }
            mCamera.setDisplayOrientation(rotation);
            if (!whichCamera){
                if (rotation == 90){
                    rotation = 270;
                }else if (rotation == 270){
                    rotation = 90;
                }
            }
            p = mCamera.getParameters();
            p.setRotation(rotation);
            mCamera.setParameters(p);
        }
    }
    private static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
        private static SurfaceHolder mHolder;
        private Camera mCamera;

        private CameraPreview(Context context, Camera camera){
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        public void surfaceCreated(SurfaceHolder holder){
            myHolder = holder;
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        public void surfaceDestroyed(SurfaceHolder holder){

        }
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){

        }
    }
}
