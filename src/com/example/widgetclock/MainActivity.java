package com.example.widgetclock;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.example.cropimage.ContactPhotoUtils;
import com.example.cropimage.CropPhotoActivity;
import com.example.cropimage.ImageUtils;
import com.example.cropimage.InputStreamLoader;
import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.FeedbackAgent;

public class MainActivity extends Activity {

	private Button mFeedbackBtn;
	
	private ImageView mImageView1;
	private ImageView mImageView2;
	private int mPhotoWidth;
	private int mPhotoHeight;
	
	private String mFileName;
	
	private static final int REQUEST_CODE_PICK_IMAGE = 10001;
	private static final int REQUEST_CODE_CROP_RESULT = 10002;
	
	private static final String PHOTO_RET1 = "Photo1.jpg";
	private static final String PHOTO_RET2 = "Photo2.jpg";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupUI();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
	
	private void setupUI() {
		mImageView1 = (ImageView) findViewById(R.id.imageView1);
		mImageView2 = (ImageView) findViewById(R.id.imageView2);
		mFeedbackBtn = (Button) findViewById(R.id.feedback);
		
		mPhotoWidth = mImageView1.getWidth();
		mPhotoHeight = mImageView2.getHeight();
		
		ContactPhotoUtils.PHOTO_WIDTH = mPhotoWidth;
		ContactPhotoUtils.PHOTO_HEIGHT = mPhotoHeight;

		
		mImageView1.setOnClickListener(mItemClickListener);
		mImageView2.setOnClickListener(mItemClickListener);
		mFeedbackBtn.setOnClickListener(mItemClickListener);
		
	}
	
	private OnClickListener mItemClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.imageView1) {
				startPhotoActivity(PHOTO_RET1);
			} else if(v.getId() == R.id.imageView2) {
				startPhotoActivity(PHOTO_RET2);
			} else if(v.getId() == R.id.feedback) {
				FeedbackAgent agent = new FeedbackAgent(MainActivity.this);
				agent.startFeedbackActivity();
			}
		}
	};
	
    public void startPhotoActivity(String photoResultName) {
        //直接进入图库
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/*");
		mFileName = photoResultName;
		try {
			startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
		} catch (ActivityNotFoundException e) {
			Log.i("Dozen", "Exception", e);
		}
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data == null) {
			super.onActivityResult(requestCode, resultCode, data);
		} else {
			switch (requestCode) {
			case REQUEST_CODE_PICK_IMAGE:
				Uri dataUri = data.getData();
				Intent intent = new Intent(this, CropPhotoActivity.class);
				intent.setData(dataUri);
				intent.putExtra(CropPhotoActivity.FILE_NAME, mFileName);
				startActivityForResult(intent, REQUEST_CODE_CROP_RESULT);
				break;
			case REQUEST_CODE_CROP_RESULT:
				String resultFilePath = data.getStringExtra(CropPhotoActivity.FILE_NAME);
				Bitmap ret = ContactPhotoUtils.getWidgetPhotoBitmap(this, resultFilePath);
				if(mFileName.equals(PHOTO_RET1)) {
					WidgetPreferenceManager.putString(this, WidgetPreferenceManager.PHOTO_WIDGET_PATH1, resultFilePath);
					mImageView1.setImageBitmap(ret);
				} else if(mFileName.equals(PHOTO_RET2)) {
					WidgetPreferenceManager.putString(this, WidgetPreferenceManager.PHOTO_WIDGET_PATH2, resultFilePath);
					mImageView2.setImageBitmap(ret);
				}
				Log.i("Dozen", "onActivityResult filePath : " + resultFilePath);
				Intent photoIntent = new Intent(PhotoChangedReceiver.PHOTO_CHANGED_ACTION);
			    photoIntent.putExtra(PhotoChangedReceiver.PHOTO_PATH, resultFilePath);
				sendBroadcast(photoIntent);

				break;

			default:
				break;
			}
		}
	}
    
}
