package com.example.styletransfromapplication.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class LiteInterpreter {

    private AssetManager assetManager;
    private Interpreter interpreter;

    public LiteInterpreter(Context context, String model){
        this.assetManager = context.getAssets();
        GpuDelegate delegate = new GpuDelegate();
        Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);

        try {
            this.interpreter = new Interpreter(loadModelFile(model), options);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    // 加载tflite模型
    private ByteBuffer loadModelFile(String model) throws IOException {
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(model);
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}
