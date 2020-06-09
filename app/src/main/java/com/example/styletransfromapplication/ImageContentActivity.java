package com.example.styletransfromapplication;

import com.example.styletransfromapplication.util.PictureUtils;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.styletransfromapplication.util.ItemClickListener;
import com.example.styletransfromapplication.util.LiteInterpreter;
import com.example.styletransfromapplication.util.RecycleViewAdapter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.tensorflow.lite.Interpreter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class ImageContentActivity extends Activity {

    ProgressBar progressBar;
    boolean transformed;
    ImageView imageView;
    RecyclerView recyclerView;
    ImageButton flip_image;
    ImageButton rotate_degree;
    ImageButton save_image;
    String[] style = {"rain_princess", "starry_night", "candy", "udnie", "poppy_field"};
    String[] cv_style = {"gray", "gaussianblur", "sharpen", "cvt", "dilate", "erode", "grayeh", "coloreh", "pixel"};
    Drawable[] drawables = new Drawable[14];
    String[] texts = {"rain_princess", "starry_night", "candy", "udnie", "poppy_field", "灰度化", "高斯模糊", "锐化",
                       "阈值化", "腐蚀", "膨胀", "单通道均衡", "多通道均衡", "像素化"};
    List<String> extra;
    Bitmap imageBitmap;
    Bitmap newest_imageBitmap;
    int newWidth = 512;
    int newHeight = 512;
    float[][][][] inputValues;
    float[][][][] outputValues = new float[1][newHeight][newWidth][3];
    Interpreter interpreter;
    HashMap<Integer, String> style_map = new HashMap<>();
    Handler handler = new Handler();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.capture_image);
        // 设置状态栏
        PictureUtils.setStatusBar(this);

        imageView = findViewById(R.id.capture_image);
        recyclerView = findViewById(R.id.recycle_view);
        flip_image = findViewById(R.id.flip_image);
        rotate_degree = findViewById(R.id.rotate_degree);
        save_image = findViewById(R.id.save_image);
        progressBar = findViewById(R.id.progress_bar);

        Intent intent = getIntent();
        extra = intent.getStringArrayListExtra("extra");
        assert extra != null;
        Log.d("imageFilename", String.valueOf(extra));
//        imageView.setImageURI(Uri.parse(imagePath));
        // 获取文件名
        String imageFilename = extra.get(0);
        // 获取图片矫正角度或uri
        String orientation = extra.get(1);
        if (imageFilename.equals("1970")){
            // 从图库获取图片
            imageBitmap = PictureUtils.loadImage(false, this, Uri.parse(orientation), newWidth, newHeight, "0");
        } else {
            // 拍照获取图片
            imageBitmap = PictureUtils.loadImage(true, this, getMediaImage(this, imageFilename + ".jpg"), newWidth, newHeight, orientation);
        }
        // 显示缩放图片
        imageView.setImageBitmap(imageBitmap);
        newest_imageBitmap = imageBitmap;
        // 渲染RecyclerView
        for (int i = 0; i < 5; i++) {
            drawables[i] = this.getResources().getDrawable(getResourcesId(style[i]), null);
            style_map.put(i, style[i] + ".tflite");
        }
        for (int i = 0; i < cv_style.length; i++){
            drawables[i + 5] = this.getResources().getDrawable(getResourcesId(cv_style[i]), null);
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        RecycleViewAdapter adapter = new RecycleViewAdapter(drawables, texts, getApplicationContext());
        adapter.setOnItemClickListener(new ItemClickListener() {
            @Override
            public void OnItemClick(View v, final int position) {
                Log.d("位置", String.valueOf(position));
                // 显示进度条
                progressBar.setVisibility(View.VISIBLE);
                if (position <= 4) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            transformed = true;
                            // run模型
                            runNet(style_map.get(position));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });

                        }
                    }).start();
                }
                if (position > 4) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // run OpenCv
                            newest_imageBitmap = PictureUtils.processImage(imageBitmap, position);
                            imageView.setImageBitmap(newest_imageBitmap);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        }
                    }).start();

                }
            }
        });
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 加载OpenCv库
        if (!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback);
        }else {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        // 图片翻转
        flip_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newest_imageBitmap = flipImage(newest_imageBitmap);
                imageView.setImageBitmap(newest_imageBitmap);
            }
        });

        // 图片旋转
        rotate_degree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (transformed){
//                    transImage = rotateImage(transImage, -90);
//                    imageView.setImageBitmap(transImage);
//                }else if (cv_transformed){
//                    cvBitmap = rotateImage(cvBitmap, -90);
//                    imageView.setImageBitmap(cvBitmap);
//                }else {
//                    imageBitmap = rotateImage(imageBitmap, -90);
//                    imageView.setImageBitmap(imageBitmap);
//                }
                newest_imageBitmap = rotateImage(newest_imageBitmap, -90);
                imageView.setImageBitmap(newest_imageBitmap);
            }
        });
        // 图片保存
        save_image.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmsss", Locale.getDefault()).format(new Date());
