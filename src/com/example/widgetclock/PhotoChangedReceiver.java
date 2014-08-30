package com.example.widgetclock;

import com.example.cropimage.ContactPhotoUtils;
import com.example.cropimage.ImageUtils;
import com.example.cropimage.InputStreamLoader;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class PhotoChangedReceiver extends BroadcastReceiver{
	
	public static final String PHOTO_CHANGED_ACTION = "com.dozen.PHOTO_CHANGED_ACTION";
	public static final String PHOTO_PATH = "PHOTO_PAHT";

	@Override
	public void onReceive(Context context, Intent intent) {
		if(PHOTO_CHANGED_ACTION.equals(intent.getAction())) {
//			String photoPath = intent.getStringExtra(PHOTO_PATH);
//			Log.i("Dozen", " onReceive photoPath : " + photoPath);
//			
//			InputStreamLoader streamLoader = new InputStreamLoader(photoPath);
//			Bitmap ret = ImageUtils.getBitmap(streamLoader, ContactPhotoUtils.PHOTO_WIDTH * ContactPhotoUtils.PHOTO_HEIGHT);
//			RemoteViews views= new RemoteViews(context.getPackageName(),
//	                R.layout.clock_gadget);
//			views.setBitmap(R.id.imageView1, "setImageBitmap", ret);
//			views.setBitmap(R.id.imageView2, "setImageBitmap", ret);
			
	    	RemoteViews views= new RemoteViews(context.getPackageName(),
	                R.layout.clock_gadget);
	    	
	    	//set photos by preference
	    	String photoPath1 = WidgetPreferenceManager.getString(context, WidgetPreferenceManager.PHOTO_WIDGET_PATH1, "");
	    	String photoPath2 = WidgetPreferenceManager.getString(context, WidgetPreferenceManager.PHOTO_WIDGET_PATH2, "");
	    	
	    	if(!TextUtils.isEmpty(photoPath1)) {
	    		Bitmap ret1 = ContactPhotoUtils.getWidgetPhotoBitmap(context, photoPath1);
	    		views.setImageViewBitmap(R.id.imageView1, ret1);
	    	} else {
	    		views.setImageViewResource(R.id.imageView1, R.drawable.kxy);
	    	}
	    	
	    	if(!TextUtils.isEmpty(photoPath2)) {
	    		Bitmap ret2 = ContactPhotoUtils.getWidgetPhotoBitmap(context, photoPath2);
	    		views.setImageViewBitmap(R.id.imageView2, ret2);
	    	} else {
	    		views.setImageViewResource(R.id.imageView2, R.drawable.kxy);
	    	}


		    ComponentName componentName = new ComponentName(context,
					ClockWidgetProvider.class);
			
			AppWidgetManager.getInstance(context).updateAppWidget(
					componentName, views);
		}
		
	}

}
