/**
 * Created by Albert on 31.10.15.
 */
package com.studio.arm.wink;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class FaceFile {
    /**
     * Save binary data to public image dirr
     *
     * @param content data to save
     * @param extension file's extension
     * @return newly created file
     * @throws IOException
     */
    public static File savePicture(byte[] content, String extension) throws IOException {
        // Create an image file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = new File(dir, timeStamp + "." + extension);

        //File root = Environment.getExternalStorageDirectory();
        //File image = new File("sdcard/Pictures/timeStamp.jpg");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(image);
            out.write(content);
            out.close();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return image;
    }

}
