package com.remobile.qrcodeLocalImage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Formatter;
import java.util.Hashtable;

import android.util.Log;
import android.util.Base64;


public class RCTQRCodeLocalImage extends ReactContextBaseJavaModule {
    public RCTQRCodeLocalImage(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RCTQRCodeLocalImage";
    }

    @ReactMethod
    public void decode(String path, Callback callback) {
        Log.v("path",path);
        Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8"); // 设置二维码内容的编码
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        options.inJustDecodeBounds = false; // 获取新的大小

        int sampleSize = (int) (options.outHeight / (float) 200);

        if (sampleSize <= 0)
            sampleSize = 1;
        options.inSampleSize = sampleSize;
        Bitmap scanBitmap = null;
        if (path.startsWith("http://")||path.startsWith("https://")) {
            scanBitmap = this.getbitmap(path);
        } else if (path.startsWith("data:image/jpeg;base64,")) {
            String base64data = path.replace("data:image/jpeg;base64,","");
            byte[] imageBytes = Base64.decode(base64data, Base64.DEFAULT);
            scanBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//            callback.invoke("something different happened, YAY!");
        } else {
            scanBitmap = BitmapFactory.decodeFile(path, options);
        }
        if (scanBitmap == null) {
            callback.invoke("cannot load image");
            return;
        }
        int[] intArray = new int[scanBitmap.getWidth()*scanBitmap.getHeight()];
        scanBitmap.getPixels(intArray, 0, scanBitmap.getWidth(), 0, 0, scanBitmap.getWidth(), scanBitmap.getHeight());

        RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap.getWidth(), scanBitmap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source.invert()));
        //source.invert above needs to be checked without invert first
        QRCodeReader reader = new QRCodeReader();
        try {
            Result result = reader.decode(bitmap, hints);
            if (result == null) {
                callback.invoke("image format error");
            } else {


                callback.invoke(null, serializeEventData(result));
            }

        } catch (Exception e) {
            callback.invoke(e.toString());
        }
    }

    private WritableMap serializeEventData(Result result) {
        WritableMap event = Arguments.createMap();
        WritableMap eventOrigin = Arguments.createMap();

        event.putInt("target", 0);
        event.putString("data", result.getText());

        byte[] rawBytes = result.getRawBytes();
        if (rawBytes != null && rawBytes.length > 0) {
            Formatter formatter = new Formatter();
            for (byte b : rawBytes) {
                formatter.format("%02x", b);
            }
            event.putString("rawData", formatter.toString());
            formatter.close();
        }

        event.putString("type", result.getBarcodeFormat().toString());
        WritableArray resultPoints = Arguments.createArray();
        ResultPoint[] points = result.getResultPoints();
        for (ResultPoint point: points) {
            if(point!=null) {
                WritableMap newPoint = Arguments.createMap();
                newPoint.putString("x", String.valueOf(point.getX()));
                newPoint.putString("y", String.valueOf(point.getY()));
                resultPoints.pushMap(newPoint);
            }
        }

        eventOrigin.putArray("origin", resultPoints);
        eventOrigin.putInt("height", 0);
        eventOrigin.putInt("width", 0);
        event.putMap("bounds", eventOrigin);
        return event;
    }

    public static Bitmap getbitmap(String imageUri) {
        Bitmap bitmap = null;
        try {
            URL myFileUrl = new URL(imageUri);
            HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            bitmap = null;
        } catch (IOException e) {
            e.printStackTrace();
            bitmap = null;
        }
        return bitmap;
    }
}
