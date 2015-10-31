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
        SavePhotoTask savePhotoTask = new SavePhotoTask();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = new File("/sdcard/Pictures/temp.jpg", "temp.jpg");

        //File root = Environment.getExternalStorageDirectory();
        //File image = new File("sdcard/Pictures/timeStamp.jpg");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("/sdcard/Pictures/temp.jpg");
            out.write(content);
            out.close();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        //savePhotoTask.execute(content);
        return image;
    }

    static class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {
            File photo=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "photo.jpg");

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());

                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            return(null);
        }
    }
}
