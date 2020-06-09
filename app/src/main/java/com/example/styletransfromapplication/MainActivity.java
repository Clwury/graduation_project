package com.example.styletransfromapplication;

import com.example.styletransfromapplication.util.PictureUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity {

    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE};
    List<String> mPermissions = new ArrayList<>();
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    AlertDialog mPermissionDialog;

    CameraManager cameraManager;
    // 前后摄像头
    String frontCameraId;
    String backCameraId;
    CameraCharacteristics frontCameraCharacteristics;
    CameraCharacteristics backCameraCharacteristics;
    CameraDevice cameraDevice;
    CameraCaptureSession cameraCaptureSession;

    HandlerThread backgroundThread;
    Handler backgroundHandler;

    CaptureRequest.Builder captureRequestBuilder;
    CaptureRequest captureRequest;
    CaptureResult captureResult;
    CameraDevice.StateCallback CameraeStateCallback;

    TextureView view_finder;
    ImageButton capture_picture;
    ImageButton flip_camera;
    ImageButton landscape;
    // 屏幕方向
    int deviceOrientation;
    TextureView.SurfaceTextureListener surfaceTextureListener;
    SurfaceTexture previewSurfaceTexture;
    Surface previewSurface;
    Surface captureSurface;
    Size previewSize;
    int windowHeight;
    int windowWidth;
    File galleryFolder;
    ImageReader imageReader;
