package com.miaxis.faceid_yc.utils;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by xu.nan on 2017/2/15.
 */

public class CommonUtil {
    public static String unicode2String(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length / 2; i++) {
            int a = bytes[2 * i + 1];
            if (a < 0) {
                a = a + 256;
            }
            int b = bytes[2 * i];
            if (b < 0) {
                b = b + 256;
            }
            int c = (a << 8) | b;
            sb.append((char) c);
        }
        return sb.toString();
    }

    public static void saveBitmap(Bitmap bitmap) throws Exception {
        File f = new File(Constants.ID_PHOTO_PATH, Constants.ID_PHOTO_NAME);
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream out = new FileOutputStream(f);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();
    }

    public static void ImrotateLevel_raw(byte[] rawBuf, int imgX, int imgY) {
        for(int i = 0; i < imgY; ++i) {
            for(int j = 0; j < imgX >> 1; ++j) {
                byte tmp = rawBuf[i * imgX + j];
                rawBuf[i * imgX + j] = rawBuf[i * imgX + (imgX - 1 - j)];
                rawBuf[i * imgX + (imgX - 1 - j)] = tmp;
            }
        }
    }
}
