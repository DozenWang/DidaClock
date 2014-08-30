package com.example.cropimage;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Utilities related to loading/saving contact photos.
 *
 */
public class ContactPhotoUtils {
    private static final String TAG = "ContactPhotoUtils";

    private static final String PHOTO_DATE_FORMAT = "'IMG'_yyyyMMdd_HHmmss";
    private static final String NEW_PHOTO_DIR_PATH =
            Environment.getExternalStorageDirectory() + "/DCIM/Camera";


    /**
     * Generate a new, unique file to be used as an out-of-band communication
     * channel, since hi-res Bitmaps are too big to serialize into a Bundle.
     * This file will be passed to other activities (such as the gallery/camera/cropper/etc.),
     * and read by us once they are finished writing it.
     */
    public static File generateTempPhotoFile(Context context) {
        return new File(pathForCroppedPhoto(context, generateTempPhotoFileName()));
    }

    public static String pathForCroppedPhoto(Context context, String fileName) {
        final File dir = new File(context.getExternalCacheDir() + "/tmp");
        dir.mkdirs();
        final File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }

    public static String pathForNewCameraPhoto(String fileName) {
        final File dir = new File(NEW_PHOTO_DIR_PATH);
        dir.mkdirs();
        final File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }

    public static String generateTempPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(PHOTO_DATE_FORMAT);
        return "ContactPhoto-" + dateFormat.format(date) + ".jpg";
    }

    /**
     * Creates a byte[] containing the PNG-compressed bitmap, or null if
     * something goes wrong.
     */
    public static byte[] compressBitmap(Bitmap bitmap) {
        final int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Unable to serialize photo: " + e.toString());
            return null;
        }
    }
    
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        String fileName = null;
        try {
            if (contentUri.getScheme().toString().equals("content")) {
                String[] proj = { MediaStore.Images.Media.DATA,
                        MediaStore.Files.FileColumns.DATA };
                cursor = context.getContentResolver().query(contentUri, proj,
                        null, null, null);
                if (cursor != null) {
                    int column_index = cursor
                            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToNext()) {
                        fileName = cursor.getString(column_index);
                    }
                }
            } else if (contentUri.getScheme().equals("file")) {
                fileName = contentUri.getEncodedPath();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileName;
    }
}


