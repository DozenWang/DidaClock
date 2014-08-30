
package  com.example.cropimage;


import java.util.List;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

public class MiuiMediaUriCompat {

    private static final String LOG_TAG = "MiuiMediaUriCompat";
    private static final String PATH_DOCUMENT = "document";
    interface MediaUriCompatImpl {
        public Uri getImageContentUri(Uri documentUri);
    }

    static class MediaUriCompatDefaultImpl implements MediaUriCompatImpl {

        @Override
        public Uri getImageContentUri(Uri documentUri) {
            return documentUri;
        }
    }

    static class MediaUriCompatImpl19 extends MediaUriCompatDefaultImpl {

        @Override
        public Uri getImageContentUri(Uri documentUri) {
            // 有可能用户选择的不是图库里的图片而是SD卡上的图片，这个时候不需要转换Uri
            if (documentUri.getPathSegments().contains(PATH_DOCUMENT)) {
                final String docId = getDocumentId(documentUri);
                final String[] split = docId.split(":");
                final String type = split[0];

                long id;
                try {
                    id = Long.parseLong(split[1]);
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, documentUri.toString() + " convert to standard mediaUri failed : ", e);
                    return documentUri;
                }
                if ("image".equals(type)) {
                    documentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    documentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    documentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                documentUri = ContentUris.appendId(documentUri.buildUpon(), id).build();
            }
            return documentUri;
        }
    }

    static final MediaUriCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 19) {
            IMPL = new MediaUriCompatImpl19();
        } else {
            IMPL = new MediaUriCompatDefaultImpl();
        }
    }

    /**
     * Extract the {@link Document#COLUMN_DOCUMENT_ID} from the given URI.
     * MIUIAPP-7080
     * 4.4之前获取的uri是带文件路径的，而4.4返回的却是content://com.android.providers.media.documents/document/image:3951这样的,
     * 没有路径,只有图片编号的uri.这就导致接下来无法根据图片路径来裁剪的步骤了，所以需要用到DocumentsContract类来解析传进来的uri,
     * 又因为DocumentsContract是4.4新添加的类，所以这里只是单独把用到的方法迁移出来。
     */
    public static String getDocumentId(Uri documentUri) {
        final List<String> paths = documentUri.getPathSegments();
        if (paths.size() < 2) {
            throw new IllegalArgumentException("Not a document: " + documentUri);
        }
        if (!PATH_DOCUMENT.equals(paths.get(0))) {
            throw new IllegalArgumentException("Not a document: " + documentUri);
        }
        return paths.get(1);
    }

    public static Uri getImageContentUri(Uri documentUri) {
        return IMPL.getImageContentUri(documentUri);
    }
}
