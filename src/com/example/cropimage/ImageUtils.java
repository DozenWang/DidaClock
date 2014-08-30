package com.example.cropimage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

/**
 * @hide
 */
public class ImageUtils {

    // @MiuiLiteHook add start
    public static void setEnable(ImageView img, boolean enable) {
        int enableAlpha = 255;
        int disableAlpha = (int) (enableAlpha * 0.3f);
        setAlpha(img, enable ? enableAlpha : disableAlpha);
    }

    public static void setAlpha(ImageView img, int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException();
        }
        // Fixbug[MIUIAPP-5395]: 避免alpha drawable的循环使用引起显示错误
        final Drawable drawable = img.getDrawable();
        if (drawable != null) {
            drawable.setAlpha(alpha);
        }
    }
    // @MiuiLiteHook add end

    public static BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inDither = false;
        opt.inJustDecodeBounds = false;
        opt.inSampleSize = 1;
        opt.inScaled = false;
        return opt;
    }

    public static int computeSampleSize(InputStreamLoader streamLoader, int pixelSize) {
        int roundedSize = 1;
        if (pixelSize > 0) {
            BitmapFactory.Options options = getBitmapSize(streamLoader);
            double size = Math.sqrt((double)options.outWidth * options.outHeight / pixelSize);
            while (roundedSize * 2 <= size) {
                roundedSize <<= 1;
            }
        }
        return roundedSize;
    }

    public static final BitmapFactory.Options getBitmapSize(InputStreamLoader streamLoader) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(streamLoader.get(), null, options);
        }
        catch (Exception e) {
        } finally {
            streamLoader.close();
        }
        return options;
    }

    public static int getBitmapSize(Bitmap bitmap) {
        if (bitmap == null) {
            throw new NullPointerException("bitmap cannot be null !!");
        }

        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public static final BitmapFactory.Options getBitmapSize(String filePath) {
        return getBitmapSize(new InputStreamLoader(filePath));
    }

    /**
     * @param pixelSize: 若 <= 0, 则首先尝试解码原始图片大小
     */
    public static final Bitmap getBitmap(InputStreamLoader streamLoader, int pixelSize) {
        BitmapFactory.Options options = getDefaultOptions();
        options.inSampleSize = computeSampleSize(streamLoader, pixelSize);

        // Decode bufferedInput to a bitmap.
        Bitmap bitmap = null;
        int retry = 0;
        while (retry++ < 3) { // 如果内存不够，则加大sample size重新读取，超过3次后退出，返回null
            try {
                // Get the input stream again for decoding it to a bitmap.
                bitmap = BitmapFactory.decodeStream(streamLoader.get(), null, options);
                break;
            } catch (OutOfMemoryError ex) {
                System.gc();
                options.inSampleSize *= 2;
            } catch (Exception ex) {
                break;
            } finally {
                streamLoader.close();
            }
        }

        return bitmap;
    }

    public static Bitmap getBitmap(InputStreamLoader streamLoader, int destWidth, int destHeight) {
        final int PIXEL_FACTOR_FOR_COMPUTING_SAMPLE_SIZE = 2;
        int pixelSize = destWidth * destHeight * PIXEL_FACTOR_FOR_COMPUTING_SAMPLE_SIZE;
        if (destWidth <= 0 || destHeight <= 0) {
            pixelSize = -1;
        }
        Bitmap destBmp = getBitmap(streamLoader, pixelSize);
        // @MiuiLiteHook.CHANGE_CODE [bugfix:MIUIAPP-5631]
        if (pixelSize > 0 && destBmp != null) {
            destBmp = scaleBitmapToDesire(destBmp, destWidth, destHeight, true);
        }
        return destBmp;
    }

    /**
     * 1. destWidth或destHeight 小于等于0，则解码原始图片大小
     * 2. reusedBitmap：用户提供的可被重用的bitmap
     */
    public static Bitmap getBitmap(InputStreamLoader streamLoader, int destWidth, int destHeight, Bitmap reusedBitmap) {
        Bitmap srcBitmap = null;
        if (reusedBitmap != null && !reusedBitmap.isRecycled()) {
            BitmapFactory.Options sizeOp = getBitmapSize(streamLoader);
            if (sizeOp.outWidth == reusedBitmap.getWidth() && sizeOp.outHeight == reusedBitmap.getHeight()) {
                try {
                    BitmapFactory.Options op = getDefaultOptions();
                    op.inBitmap = reusedBitmap;
                    op.inSampleSize = 1;
                    srcBitmap = BitmapFactory.decodeStream(streamLoader.get(), null, op);
                } catch (Exception e) {
                } finally {
                    streamLoader.close();
                }
            }
            if (srcBitmap == null) {
                reusedBitmap.recycle();
            }
        }

        Bitmap destBitmap = srcBitmap;
        if (destBitmap != null) {
            if (destWidth > 0 && destHeight > 0) {
                destBitmap = scaleBitmapToDesire(destBitmap, destWidth, destHeight, true);
            }
        } else {
            destBitmap = getBitmap(streamLoader, destWidth, destHeight);
        }

        return destBitmap;
    }

    public static Bitmap scaleBitmapToDesire(Bitmap srcBmp, int destWidth, int destHeight, boolean recycleSrcBmp) {
        CropOption cOpt = new CropOption();
        cOpt.recycleSrcBmp = recycleSrcBmp;
        return scaleBitmapToDesire(srcBmp, destWidth, destHeight, cOpt);
    }

    /**
     * @deprecated Use {@link #scaleBitmapToDesire(Bitmap, int, int, CropOption)} instead.
     */
    @Deprecated
    public static Bitmap scaleBitmapToDesire(Bitmap srcBmp, int destWidth, int destHeight, CropOption cOpt, boolean recycleSrcBmp) {
        cOpt.recycleSrcBmp = recycleSrcBmp;
        return scaleBitmapToDesire(srcBmp, destWidth, destHeight, cOpt);
    }

    public static Bitmap scaleBitmapToDesire(Bitmap srcBmp, int destWidth, int destHeight, CropOption cOpt) {
        if (cOpt == null) {
            cOpt = new CropOption();
        }

        Bitmap destBmp = null;
        try {
            int srcWidth = srcBmp.getWidth();
            int srcHeight = srcBmp.getHeight();

            if (srcWidth == destWidth && srcHeight == destHeight && cOpt.rx <= 0
                    && cOpt.ry <= 0 && cOpt.borderWidth <= 0) {
                destBmp = srcBmp;
            } else {
                Config config = Config.ARGB_8888;
                if (srcBmp.getConfig() != null) {
                    config = srcBmp.getConfig();
                }
                destBmp = Bitmap.createBitmap(destWidth, destHeight, config);
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {

            Config config = Config.RGB_565;
            Log.e("ImageUtils",
                    "OOM while creating a bitmap!-----change config to RGB_565 , try again",
                    e);
            destBmp = Bitmap.createBitmap(destWidth, destHeight, config);

        }
        cropBitmapToAnother(srcBmp, destBmp, cOpt);

        return destBmp;
    }

    /**
     * 中心对齐，将源图片绘制到目的图片上，且可以根据cropOption的参数来增加圆角和描边
     */
    public static boolean cropBitmapToAnother(Bitmap srcBmp, Bitmap destBmp, boolean recycleSrcBmp) {
        CropOption cOpt = new CropOption();
        cOpt.recycleSrcBmp = recycleSrcBmp;
        return cropBitmapToAnother(srcBmp, destBmp, cOpt);
    }

    public static boolean cropBitmapToAnother(Bitmap srcBmp, Bitmap destBmp, CropOption cOpt) {
        if (srcBmp == null || destBmp == null || srcBmp == destBmp) {
            return false;
        }

        if (cOpt == null) {
            cOpt = new CropOption();
        }

        Rect srcDrawingArea = cOpt.srcBmpDrawingArea;
        if (srcDrawingArea == null) {
            srcDrawingArea = new Rect(0, 0, srcBmp.getWidth(), srcBmp.getHeight());
        }
        int srcLeft = between(0, srcBmp.getWidth() - 1, srcDrawingArea.left);
        int srcRight = between(srcLeft, srcBmp.getWidth(), srcDrawingArea.right);
        int srcTop = between(0, srcBmp.getHeight() - 1, srcDrawingArea.top);
        int srcBottom = between(srcTop, srcBmp.getHeight(), srcDrawingArea.bottom);
        int srcWidth = srcRight - srcLeft;
        int srcHeight = srcBottom - srcTop;
        int destWidth = destBmp.getWidth();
        int destHeight = destBmp.getHeight();

        cOpt.borderWidth = between(0, Math.min(destWidth, destHeight) / 2, cOpt.borderWidth);
        cOpt.rx = between(0, destWidth / 2, cOpt.rx);
        cOpt.ry = between(0, destHeight / 2, cOpt.ry);

        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);

        Canvas canvas = new Canvas(destBmp);
        canvas.drawARGB(0, 0, 0, 0);

        if (cOpt.rx - cOpt.borderWidth > 0 && cOpt.ry - cOpt.borderWidth > 0) {
            canvas.drawRoundRect(new RectF(cOpt.borderWidth, cOpt.borderWidth, destWidth - cOpt.borderWidth,
                    destHeight - cOpt.borderWidth), cOpt.rx - cOpt.borderWidth, cOpt.ry - cOpt.borderWidth, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        }

        int visibleWidth = destWidth - 2 * cOpt.borderWidth;
        int visibleHeight = destHeight - 2 * cOpt.borderWidth;
        float ratio = Math.min(1.0f * srcWidth / visibleWidth, 1.0f * srcHeight / visibleHeight);
        int srcHorPadding = (int)((srcWidth - visibleWidth * ratio) / 2);
        int srcVerPadding = (int)((srcHeight - visibleHeight * ratio) / 2);
        Rect src = new Rect(srcLeft + srcHorPadding, srcTop + srcVerPadding,
                srcRight - srcHorPadding, srcBottom - srcVerPadding);
        Rect dst = new Rect(cOpt.borderWidth, cOpt.borderWidth, destWidth - cOpt.borderWidth, destHeight - cOpt.borderWidth);
        canvas.drawBitmap(srcBmp, src, dst, paint);

        if (cOpt.borderWidth > 0 && cOpt.borderColor >>> 24 != 0) {
            paint.setColor(cOpt.borderColor);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            canvas.drawRoundRect(new RectF(0, 0, destWidth, destHeight), cOpt.rx, cOpt.ry, paint);
        }

        if (cOpt.recycleSrcBmp) {
            srcBmp.recycle();
        }
        return true;
    }

    private static int between(int minValue, int maxValue, int value) {
        return Math.min(maxValue, Math.max(minValue, value));
    }

    public static boolean saveBitmapToLocal(InputStreamLoader streamLoader, String path, int destWidth, int destHeight) {
        if (streamLoader == null || path == null || destWidth < 1 || destHeight < 1) {
            return false;
        }

        boolean result = false;
        BitmapFactory.Options options = getBitmapSize(streamLoader);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return result;
        }

        if (options.outWidth == destWidth && options.outHeight == destHeight) {
            result = saveToFile(streamLoader, path);
        } else {
            Bitmap destBmp = getBitmap(streamLoader, destWidth, destHeight);
            if (destBmp != null) {
                result = saveToFile(destBmp, path, isPngFormat(streamLoader));
                destBmp.recycle();
            }
        }
        return result;
    }

    public static boolean saveToFile(Bitmap bitmap, String path) {
        return saveToFile(bitmap, path, false);
    }

    public static boolean saveToFile(Bitmap bitmap, String path, boolean saveToPng) {
        try {
            if (bitmap != null) {
                FileOutputStream outputStream = new FileOutputStream(path);
                bitmap.compress(saveToPng ? CompressFormat.PNG : CompressFormat.JPEG, 100, outputStream);
                outputStream.close();
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static boolean saveToFile(InputStreamLoader streamLoader, String path) {
        boolean result = false;
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            InputStream inputStream = streamLoader.get();
            copy(inputStream, outputStream);
            outputStream.close();
            streamLoader.close();
            result = true;
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * Copies all of the bytes from {@code in} to {@code out}. Neither stream is closed.
     * Returns the total number of bytes transferred.
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            total += c;
            out.write(buffer, 0, c);
        }
        return total;
    }

    private static byte[] PNG_HEAD_FORMAT = new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static boolean isPngFormat(InputStreamLoader streamLoader) {
        boolean ret = false;
        try {
            InputStream is = streamLoader.get();
            byte head[] = new byte[PNG_HEAD_FORMAT.length];
            int n = is.read(head);
            if (n >= head.length) {
                ret = isPngFormat(head);
            }
        } catch (Exception e) {
        } finally {
            if (streamLoader != null) {
                streamLoader.close();
            }
        }
        return ret;
    }

    public static boolean isPngFormat(byte pngHead[]) {
        if (pngHead == null || pngHead.length < PNG_HEAD_FORMAT.length) {
            return false;
        }
        for(int i = 0; i<PNG_HEAD_FORMAT.length; ++i) {
            if (pngHead[i] != PNG_HEAD_FORMAT[i]) {
                return false;
            }
        }
        return true;
    }

    //@MiuiLiteHook.DELETE_START  此段代码与com.android.launcher2.jni.ImageUtils重复，某些机型loadLibrary会失败
//    public static void fastBlur(Bitmap bmpIn, Bitmap bmpOut, int radius) {
//        native_fastBlur(bmpIn, bmpOut, radius);
//    }
//
//    private static native void native_fastBlur(Bitmap bmpIn, Bitmap bmpOut, int radius);

//    static {
//        System.loadLibrary("imageutils_jni");
//    }
    //@MiuiLiteHook.DELETE_END

    public static class CropOption {
        public int rx;
        public int ry;
        public int borderWidth;
        public int borderColor;
        // 选取srcBmp的子区域，缩放绘制到destBmp上；如果为null，则默认选取srcBmp的整个区域
        public Rect srcBmpDrawingArea;
        public boolean recycleSrcBmp;

        public CropOption() {
        }

        public CropOption(int rx, int ry, int borderWidth, int borderColor) {
            this.rx = rx;
            this.ry = ry;
            this.borderWidth = borderWidth;
            this.borderColor = borderColor;
        }

        public CropOption(CropOption cOpt) {
            rx = cOpt.rx;
            ry = cOpt.ry;
            borderWidth = cOpt.borderWidth;
            borderColor = cOpt.borderColor;
            srcBmpDrawingArea = cOpt.srcBmpDrawingArea;
            recycleSrcBmp = cOpt.recycleSrcBmp;
        }
    }
}
