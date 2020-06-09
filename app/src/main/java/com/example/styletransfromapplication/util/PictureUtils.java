package com.example.styletransfromapplication.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class PictureUtils {
    // 通过Uri读取图片、缩放
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Bitmap loadImage(boolean flag, Context context, Uri imageUri, int newWidth, int newHeight, String orientation){
        ParcelFileDescriptor parcelFileDescriptor = null;
        FileDescriptor fileDescriptor;
        Bitmap bitmap = null;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(imageUri, "r");
            if (parcelFileDescriptor != null && parcelFileDescriptor.getFileDescriptor() != null){
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                // 转化uri为bitmap
                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 获取图片Exif信息
//        assert fileDescriptor != null;
//        try {
//            ExifInterface exif = new ExifInterface(fileDescriptor);
//            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
//            assert orientation != null;
//            Log.d("图片方向", orientation);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        assert bitmap != null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (float)newWidth / width;
        float scaleHeight = (float)newHeight / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 设置图片方向
        matrix.postRotate(Float.parseFloat(orientation));
//        assert orientation != null;
//        if (orientation.equals("6")){
//            matrix.postRotate(90);
//        }else if (orientation.equals("8")){
//            matrix.postRotate(-90);
//        }
        // 得到新的图片
        Bitmap newImageBitmap = null;
        if (flag || (width <= newWidth && height <= newHeight)) {
            newImageBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        }
        if (!flag && (width > newWidth || height > newHeight)){
            if (width > newWidth && height <= newHeight) {
                newImageBitmap = Bitmap.createBitmap(bitmap, (width - newWidth) / 2, 0, newWidth, height);
                newImageBitmap = scale(newImageBitmap, newWidth, newHeight);
            }else if (width <= newWidth && height > newWidth) {
                newImageBitmap = Bitmap.createBitmap(bitmap, 0, (height - newHeight) / 2, width, newHeight);
                newImageBitmap = scale(newImageBitmap, newWidth, newHeight);
            }else {
                newImageBitmap = Bitmap.createBitmap(bitmap, (width - newWidth) / 2, (height - newHeight) / 2,
                        newWidth, newHeight);
            }
        }
        assert newImageBitmap != null;
        Log.d("newWidth", String.valueOf(newImageBitmap.getWidth()));
        Log.d("newHeight", String.valueOf(newImageBitmap.getHeight()));
        if (!bitmap.isRecycled()){
            bitmap.recycle();
        }
        return newImageBitmap;
    }

    // 保存图片到Picture目录
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void saveImage(Context context, String Filename, String Dirname, Image image){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, Filename);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + Dirname);

        ContentResolver resolver = context.getContentResolver();
        Uri insertUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        OutputStream outputStream = null;
        try {
            if (insertUri != null) {
                outputStream = resolver.openOutputStream(insertUri);
            }
            if (outputStream != null){
                outputStream.write(data, 0, data.length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 保存bitmap图片到相册
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void saveImage(Context context, String Filename, String Dirname, Bitmap bitmap){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, Filename);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + Dirname);

        ContentResolver resolver = context.getContentResolver();
        Uri insertUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        // Bitmap转byte数组
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        OutputStream outputStream = null;
        try {
            if (insertUri != null) {
                outputStream = resolver.openOutputStream(insertUri);
            }
            if (outputStream != null){
                outputStream.write(data, 0, data.length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 图片缩放
    public static Bitmap scale(Bitmap bitmap, int newWidth, int newHeight){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (float)newWidth / width;
        float scaleHeight = (float)newHeight / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    // opencv图像处理
    public static Bitmap processImage(Bitmap bitmap, int model){
        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Log.d("图片高", String.valueOf(bitmap.getHeight()));
        Log.d("图片宽", String.valueOf(bitmap.getWidth()));
        Utils.bitmapToMat(bitmap, src);

        switch (model){
            case 5:
                // 灰度化
                Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
                break;
            case 6:
                // 高斯模糊
                Imgproc.GaussianBlur(src, src, new Size(5.0, 5.0), 0.0, 0.0);
                break;
            case 7:
                // 锐化
                Mat kernel = new Mat(3, 3, CvType.CV_16SC1);
                kernel.put(
                      0, 0,
                      0.0, -1.0, 0.0,
                      -1.0, 5.0, -1.0,
                      0.0, -1.0, 0.0
                );
                Imgproc.filter2D(src, src, src.depth(), kernel);
                break;
            case 8:
                // 阈值化
                Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, 0.0);
                Imgproc.cvtColor(src, src, Imgproc.COLOR_GRAY2BGRA);
                break;
            case 9:
                // 腐蚀
                Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5.0, 5.0));
                Imgproc.erode(src, src, erodeKernel);
                break;
            case 10:
                // 膨胀
                Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3.0, 3.0));
                Imgproc.dilate(src, src, dilateKernel);
                break;
            case 11:
                // 单通道均衡
                Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(src, src);
                break;
            case 12:
                // 通道均衡
                ArrayList<Mat> mats = new ArrayList<>();
                Core.split(src, mats);
                for (Mat it: mats){
                    Imgproc.equalizeHist(it, it);
                }
                Core.merge(mats, src);
                break;
            case 13:
                // 像素化
                Mat dst = new Mat(src.rows(), src.cols(), CvType.CV_8UC4);
                int block = 8;
                double[] pixel = new double[4];
                for (int i = 0; i < src.rows(); i += block){
                    for (int j = 0; j < src.cols(); j += block){
                        double blue = 0, green = 0, red = 0;
                        for (int m = i; m < i + block; m++){
                            for (int n = j; n < j + block; n++){
                                blue += src.get(n, m)[0];
                                green += src.get(n, m)[1];
                                red += src.get(n, m)[2];
                            }
                        }
                        pixel[0] = blue / (block * block);
                        pixel[1] = green / (block * block);
                        pixel[2] = red / (block * block);
                        pixel[3] = 255;
                        for (int n = i; n <i+block; ++n) {
                            for (int m = j; m <j + block; ++m) {
                                dst.put(m, n, pixel);
                            }
                        }
                    }
                }
                Log.d("dst宽", String.valueOf(dst.cols()));
                src = dst;
                break;
        }
        Log.d("src宽", String.valueOf(src.cols()));

        Bitmap processBitmap = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, processBitmap);
        src.release();
        return processBitmap;
    }

    // 设置状态栏
    public static void setStatusBar(Activity activity){
        View decorView = activity.getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

}
