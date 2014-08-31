
package com.example.cropimage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.widgetclock.R;

public class CropPhotoActivity extends Activity implements OnClickListener {
    private static final String TAG = "CropPhotoActivity";
    public static final String ANIMATE = "animate";
    public static final String FILE_NAME = "fileName";
    private static final int THUMBNAIL_PHOTO_SIZE = 96;

    private PhotoFrameView mPhoto;
    private Bitmap mBitmap;
    private Button mTurnBtn;
    private Button mOk;
    private Button mCancel;
    private ImageView mAnimateView;
    private Rect mTargetScreen = new Rect();
    private boolean isAnimate = false;
    private ProgressDialog mDialog;
    private int mRotation = 0;
    private String mFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.crop_photo_layout);
        mAnimateView = (ImageView) findViewById(R.id.animate_view);
        mPhoto = (PhotoFrameView) findViewById(R.id.photo);
        mTurnBtn = (Button) findViewById(R.id.turn_btn);
        mOk = (Button) findViewById(R.id.ok);
        mCancel = (Button) findViewById(R.id.cancel);
        mTurnBtn.setOnClickListener(this);
        mOk.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        Uri uri = getIntent().getData();
        mBitmap = getBitmap(uri);

        if (mBitmap == null) {
            finish();
        }

        mTargetScreen = getIntent().getSourceBounds();

        if (mTargetScreen != null) {
            isAnimate = getIntent().getBooleanExtra(ANIMATE, false);
        }

        mFileName =  getIntent().getStringExtra(FILE_NAME);
        Log.i("Dozen"," CropPhotoActivity mFileName : " + mFileName);

        mPhoto.setImageBitmap(mBitmap);
        if (mRotation != 0) {
            mPhoto.setRotateDegrees(mRotation, true);
        }
    }

    private Bitmap getBitmap(Uri uri) {
        if (uri == null) {
            return null;
        }

        if (!TextUtils.equals(uri.getScheme(), ContentResolver.SCHEME_FILE)) {
            ContentResolver cr = this.getContentResolver();
            // get the physical path of the image
            uri = MiuiMediaUriCompat.getImageContentUri(uri);
            // @MiuiLiteHook.ADD_END
            Cursor c = cr.query(uri, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        String photoTemp = c.getString(c.getColumnIndex(ImageColumns.DATA));
                        final int orientationIndex = c.getColumnIndex(ImageColumns.ORIENTATION);
                        mRotation = orientationIndex == -1 ? ExifInterface.ORIENTATION_UNDEFINED :
                            c.getInt(orientationIndex) % 360;
                        uri = Uri.fromFile(new File(photoTemp));
                    }
                } finally {
                    c.close();
                }
            }
        } else {
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(uri.getPath());
                mRotation = exifOrientationToDegrees(exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
            } catch (Exception ex) {
                // suggest that you can ignore the exception, and set rotation
                // as 0 to continue
            }
        }

        InputStreamLoader isLoader = new InputStreamLoader(this, uri);
        BitmapFactory.Options opts = ImageUtils.getBitmapSize(isLoader);
        double picWidth = opts.outWidth;
        double picHeight = opts.outHeight;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        // 修复MiOne手机拍摄4*3相机照片无法设置为大头像
        double scale = dm.densityDpi <= DisplayMetrics.DENSITY_HIGH ? 2.0 : 1.5;
        final double maxSize = dm.heightPixels * dm.widthPixels * scale * scale;
        scale = Math.max(1, (picHeight * picWidth) / maxSize);

        // round the double value to the nearest int value
        opts.inSampleSize = (int)(scale + 0.5);
        opts.inJustDecodeBounds = false;
        try {
            return BitmapFactory.decodeStream(isLoader.get(), null, opts);
        } finally {
            isLoader.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPhoto != null) {
            mPhoto.recyleAllBitmap();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.turn_btn:
                mPhoto.setRotateDegrees(mPhoto.getRotateDegrees() + 90, true);
                break;
            case R.id.ok:
                Bitmap bitmap;
                try {
                    bitmap = mPhoto.generatePhoto();
                    savePhoto(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.cancel:
                finish();
                break;

            default:
                break;
        }
    }

    public void finishActivity() {
        if (isAnimate) {
            animateScale(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    // crop square thumbnailPhoto
    private Bitmap getScaledBitmap(final Bitmap bitmap) {
        float scaleFactor = ((float) THUMBNAIL_PHOTO_SIZE) / bitmap.getWidth();
        // Need to scale or crop the photo.
        Matrix matrix = new Matrix();
        matrix.setScale(scaleFactor, scaleFactor);
        Bitmap scaledBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        return scaledBitmap;
    }

    // 截取圈定范围的小头像
    private Bitmap getSquareBitmap(final Bitmap bitmap) {
        final int height = mAnimateView.getTop() + mAnimateView.getHeight() < bitmap.getHeight() ? mAnimateView.getHeight() : bitmap.getHeight(); // @MiuiLiteHook FIX BUG [MIUIAPP-6120]
        return Bitmap.createBitmap(bitmap, 0, mAnimateView.getTop(), mAnimateView.getWidth(),
                height);
    }

    private void savePhoto(final Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        if (mDialog == null) {
          //@MiuiLiteHook.CHANGE MIUIAPP-6604
            mDialog = new ProgressDialog(this);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setMessage(getString(R.string.save_photo_progress));
        }
        mDialog.show();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final Bitmap squareBitmap = getSquareBitmap(bitmap);

                    // 设置动画的头像
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mAnimateView.setImageBitmap(squareBitmap);
                        }
                    });

                    getIntent().putExtra("data", getScaledBitmap(squareBitmap));

                    String filePath = null;
                    // 如果选择的图片支持符合标准，则生成大头像
                    if (mPhoto.photoCropedFromLarge()) {
                        filePath = ContactPhotoUtils.pathForCroppedPhoto(
                                CropPhotoActivity.this, mFileName);
                        File f = new File(filePath);
                        if (f.exists()) {
                            f.delete();
                        }
                        f.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(f);
                        squareBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);

                        fOut.flush();
                        fOut.close();
                    }
                    Log.i("Dozen"," finish crop result Path : " + filePath);
                    getIntent().putExtra(FILE_NAME, filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    showFailedMsg();
                } catch (OutOfMemoryError ex) {
                    ex.printStackTrace();
                    showFailedMsg();
                } finally {
                    bitmap.recycle();

                    if (mDialog != null) {
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                setResult(RESULT_OK, getIntent());
                finishActivity();
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    private void showFailedMsg() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CropPhotoActivity.this, R.string.set_photo_failed_msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Creates the animation */
    private void animateScale(final Runnable onAnimationEndRunnable) {
        mAnimateView.setPivotX(mTargetScreen.left - mAnimateView.getLeft());
        mAnimateView.setPivotY(mTargetScreen.top - mAnimateView.getTop());
        ViewPropertyAnimator animator = mAnimateView.animate();
        animator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
        final int scaleInterpolator = android.R.interpolator.anticipate;
        animator.setInterpolator(AnimationUtils.loadInterpolator(this, scaleInterpolator));
        final float scaleTarget = 0.3F;
        animator.scaleX(scaleTarget);
        animator.scaleY(scaleTarget);
        animator.alpha(1.0f);

        if (onAnimationEndRunnable != null) {
            animator.setListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mPhoto.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    onAnimationEndRunnable.run();
                }
            });
        }
    }

    public static int exifOrientationToDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
        }
        return 0;
    }

}