//                if (transformed){
////                    // 保存风格转化图片
////                    PictureUtils.saveImage(ImageContentActivity.this, timeStamp, getResources().getString(R.string.app_name), transImage);
////                } else if (cv_transformed){
////                    PictureUtils.saveImage(ImageContentActivity.this, timeStamp, getResources().getString(R.string.app_name), cvBitmap);
////                }else {
////                    PictureUtils.saveImage(ImageContentActivity.this, timeStamp, getResources().getString(R.string.app_name), imageBitmap);
////                }
                PictureUtils.saveImage(ImageContentActivity.this, timeStamp, getResources().getString(R.string.app_name), newest_imageBitmap);
                Toast.makeText(ImageContentActivity.this, timeStamp + "已保存", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // OpenCv加载回调
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            if (status == LoaderCallbackInterface.SUCCESS){
                Log.d("status", "OpenCv loaded successfully");
            }
        }
    };

    // 翻转图片
    public Bitmap flipImage(Bitmap bitmap){
        if (bitmap == null){
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1);
        Bitmap newRM = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
        bitmap.recycle();
        return newRM;
    }

    // 旋转图片
    public Bitmap rotateImage(Bitmap bitmap, float rotate){
        if (bitmap == null){
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(rotate);
        Bitmap newRM = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
        bitmap.recycle();
        return newRM;
    }

    // 输入图像到网络
    public void runNet(String tflite_model){
        // 获取模型Interpreter
        interpreter = new LiteInterpreter(getApplicationContext(), "model/" + tflite_model).getInterpreter();
        Log.d("解释器", String.valueOf(interpreter));
        Log.d("输入", String.valueOf(interpreter.getInputTensorCount()));
        Log.d("输出", String.valueOf(interpreter.getOutputTensorCount()));

        // 转换bitmap数据格式,输入到网络
        inputValues = getScaledMatrix(imageBitmap, newWidth, newHeight);
        Log.d("input", Arrays.toString(inputValues));
        Log.d("inputcount", String.valueOf(inputValues.length));
        interpreter.run(inputValues, outputValues);
        // 修正过大或过小数值
        for (int i = 0; i < newHeight; ++i){
            for (int j = 0; j < newWidth; ++j){
                for (int k = 0; k < 3; ++k){
                    if (outputValues[0][i][j][k] > 255) {
                        outputValues[0][i][j][k] = 255;
                    }
                    if (outputValues[0][i][j][k] < 0){
                        outputValues[0][i][j][k] = 0;
                    }
                }
            }
        }
        newest_imageBitmap = getTransImage(outputValues, newWidth, newHeight);
        imageView.setImageBitmap(newest_imageBitmap);
    }


    // Bitmap转tensorflow lite数据格式
    public float[][][][] getScaledMatrix(Bitmap bitmap, int newWidth, int newHeight){
        float[] floatValues = new float[newWidth * newHeight * 3];
        int[] intValues = new int[newWidth * newHeight];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, newWidth, newHeight);
        for (int i = 0; i < intValues.length; ++i){
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF);
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF);
            floatValues[i * 3 + 2] = (val & 0xFF);
        }

        // 变换数组矩阵的秩
        float[][][][] floats = new float[1][newHeight][newWidth][3];
        for (int i = 0; i < newHeight; ++i){
            for (int j = 0; j < newWidth; ++j){
                for (int k = 0; k < 3; ++k){
                    floats[0][i][j][k] = floatValues[3 * 512 * i + 3 * j + k] / 255.0f;
                }
            }
        }
        return floats;
    }

    //Bitmap像素组合
    public Bitmap getTransImage(float[][][][] values,  int newWidth, int newHeight){
        int[] intValues = new int[newHeight * newWidth];
        for (int i = 0; i < newHeight; ++i){
            for (int j = 0; j < newWidth; ++j){
                intValues[512 * i + j] = 0xFF000000 | (((int)(values[0][i][j][0])) << 16)
                                                    | (((int)(values[0][i][j][1])) << 8)
                                                    | (((int)(values[0][i][j][2])));
            }
        }
        // 存入Bitmap
        Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        return bitmap;
    }

    // 从Pictures目录读取文件
    public Uri getMediaImage(Context context, String imageFilename){
        Uri uri = null;
        String[] projection = {MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Thumbnails.DATA
        };
        ContentResolver resolver = context.getContentResolver();
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "='" + imageFilename + "'";
        Cursor cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);
        if (cursor != null && cursor.moveToFirst()){
            int mediaId = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
//            filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)); //获取图片路径
//            Log.d("相册路径", filePath);
            uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + mediaId ); //
            Log.d("图片uri", String.valueOf(uri));
        }
        if (uri != null){
            cursor.close();
        }
        return uri;
    }

    // 获取drawable图片资源
    public int getResourcesId(String image){
        return getResources().getIdentifier(image, "drawable", getBaseContext().getPackageName());
    }

}
