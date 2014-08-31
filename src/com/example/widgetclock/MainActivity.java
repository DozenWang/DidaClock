package com.example.widgetclock;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.example.cropimage.ContactPhotoUtils;
import com.example.cropimage.CropPhotoActivity;
import com.example.cropimage.ImageUtils;
import com.example.cropimage.InputStreamLoader;
import com.example.timezone.CityZoneHelper;
import com.example.timezone.CityZoneHelper.CityTimezoneItem;
import com.example.timezone.TimezoneListActivity;
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
	private static final int REQUEST_CODE_TIMEZONE_CHANGED = 10003;
	
	private static final String PHOTO_RET1 = "Photo1.jpg";
	private static final String PHOTO_RET2 = "Photo2.jpg";
	
	private TextClock mTextClock1;
	private TextClock mTextClock2;
	private TextView mTimeZone1;
	private TextView mTimeZone2;
	private int mChangedTimeZone = -1;
	
	private static final int TIMEZONE1 = 1;
	private static final int TIMEZONE2 = 2;
	
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
		mTextClock1 = (TextClock) findViewById(R.id.time1);
		mTextClock2 = (TextClock) findViewById(R.id.time2);
		mTimeZone1 = (TextView) findViewById(R.id.timeZone1);
		mTimeZone2 = (TextView) findViewById(R.id.timeZone2);
		
		mImageView1.setOnClickListener(mItemClickListener);
		mImageView2.setOnClickListener(mItemClickListener);
		mFeedbackBtn.setOnClickListener(mItemClickListener);
		mTextClock1.setOnClickListener(mItemClickListener);
		mTextClock2.setOnClickListener(mItemClickListener);
		
		mPhotoWidth = mImageView1.getWidth();
		mPhotoHeight = mImageView2.getHeight();
		
		ContactPhotoUtils.PHOTO_WIDTH = mPhotoWidth;
		ContactPhotoUtils.PHOTO_HEIGHT = mPhotoHeight;

		
		String photoPath1 = WidgetPreferenceManager.getString(this, WidgetPreferenceManager.PHOTO_WIDGET_PATH1, "");
		if(!TextUtils.isEmpty(photoPath1)) {
			mImageView1.setImageBitmap(ContactPhotoUtils.getWidgetPhotoBitmap(this, photoPath1));
		} else {
			mImageView1.setImageResource(R.drawable.default_photo);
		}
		
		String photoPath2 = WidgetPreferenceManager.getString(this, WidgetPreferenceManager.PHOTO_WIDGET_PATH2, "");
		if(!TextUtils.isEmpty(photoPath2)) {
			mImageView2.setImageBitmap(ContactPhotoUtils.getWidgetPhotoBitmap(this, photoPath2));
		} else {
			mImageView2.setImageResource(R.drawable.default_photo);
		}
	}
	
	private OnClickListener mItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.imageView1) {
				startPhotoActivity(PHOTO_RET1);
			} else if (v.getId() == R.id.imageView2) {
				startPhotoActivity(PHOTO_RET2);
			} else if (v.getId() == R.id.feedback) {
				FeedbackAgent agent = new FeedbackAgent(MainActivity.this);
				agent.startFeedbackActivity();
			} else if (v.getId() == R.id.time1) {
				startTimeZoneListActivity(TIMEZONE1);
			} else if (v.getId() == R.id.time2) {
				startTimeZoneListActivity(TIMEZONE2);
			}
		}
	};
	
    private void startPhotoActivity(String photoResultName) {
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
    
    private void startTimeZoneListActivity(int timeZoneId) {
    	Intent intent = new Intent(MainActivity.this,
				TimezoneListActivity.class);
    	mChangedTimeZone = timeZoneId;
		startActivityForResult(intent, REQUEST_CODE_TIMEZONE_CHANGED);
    }
    
    private void sendWidgetChangedBroadcast(String action) {
    	Intent photoIntent = new Intent(action);
		sendBroadcast(photoIntent);
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
				sendWidgetChangedBroadcast(WidgetChangedReceiver.PHOTO_CHANGED_ACTION);

				break;
			case REQUEST_CODE_TIMEZONE_CHANGED:
				int cityId = data.getIntExtra(Intent.EXTRA_TEXT, -1);
				Log.i("Dozen", " cityId : " + cityId);
				CityTimezoneItem item = CityZoneHelper.getInstance(this).getCityTimezoneItemById(cityId);
				if(mChangedTimeZone == TIMEZONE1) {
					mTextClock1.setTimeZone(item.mTimezone);
					mTimeZone1.setText(item.getCityNameByLocale(true));
					WidgetPreferenceManager.putInt(this, WidgetPreferenceManager.TIMEZONE_ID1, item.mId);
				} else if(mChangedTimeZone == TIMEZONE2) {
					mTextClock2.setTimeZone(item.mTimezone);
					mTimeZone2.setText(item.getCityNameByLocale(true));
					WidgetPreferenceManager.putInt(this, WidgetPreferenceManager.TIMEZONE_ID2, item.mId);
				}
				sendWidgetChangedBroadcast(WidgetChangedReceiver.TIMEZONE_CHANGED_ACTION);
				break;

			default:
				break;
			}
		}
	}
    
}
