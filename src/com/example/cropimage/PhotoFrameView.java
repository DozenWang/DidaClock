package com.example.cropimage;

import java.io.IOException;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;

import com.example.widgetclock.R;

/**
 * Used to display images, and response to scale, rotate and translate
 * operations
 */
public class PhotoFrameView extends View {

    public PhotoFrameView(Context context) {
        this(context, null);
    }

    public PhotoFrameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoFrameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PhotoFrameView, defStyle, 0);
        final int boundType = a.getInt(R.styleable.PhotoFrameView_boundType, OUTSIDE_BOUND_TYPE);

        if (boundType == OUTSIDE_BOUND_TYPE) {
            setBoundStrategy(new OutsideBoundStrategy());
        } else if (boundType == INSIDE_BOUND_TYPE) {
            int top = a.getDimensionPixelOffset(R.styleable.PhotoFrameView_insideTop,
                    InsideBoundStrategy.DEFAULT_INSIDE_TOP);
            setBoundStrategy(new InsideBoundStrategy(top));
        } else {
            throw new UnsupportedOperationException("unsupported type=" + boundType);
        }

        a.recycle();

        mFilterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mMaskPaint.setAlpha(255 - ALPHA_DEGREE);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    // -------------------------------------------------------------------------
    // public methods
    // -------------------------------------------------------------------------

    public void setOnSizeChangedListener(OnSizeChangedListener l) {
        mSizeChangedListener = l;
    }

    public void setAutoAdjustMinZoom(boolean auto) {
        mAutoAdjustMinZoom = auto;
    }

    public void setMinZoom(float minZoom) {
        mMinZoom = minZoom;
    }

    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap, 0);
    }

    public Bitmap getImageBitmap() {
        return mBitmapDisplayed;
    }

    public void setFrameBitmap(Bitmap frame) {
        mFrameBitmap = frame;
        invalidate();
    }

    public Bitmap getFrameBitmap() {
        return mFrameBitmap;
    }

    public void setFilterBitmap(Bitmap filter) {
        mFilterBitmap = filter;
        if (filter == null) {
            mTransformedFilterBitmap = null;
            return;
        }
        final int width = filter.getWidth();
        final int height = filter.getHeight();
        final int[] pixels = new int[width * height];
        final int halfAlphaHighBit = ALPHA_DEGREE << 24;
        filter.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; ++i) {
            if ((pixels[i] & 0xFF000000) == 0) {
                pixels[i] = halfAlphaHighBit;
            }
        }

        mTransformedFilterBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mTransformedFilterBitmap);
        canvas.setDrawFilter(mPaintFlags);
        canvas.drawBitmap(pixels, 0, width, 0, 0, width, height, true, null);

        invalidate();
    }

    public void recyleAllBitmap() {
        if (mTransformedFilterBitmap != null) {
            mTransformedFilterBitmap.recycle();
            mTransformedFilterBitmap = null;
        }

        if (mFilterBitmap != null) {
            mFilterBitmap.recycle();
            mFilterBitmap = null;
        }

        if (mFrameBitmap != null) {
            mFrameBitmap.recycle();
            mFrameBitmap = null;
        }

        if (mBitmapDisplayed != null) {
            mBitmapDisplayed.recycle();
            mBitmapDisplayed = null;
        }
    }

    /** 设置Image相对于Frame的Matrix */
    public void setMatrixValues(float[] values) {
        if (getWidth() == 0) {
            final float[] v = new float[MATRIX_VALUES_SIZE];
            System.arraycopy(values, 0, v, 0, MATRIX_VALUES_SIZE);
            mPrepareValues = v;
        } else {
            mPrepareValues = null;
            System.arraycopy(values, 0, mMatrixValuesTemp, 0, MATRIX_VALUES_SIZE);
            convertMatrixReference(mMatrixValuesTemp, true);
            mDisplayMatrix.setValues(mMatrixValuesTemp);
            invalidate();
        }
    }

    /** 获得Image相对于Frame的Matrix */
    public void getMatrixValues(float[] values) {
        mDisplayMatrix.getValues(values);
        convertMatrixReference(values, false);
    }

    public void resetMatrix() {
        resetMatrix(true);
    }

    public void resetMatrix(boolean resetScale) {
        if (mBitmapDisplayed != null) {
            fitCenter(mMatrixTemp, resetScale);
            mDisplayMatrix.set(mMatrixTemp);
            invalidate();
        }
    }

    public int getRotateDegrees() {
        return mRotation;
    }

    private boolean isVertical() {
        return mRotation % 180 == 0;
    }

    public void setRotateDegrees(int degrees, boolean refresh) {
        mRotation = degrees % 360;
        if (refresh) {
            fitCenter(mMatrixTemp);
            mDisplayMatrix.set(mMatrixTemp);
            invalidate();
        }
    }

    public float getFitCenterScale() {
        final int viewWidth = getWidth();
        final int viewHeight = getHeight();
        final float frameWidth;
        final float frameHeight;
        if (mFrameBitmap != null) {
            frameWidth = mFrameBitmap.getWidth();
            frameHeight = mFrameBitmap.getHeight();
        } else {
            frameWidth = viewWidth;
            frameHeight = viewHeight;
        }

        float w = mBitmapDisplayed.getWidth();
        float h = mBitmapDisplayed.getHeight();
        float scale;
        if (isVertical()) {
            scale = Math.min(frameWidth / w, frameHeight / h);
        } else {
            scale = Math.min(frameWidth / h, frameHeight / w);
        }

        return scale;
    }

    // -------------------------------------------------------------------------
    // touch event about
    // -------------------------------------------------------------------------

    private static final float DISTANCE_OF_FINGERS = 20F;
    private static final float MIN_BITMAP_SIZE = 20F;
    private static final long DOUBLE_CLICK_INTERVAL = 500;
    private static final long SINGLE_CLICK_INTERVAL = 100;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private final PointF mDown = new PointF();
    private final PointF mLastMove = new PointF();
    private final PointF mMidPoint = new PointF();
    private int mMode = NONE;
    private float mOldDist = DISTANCE_OF_FINGERS;

    private final PointF mLastDown = new PointF();
    private long mCurrentDownTime = 0;
    private long mLastDownTime = -1;
    private boolean mAutoAdjustMinZoom = true;

    private boolean mInteractive = true;

    public void setInteractive(boolean interactive) {
        mInteractive = interactive;
    }


    class JustifyAnimation extends Animation {

        private final float mStartScale;
        private final float mScaleDelta;
        private final float[] mStarts = new float[] {0, 0};
        private final float[] mOffsets = new float[] {0, 0};
        private final float[] mTemps = new float[] {0, 0};
        private final boolean mResetScale;

        JustifyAnimation(boolean resetScale) {
            mResetScale = resetScale;
            mStartScale = getScale();
            final Matrix matrix = new Matrix();
            fitCenter(matrix, resetScale);
            mScaleDelta = resetScale ? matrix.mapRadius(1F) - mStartScale : 0;
            mDisplayMatrix.mapPoints(mStarts);
            matrix.mapPoints(mOffsets);
            mOffsets[0] -= mStarts[0];
            mOffsets[1] -= mStarts[1];
        }

        public boolean getResetScale() {
            return mResetScale;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final float scale = mStartScale + mScaleDelta * interpolatedTime;
            final float deltaS = scale / getScale();
            mDisplayMatrix.postScale(deltaS, deltaS);

            float tx = mStarts[0] + mOffsets[0] * interpolatedTime;
            float ty = mStarts[1] + mOffsets[1] * interpolatedTime;
            final float[] deltaT = mTemps;
            deltaT[0] = 0;
            deltaT[1] = 0;
            mDisplayMatrix.mapPoints(deltaT);
            mDisplayMatrix.postTranslate(tx - deltaT[0], ty - deltaT[1]);
            invalidate();
        }
    }

    public static interface BoundStrategy {
        public float adjustDy(RectF bound, float dy);
        public float adjustDx(RectF bound, float dx);
        public void adjustIfNeeded(RectF bound, float scale, boolean resetScale);
        /** Setup the matrix so that image displayed fit the indicated area **/
        public void adjustCropArea(Matrix matrix, boolean resetScale);

        public int getCropedPhotoWidth();
        public int getCropedPhotoHeight();
        public Matrix getCropedMatrix();
    }

    private BoundStrategy mBoundStrategy;

    public static final int OUTSIDE_BOUND_TYPE = 0;
    public static final int INSIDE_BOUND_TYPE = 1;

    public void setBoundStrategy(BoundStrategy strategy) {
        mBoundStrategy = strategy;
    }

    public class OutsideBoundStrategy implements BoundStrategy {

        @Override
        public float adjustDx(RectF bound, float dx) {
            if (dx < 0 && bound.right + dx < 0) {
                // 向左
                dx = -bound.right;
            } else if (dx > 0 && bound.left + dx > getWidth()) {
                // 向右
                dx = getWidth() - bound.left;
            }

            return dx;
        }

        @Override
        public float adjustDy(RectF bound, float dy) {
            if (dy < 0 && bound.bottom + dy < 0) {
                // 向上
                dy = -bound.bottom;
            } else if (dy > 0 && bound.top + dy > getHeight()) {
                // 向下
                dy = getHeight() - bound.top;
            }

            return dy;
        }

        @Override
        public void adjustIfNeeded(RectF bound, float scale, boolean resetScale) {
        }

        @Override
        public void adjustCropArea(Matrix matrix, boolean resetScale) {
            final float w = mBitmapDisplayed.getWidth();
            final float h = mBitmapDisplayed.getHeight();
            matrix.reset();
            if (mRotation != 0) {
                matrix.postRotate(mRotation, w / 2, h / 2);
            }

            // Fit center: position and scale
            final float scale = getFitCenterScale();
            matrix.postScale(scale, scale);
            matrix.postTranslate((getWidth() - w * scale) / 2F, (getHeight() - h * scale) / 2F);
        }

        @Override
        public int getCropedPhotoWidth() {
            return getWidth();
        }

        @Override
        public int getCropedPhotoHeight() {
            return getHeight();
        }

        @Override
        public Matrix getCropedMatrix() {
            final float[] matrixValues = new float[MATRIX_VALUES_SIZE];
            mDisplayMatrix.getValues(matrixValues) ;
            convertMatrixReference(matrixValues, false);
            Matrix result = new Matrix();
            result.setValues(matrixValues);
            return result;
        }

    }

    public class InsideBoundStrategy implements BoundStrategy, AnimationListener {
        final int mTop;

        public static final int DEFAULT_INSIDE_TOP = 0;

        public InsideBoundStrategy(int top) {
            mTop = top;
        }

        @Override
        public float adjustDx(RectF bound, float dx) {
            if (bound.left > 0 || bound.right < getWidth()) {
                dx /= 2F;
            }

            return dx;
        }

        @Override
        public float adjustDy(RectF bound, float dy) {
            if (bound.top > mTop || bound.bottom < mTop + getWidth()) {
                dy /= 2F;
            }

            return dy;
        }

        @Override
        public void adjustIfNeeded(RectF bound, float scale, boolean resetScale) {
            if (resetScale) {
                if (scale < getFitCenterScale()) {
                    startAdjust(true);
                }
            } else {
                if (bound.left > 0 ||
                    bound.right < getWidth() ||
                    bound.top > mTop ||
                    bound.bottom < mTop + getWidth()) {
                    startAdjust(false);
                }
            }
        }

        private void startAdjust(boolean resetScale) {
            setInteractive(false);
            final Animation anim = new JustifyAnimation(resetScale);
            anim.setDuration(400);
            anim.setAnimationListener(this);
            startAnimation(anim);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            resetMatrix(((JustifyAnimation) animation).getResetScale());
            setInteractive(true);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void adjustCropArea(Matrix matrix, boolean resetScale) {
            final int viewWidth = getWidth();
            final int viewHeight = getHeight();
            float w = mBitmapDisplayed.getWidth();
            float h = mBitmapDisplayed.getHeight();
            final RectF bound = getImageBounds();
            matrix.reset();
            if (mRotation != 0) {
                matrix.postRotate(mRotation, w / 2, h / 2);
            }
            float tranX = 0;
            float tranY = 0;
            final float scale = resetScale ? getFitCenterScale() : getScale();
            if (!isVertical()) {
                tranX = (h - w) * scale / 2F;
                tranY = (w - h) * scale / 2F;
                final float temp = h;
                h = w;
                w = temp;
            }

            if (resetScale) {
                if (w * scale / 2F > viewWidth / 2F) {
                    tranX += (viewWidth - w * scale) / 2F;
                } else {
                    tranX += (getWidth() - w * scale) / 2F;
                }
                if (h * scale / 2F > viewHeight / 2F - mTop) {
                    tranY += (viewHeight - h * scale) / 2F;
                } else {
                    tranY += (getWidth() - h * scale) / 2F + mTop;
                }
            } else {
                if (bound.right - bound.left > getWidth()) {
                    if (bound.left > 0) {
                        // 使左边框重合
                        tranX += 0;
                    } else if (bound.right < getWidth()) {
                        // 使右边框重合
                        tranX += getWidth() - w * scale;
                    } else {
                        // 水平方向位置不变
                        tranX += bound.left;
                    }
                } else {
                    // 水平方向居中显示
                    tranX += (getWidth() - w * scale) / 2F;
                }

                if (bound.bottom - bound.top > getWidth()) {
                    if (bound.top > mTop) {
                        // 使上边框对齐
                        tranY += mTop;
                    } else if (bound.bottom < getWidth() + mTop) {
                        // 使下边框对齐
                        tranY += getWidth() + mTop - h * scale;
                    } else {
                        // 垂直方向位置不变
                        tranY += bound.top;
                    }
                } else {
                    // 垂直方向居中显示
                    tranY += (getWidth() - h * scale) / 2F + mTop;
                }
            }
            matrix.postScale(scale, scale);
            matrix.postTranslate(tranX, tranY);
        }

        @Override
        public int getCropedPhotoWidth() {
            return CONTACT_THUMBNAIL_BIT;
        }

        @Override
        public int getCropedPhotoHeight() {
            return CONTACT_THUMBNAIL_BIT;
        }

        @Override
        public Matrix getCropedMatrix() {
            final float[] matrixValues = new float[MATRIX_VALUES_SIZE];
            mDisplayMatrix.getValues(matrixValues);
            Matrix result = new Matrix();
            result.setValues(matrixValues);
            float scale = (float) CONTACT_THUMBNAIL_BIT / (float) getWidth();
            result.postScale(scale, scale, 0, 0);
            result.postTranslate(0, -mTop * scale);
            return result;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (!mInteractive) {
                    return false;
                }

                mLastDown.set(mDown.x, mDown.y);
                mLastDownTime = mCurrentDownTime;
                mCurrentDownTime = event.getEventTime();

                mDown.set(event.getX(), event.getY());
                mLastMove.set(event.getX(), event.getY());
                mMode = DRAG;

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!mInteractive) {
                    return false;
                }

                mOldDist = spacing(event);
                if (mOldDist > DISTANCE_OF_FINGERS) {
                    midPoint(mMidPoint, event);
                    mMode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
                mMode = NONE;
                if (isSingleClick(event)) {
                    if (isDoubleClick(event)) {
                        resetMatrix();
                    }
                } else if (mBoundStrategy != null) {
                    RectF bound = getImageBounds();
                    float scale = getScale();
                    mBoundStrategy.adjustIfNeeded(bound, scale, scale < getFitCenterScale());
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mMode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mInteractive) {
                    return false;
                }

                if (mMode == DRAG && event.getEventTime() - mCurrentDownTime > SINGLE_CLICK_INTERVAL) {
                    float dx = event.getX() - mLastMove.x;
                    float dy = event.getY() - mLastMove.y;
                    mLastMove.set(event.getX(), event.getY());
                    final RectF bound = getImageBounds();
                    if (bound != null) {
                        if (mBoundStrategy != null) {
                            dx = mBoundStrategy.adjustDx(bound, dx);
                            dy = mBoundStrategy.adjustDy(bound, dy);
                        }
                        panBy(dx, dy);
                    }

                } else if (mMode == ZOOM) {
                    if (event.getX() > getRight() || event.getX() < getLeft()) {
                        return true;
                    }

                    float newDist = spacing(event);
                    if (newDist > DISTANCE_OF_FINGERS) {// 防止手指抖动
                        float rate = newDist / mOldDist;
                        mOldDist = newDist;
                        float scale = getScale() * rate;
                        // 只限制最小，不限之最大，因为初始时平铺就可能大于最大
                        if (scale < mMinZoom) {
                            scale = mMinZoom;
                        }

                        zoomTo(scale, mMidPoint.x, mMidPoint.y);
                    }
                }
                break;
        }

        return true;
    }

    private boolean isSingleClick(MotionEvent e) {
        return (mCurrentDownTime > 0 &&
                e.getEventTime() - mCurrentDownTime < SINGLE_CLICK_INTERVAL &&
                Math.abs(e.getX() - mDown.x) < DISTANCE_OF_FINGERS && Math.abs(e.getY()
                - mDown.y) < DISTANCE_OF_FINGERS);
    }

    private boolean isDoubleClick(MotionEvent e) {
        return (mLastDownTime > 0 &&
                e.getEventTime() - mLastDownTime < DOUBLE_CLICK_INTERVAL &&
                Math.abs(e.getX() - mLastDown.x) < DISTANCE_OF_FINGERS && Math.abs(e.getY()
                - mLastDown.y) < DISTANCE_OF_FINGERS);
    }

    private void panBy(float dx, float dy) {
        mDisplayMatrix.postTranslate(dx, dy);
        invalidate();
    }

    /** Determine the space between the first two fingers */
    private static float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    /** Calculate the mid point of the first two fingers */
    private static void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    // ------------------------------------------------------------------------
    // bitmap transaction about
    // ------------------------------------------------------------------------

    public static final int MATRIX_VALUES_SIZE = 9;

    private static final int ALPHA_DEGREE = 80;

    private static final int CONTACT_LARGE_PHOTO_MIN_LENTH = 400;
    private static final int CONTACT_THUMBNAIL_BIT = 96;

    /**
     * This is the final matrix which is computed as the concatentation of the
     * base matrix and the supplementary matrix.
     */
    final Matrix mDisplayMatrix = new Matrix();

    /** Since scale cannot be get after rotated, keep it as variable **/
    private int mRotation = 0;

    /**
     * 保存是否有手动调用setDisplayMatrixValues(). 如果有,在onLayout时,不需要自动调整mDisplayMatris
     */
    private float[] mPrepareValues = null;

    /** Temporary buffer used for getting the values out of a matrix. */
    private final float[] mMatrixValuesTemp = new float[MATRIX_VALUES_SIZE];

    private final Matrix mMatrixTemp = new Matrix();

    /** The current bitmap being displayed. */
    private Bitmap mBitmapDisplayed = null;

    /** The frame bitmap */
    private Bitmap mFrameBitmap = null;

    /** The filter bitmap */
    private Bitmap mTransformedFilterBitmap = null;

    private Bitmap mFilterBitmap = null;

    private float mMinZoom = 0.1F;
    private Paint mFilterPaint = new Paint();
    private Paint mMaskPaint = new Paint();
    private PaintFlagsDrawFilter mPaintFlags = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            if (mPrepareValues == null) {
                fitCenter(mMatrixTemp);
                mDisplayMatrix.set(mMatrixTemp);
            } else {
                System.arraycopy(mPrepareValues, 0, mMatrixValuesTemp, 0, MATRIX_VALUES_SIZE);
                convertMatrixReference(mMatrixValuesTemp, true);
                mDisplayMatrix.setValues(mMatrixValuesTemp);
            }
        }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mSizeChangedListener != null) {
            mSizeChangedListener.onSizeChanged(this);
        }
    }

    private Bitmap mTempBitmap = null;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.setDrawFilter(mPaintFlags);
        updateTempBitmap();
        canvas.drawBitmap(mTempBitmap, 0, 0, null);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private void setImageBitmap(Bitmap bitmap, int orientation) {
        if (bitmap != null) {
            setImageBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), orientation);
        } else {
            setImageBitmap(bitmap, 0, 0, orientation);
        }
    }

    private void setImageBitmap(Bitmap bitmap, int imageRawWidth, int imageRawHeight,
            int orientation) {
        mBitmapDisplayed = bitmap;
        if (mAutoAdjustMinZoom) {
            updateMinZoom();
        }
        invalidate();
    }

    public RectF getImageBounds() {
        final Bitmap bm = mBitmapDisplayed;
        if (bm == null) {
            return null;
        }

        final RectF rect = new RectF(0, 0, bm.getWidth(), bm.getHeight());
        mDisplayMatrix.mapRect(rect);

        if (mFrameBitmap != null) {
            int h = (getWidth() - mFrameBitmap.getWidth()) / 2;
            rect.left += h;
            rect.right -= h;
            int v = (getHeight() - mFrameBitmap.getHeight()) / 2;
            rect.top += v;
            rect.bottom -= v;
        }
        return rect;
    }

    /** Get the scale factor out of the matrix. */
    float getScale() {
        return mDisplayMatrix.mapRadius(1F);
    }

    /** Setup the base matrix so that the image is centered and scaled properly. */
    void fitCenter(Matrix matrix) {
        fitCenter(matrix, true);
    }

    void fitCenter(Matrix matrix, boolean resetScale) {
        if (mBitmapDisplayed == null) {
            return;
        }

        if (mBoundStrategy != null) {
            mBoundStrategy.adjustCropArea(matrix, resetScale);
        }
    }

    private void updateMinZoom() {
        float minZoom = 1F;
        final Bitmap bm = mBitmapDisplayed;
        if (bm != null) {
            final int min = (bm.getWidth() < bm.getHeight()) ? bm.getWidth() : bm.getHeight();
            if (min > MIN_BITMAP_SIZE) {
                minZoom = MIN_BITMAP_SIZE / min;
            } else {
                minZoom = 1F;
            }
        }

        mMinZoom = minZoom;
    }

    private void zoomTo(float scale, float centerX, float centerY) {
        float oldScale = getScale();
        float deltaScale = scale / oldScale;
        mDisplayMatrix.postScale(deltaScale, deltaScale, centerX, centerY);

        center(false, false);
    }

    // Center as much as possible in one or both axis. Centering is
    // defined as follows: if the image is scaled down below the
    // view's dimensions then center it (literally). If the image
    // is scaled larger than the view and is translated out of view
    // then translate it back into view (i.e. eliminate black bars).
    private void center(boolean horizontal, boolean vertical) {
        final Bitmap bm = mBitmapDisplayed;
        if (bm == null) {
            invalidate();
            return;
        }

        final RectF rect = new RectF(0, 0, bm.getWidth(), bm.getHeight());
        mDisplayMatrix.mapRect(rect);

        float height = rect.height();
        float width = rect.width();

        float deltaX = 0, deltaY = 0;

        if (vertical) {
            int viewHeight = getHeight();
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = getHeight() - rect.bottom;
            }
        }

        if (horizontal) {
            int viewWidth = getWidth();
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
            }
        }
        mDisplayMatrix.postTranslate(deltaX, deltaY);
        invalidate();
    }

    private boolean convertMatrixReference(float[] values, boolean toRaw) {
        if (getWidth() == 0) {
            return false;
        }

        if (mFrameBitmap == null) {
            return true;
        }

        final Matrix m = new Matrix();
        m.setValues(values);

        int dx = (getWidth() - mFrameBitmap.getWidth()) / 2;
        int dy = (getHeight() - mFrameBitmap.getHeight()) / 2;
        if (!toRaw) {
            dx = -dx;
            dy = -dy;
        }
        m.postTranslate(dx, dy);
        m.getValues(values);
        return true;
    }

    private void updateTempBitmap() {
        final int height = getHeight();
        final int width = getWidth();
        if (mTempBitmap == null || mTempBitmap.getHeight() != height ||
                mTempBitmap.getWidth() != width) {
            if (mTempBitmap != null) {
                mTempBitmap.recycle();
            }
            mTempBitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
        }

        final Canvas canvas = new Canvas(mTempBitmap);
        canvas.setDrawFilter(mPaintFlags);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        final Bitmap bm = mBitmapDisplayed;
        if (bm != null) {
            canvas.save();
            if (mDisplayMatrix != null) {
                canvas.concat(mDisplayMatrix);
            }
            canvas.drawBitmap(bm, 0, 0, null);
            canvas.restore();
        }

        final int viewWidth = getWidth();
        final int viewHeight = getHeight();
        int offsetX = 0;
        int offsetY = 0;
        int bmHeight = 0;
        int bmWidth = 0;
        if (mTransformedFilterBitmap != null) {
            offsetX = (viewWidth - mTransformedFilterBitmap.getWidth()) / 2;
            offsetY = (viewHeight - mTransformedFilterBitmap.getHeight()) / 2;
            canvas.drawBitmap(mTransformedFilterBitmap, offsetX, offsetY, mFilterPaint);
            bmHeight = mTransformedFilterBitmap.getHeight();
            bmWidth = mTransformedFilterBitmap.getWidth();
        }

        if (mFrameBitmap != null) {
            offsetX = (viewWidth - mFrameBitmap.getWidth()) / 2;
            offsetY = (viewHeight - mFrameBitmap.getHeight()) / 2;
            canvas.drawBitmap(mFrameBitmap, offsetX, offsetY, null);
            if (bmHeight <= 0) {
                bmHeight = mFrameBitmap.getHeight();
                bmWidth = mFrameBitmap.getWidth();
            }
        }

        if (offsetX > 0 && offsetY > 0 && (offsetX + bmWidth < viewWidth) &&
                (offsetY + bmHeight < viewHeight)) {
            canvas.clipRect(offsetX, offsetY, offsetX + bmWidth, offsetY + bmHeight,
                    Region.Op.DIFFERENCE);
            canvas.drawRect(0, 0, viewWidth, viewHeight, mMaskPaint);
        }
    }

    public boolean photoCropedFromLarge() {
//        float scale = getScale();
//        final float l = Math.min(mBitmapDisplayed.getWidth(), mBitmapDisplayed.getHeight());
//        return l > CONTACT_LARGE_PHOTO_MIN_LENTH && l * scale > CONTACT_LARGE_PHOTO_MIN_LENTH;
    	return true;
    }

    public Bitmap generatePhoto() throws IOException {
        mDisplayMatrix.getValues(mMatrixValuesTemp) ;
        convertMatrixReference(mMatrixValuesTemp, false);
        mMatrixTemp.setValues(mMatrixValuesTemp);

        return generatePhoto(mFrameBitmap, mFilterBitmap, mBitmapDisplayed,
                mMatrixTemp, getHeight(), getWidth());
    }

    // 根据不同的策略获取裁剪后的相片
    public Bitmap generateCropedPhoto() throws IOException {
        if (mBoundStrategy != null) {
            return generatePhoto(null, null, mBitmapDisplayed, mBoundStrategy.getCropedMatrix(),
                    mBoundStrategy.getCropedPhotoHeight(), mBoundStrategy.getCropedPhotoWidth());
        }
        return generatePhoto();
    }

    public static Bitmap generatePhoto(Bitmap frame, Bitmap filter, Bitmap image, Matrix matrix,
            int dftHeight, int dftWidth) throws IOException {
        // 1 获得frame的宽高
        final int frameHeight = (frame != null) ? frame.getHeight() : dftHeight;
        final int frameWidth = (frame != null) ? frame.getWidth() : dftWidth;

        final Bitmap result = Bitmap.createBitmap(frameWidth, frameHeight,
                Bitmap.Config.ARGB_8888);
        final Canvas dest = new Canvas(result);
        dest.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG
                | Paint.FILTER_BITMAP_FLAG));
        // 2 根据config画photo
        if (image != null) {
            if (matrix != null) {
                dest.drawBitmap(image, matrix, null);
            } else {
                dest.drawBitmap(image, 0, 0, null);
            }
        }

        // 3 画filterBitmap
        if (filter != null) {
            final Paint maskPaint = new Paint();
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            dest.drawBitmap(filter, 0, 0, maskPaint);
        }

        // 4 画相框
        if (frame != null) {
            dest.drawBitmap(frame, 0, 0, null);
        }

        return result;
    }

    private OnSizeChangedListener mSizeChangedListener;

    public static interface OnSizeChangedListener {
        void onSizeChanged(PhotoFrameView v);
    }

}