//    File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 设置状态栏
        PictureUtils.setStatusBar(this);

        // 获取屏幕尺寸
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        windowHeight = size.y;
        windowWidth = size.x;
        Log.d("高", String.valueOf(windowHeight));
        Log.d("宽", String.valueOf(windowWidth));

        view_finder = findViewById(R.id.view_finder);
        capture_picture = findViewById(R.id.capture_picture);
        flip_camera = findViewById(R.id.flip_camera);
        landscape = findViewById(R.id.landscape);

        // 权限申请
        initPermission();

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                previewSurfaceTexture = surface;
                initCamera();
                openCamera(backCameraId);
                Log.d("aaaaa", "////////////////////");
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };

        view_finder.setSurfaceTextureListener(surfaceTextureListener);

        // 管理CameraDevice
        CameraeStateCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                // 初始化surface
                previewSurfaceTexture = view_finder.getSurfaceTexture();
                previewSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight()); //设置显示区域大小
                previewSurface = new Surface(previewSurfaceTexture);

                imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
                captureSurface = imageReader.getSurface();
                //创建CameraPreviewSession
                try {
                    cameraDevice.createCaptureSession(Arrays.asList(previewSurface, captureSurface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            // 开启预览
                            previewCamera();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                // 开启预览
//                previewCamera();

            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                camera.close();
                cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                camera.close();
                cameraDevice = null;
            }

        };
        // 初始化相机信息
//        initCamera();
    }


    @Override
    protected void onResume() {
        super.onResume();
        openBackGroundThread();
        if (view_finder.isAvailable()){
            initCamera();
            openCamera(backCameraId);
        }else {
            view_finder.setSurfaceTextureListener(surfaceTextureListener);
        }
        // 获取设备方向
        OrientationEventListener orientationEventListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                MainActivity.this.deviceOrientation = orientation;
            }
        };
        if (orientationEventListener.canDetectOrientation()){
            orientationEventListener.enable();
        }
        // 翻转镜头
        flip_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraDevice.getId().equals(backCameraId)){
                    closeCamera();
                    openCamera(frontCameraId);
                } else {
                    closeCamera();
                    openCamera(backCameraId);
                }
            }
        });
        // 拍照
        capture_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        // 调用系统图库
        landscape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 0);
            }
        });

    }
    // 接收返回图片
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0){
            if (data != null){
                Uri uri = data.getData();
                Log.d("相册名", String.valueOf(uri));
                // 跳转到ImageContentActivity
                Intent intent = new Intent(MainActivity.this, ImageContentActivity.class);
                ArrayList<String> extra = new ArrayList<>();
                extra.add("1970");
                assert uri != null;
                extra.add(uri.toString());
                intent.putStringArrayListExtra("extra", extra);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeBackGroundThread();
        closeCamera();
        surfaceTextureListener = null;
    }

    // 关闭相机
    private void closeCamera(){
        if (cameraCaptureSession != null){
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    // 关闭后台线程
    private void closeBackGroundThread(){
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    // 启动后台线程
    private void openBackGroundThread(){
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    // 动态权限申请
    private void initPermission() {
        mPermissions.clear();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                    Context context = getApplicationContext();
                    CharSequence text = "该应用需要使用权限";
                    int duration = Toast.LENGTH_LONG;

                    Toast.makeText(context, text, duration).show();
                }
                mPermissions.add(permission);
            }
        }
        // 申请权限
        if (mPermissions.size() > 0) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean permissionDismiss = false;
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i : grantResults) {
                if (i == -1) {
                    permissionDismiss = true;
                    break;
                }
            }
            if (permissionDismiss) {
                showPermissionDialog();
            }
        }
    }

    // 禁用权限设置对话框
    private void showPermissionDialog() {
        if (mPermissionDialog == null) {
            mPermissionDialog = new AlertDialog.Builder(this)
                    .setMessage("已禁用权限，请手动授予")
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();

                            Uri packageURI = Uri.parse("package:" + MainActivity.this.getPackageName());
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //关闭页面或者做其他操作
                            cancelPermissionDialog();
                            MainActivity.this.finish();
                        }
                    })
                    .create();
        }
        mPermissionDialog.show();
    }

    //取消设置权限,退出程序
    private void cancelPermissionDialog() {
        mPermissionDialog.cancel();
    }

    // 初始化相机信息
    public void initCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        assert cameraManager != null;
        String[] cameraIdList = new String[0];
        try {
            cameraIdList = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        for (String cameraId : cameraIdList) {
            Log.d("CameraID", cameraId);
            CameraCharacteristics cameraCharacteristics = null;
            try {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            // 检测相机对Camera的支持
            assert cameraCharacteristics != null;
            Integer hard_level = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (hard_level == null) {
                Log.d("HARD_LEVEL", "can not get INFO_SUPPORTED_HARDWARE_LEVEL");
            }
            assert hard_level != null;
            switch (hard_level) {
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    Log.d("HARD_LEVEL", "hardware supported level:LEVEL_LEGACY");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    Log.d("HARD_LEVEL", "hardware supported level:LEVEL_LIMITED");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    Log.d("HARD_LEVEL", "hardware supported level:LEVEL_FULL");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                    Log.d("HARD_LEVEL", "hardware supported level:LEVEL_3");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                    Log.d("HARD_LEVEL", "hardware supported level:LEVEL_EXTERNAL");
                    break;
            }

            // 找出前置和后置摄像头
            Integer lens_facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            assert lens_facing != null;
            if (lens_facing == CameraCharacteristics.LENS_FACING_FRONT) {
                frontCameraId = cameraId;
                frontCameraCharacteristics = cameraCharacteristics;
                getConfigSize(frontCameraCharacteristics, "前置镜头");
            } else if (lens_facing == CameraCharacteristics.LENS_FACING_BACK) {
                backCameraId = cameraId;
                backCameraCharacteristics = cameraCharacteristics;
                // 后置镜头预览区域大小
                getConfigSize(backCameraCharacteristics, "后置镜头");
            }

        }
    }

    //开启相机
    public void openCamera(String cameraId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            Toast.makeText(this, "应用没有相机权限", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            cameraManager.openCamera(cameraId, CameraeStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    //相机预览
    public void previewCamera(){

//        previewSurfaceTexture = view_finder.getSurfaceTexture();
//        previewSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight()); //设置显示区域大小
//        previewSurface = new Surface(previewSurfaceTexture);
        // 创建CaptureRequest
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequest = captureRequestBuilder.build();
            cameraCaptureSession.setRepeatingRequest(captureRequest, CameraCaptureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        // 创建CameraCaptureSession
//        Handler mainHandler = new Handler(Looper.getMainLooper());
//        cameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
//            @Override
//            public void onConfigured(@NonNull CameraCaptureSession session) {
//                cameraCaptureSession = session;
//                captureRequest = captureRequestBuilder.build();
//                try {
//                    cameraCaptureSession.setRepeatingRequest(captureRequest, CameraCaptureCallback, backgroundHandler);
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//
//            }
//        }, backgroundHandler);
        // 关闭预览
//        cameraCaptureSession.stopRepeating();
    }


    private final CameraCaptureSession.CaptureCallback CameraCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

        }
    };

    // 创建图片目录
    public void createImageGallery(){
        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()){
            boolean created = galleryFolder.mkdirs();
            if (!created){
                Toast.makeText(this, galleryFolder.getPath() + "文件目录创建失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 创建图片文件
    public File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmsss", Locale.getDefault()).format(new Date());
        String imageFilename = getResources().getString(R.string.app_name) + timeStamp;
        Toast.makeText(this, imageFilename + ".jpg已创建", Toast.LENGTH_LONG).show();
        return File.createTempFile(imageFilename, ".jpg", galleryFolder);
    }

    // 拍照
    private void takePhoto() {
        // 监听拍照
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                // 保存图片
//                saveImage(image);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmsss", Locale.getDefault()).format(new Date());
                PictureUtils.saveImage(MainActivity.this, timeStamp, getResources().getString(R.string.app_name), image);
                Log.d("imageFilename", timeStamp);
                // 跳转到ImageContentActivity
                Intent intent = new Intent(MainActivity.this, ImageContentActivity.class);
                ArrayList<String> extra = new ArrayList<>();
                extra.add(timeStamp);
                extra.add(String.valueOf(getJpegOrientation()));
                intent.putStringArrayListExtra("extra", extra);
                startActivity(intent);
            }
        }, backgroundHandler);

        // 配置拍照信息
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        captureRequestBuilder.addTarget(previewSurface);
        captureRequestBuilder.addTarget(captureSurface);
        // 自动对焦
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 自动曝光
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // 校正图片方向
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation());
        captureRequest = captureRequestBuilder.build();
        // 开始拍照
        try {
            // 停止预览
            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    // 拍照完成，预览
                     previewCamera();
                    // 回调拍照结果
                    captureResult = result;
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        // 保存图片的另一种方式
//        createImageGallery();
//        FileOutputStream outputPhoto = null;
//        try {
//            outputPhoto = new FileOutputStream(createImageFile(galleryFolder));
//            view_finder.getBitmap(512, 512).compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            previewCamera();
//        }
    }

    // 保存图片
    /*
    public void saveImage(Image image){
        createImageGallery();
        try {
            imageFile = createImageFile(galleryFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(imageFile);
            fileOutputStream.write(data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null){
                try {
                    fileOutputStream.close();
                    fileOutputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }*/

    // 获取镜头输入图片大小
    private void getConfigSize(CameraCharacteristics cameraCharacteristics, String len_face) {
        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert streamConfigurationMap != null;
        Size[] size = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        for (Size s : size) {
            Log.d(len_face + "宽", String.valueOf(s.getWidth()));
            Log.d(len_face + "高", String.valueOf(s.getHeight()));
        }
        previewSize = getOptimalSize(size, windowWidth, windowWidth);
    }

    // 得到合适的屏幕预览区域
    private Size getOptimalSize(Size[] outputSizes, int width, int height){
        double preferredRatio = height / (double) width;
        Size currentOptimalSize = outputSizes[0];
        double currentOptimalRatio = currentOptimalSize.getWidth() / (double) currentOptimalSize.getHeight();
        for (Size currentSize : outputSizes) {
            double currentRatio = currentSize.getWidth() / (double) currentSize.getHeight();
            if (Math.abs(preferredRatio - currentRatio) <=
                    Math.abs(preferredRatio - currentOptimalRatio)) {
                currentOptimalSize = currentSize;
                currentOptimalRatio = currentRatio;
            }
        }
        return currentOptimalSize;
    }

    // 矫正图片输出方向
    private int getJpegOrientation(){

        int sensorOrientation = backCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // 修正设备方向为90整数倍
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
        if (cameraDevice.getId().equals(frontCameraId)){
            deviceOrientation = -deviceOrientation;
            sensorOrientation = frontCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        }
        Log.d("传感器方向", String.valueOf(sensorOrientation));
        Log.d("设备方向", String.valueOf(deviceOrientation));
        Log.d("矫正方向", String.valueOf((sensorOrientation + deviceOrientation + 360) % 360));

        return (sensorOrientation + deviceOrientation + 360) % 360;

    }


}
