package com.example.widgetclock;

import com.example.cropimage.ContactPhotoUtils;
import com.example.cropimage.ImageUtils;
import com.example.cropimage.InputStreamLoader;
import com.example.timezone.CityZoneHelper;
import com.example.timezone.CityZoneHelper.CityTimezoneItem;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetChangedReceiver extends BroadcastReceiver{
	
	public static final String PHOTO_CHANGED_ACTION = "com.dozen.PHOTO_CHANGED_ACTION";
	public static final String PHOTO_PATH = "PHOTO_PAHT";
	
	public static final String TIMEZONE_CHANGED_ACTION = "com.dozen.TIMEZONE_CHANGED_ACTION";

	@Override
	public void onReceive(Context context, Intent intent) {
		if(PHOTO_CHANGED_ACTION.equals(intent.getAction())) {
			updateWidgetPhoto(context);
		} else if(TIMEZONE_CHANGED_ACTION.equals(intent.getAction())) {
			updateWidgetTimeZone(context);
		}
		
	}
	
	private void updateWidgetPhoto(Context context) {
    	RemoteViews views= new RemoteViews(context.getPackageName(),
                R.layout.clock_gadget);
    	
    	//set photos by preference
    	String photoPath1 = WidgetPreferenceManager.getString(context, WidgetPreferenceManager.PHOTO_WIDGET_PATH1, "");
    	String photoPath2 = WidgetPreferenceManager.getString(context, WidgetPreferenceManager.PHOTO_WIDGET_PATH2, "");
    	
    	if(!TextUtils.isEmpty(photoPath1)) {
    		Bitmap ret1 = ContactPhotoUtils.getWidgetPhotoBitmap(context, photoPath1);
    		views.setImageViewBitmap(R.id.imageView1, ret1);
    	} else {
    		views.setImageViewResource(R.id.imageView1, R.drawable.default_photo);
    	}
    	
    	if(!TextUtils.isEmpty(photoPath2)) {
    		Bitmap ret2 = ContactPhotoUtils.getWidgetPhotoBitmap(context, photoPath2);
    		views.setImageViewBitmap(R.id.imageView2, ret2);
    	} else {
    		views.setImageViewResource(R.id.imageView2, R.drawable.default_photo);
    	}

	    ComponentName componentName = new ComponentName(context,
				ClockWidgetProvider.class);
		AppWidgetManager.getInstance(context).updateAppWidget(
				componentName, views);
	}
	
	private void updateWidgetTimeZone(Context context) {
		RemoteViews views= new RemoteViews(context.getPackageName(),
                R.layout.clock_gadget);
		
		int timeZoneId1 = WidgetPreferenceManager.getInt(context, WidgetPreferenceManager.TIMEZONE_ID1, -1);
		int timeZoneId2 = WidgetPreferenceManager.getInt(context, WidgetPreferenceManager.TIMEZONE_ID2, -1);

		if(timeZoneId1 >= 0) {
			CityTimezoneItem item =  CityZoneHelper.getInstance(context).getCityTimezoneItemById(timeZoneId1);
			views.setString(R.id.time1, "setTimeZone", item.mTimezone);
			views.setTextViewText(R.id.timeZone1, item.getCityNameByLocale(true));
		}
		
		if(timeZoneId2 >= 0) {
			CityTimezoneItem item =  CityZoneHelper.getInstance(context).getCityTimezoneItemById(timeZoneId2);
			views.setString(R.id.time2, "setTimeZone", item.mTimezone);
			views.setTextViewText(R.id.timeZone2, item.getCityNameByLocale(true));
		}
		
	    ComponentName componentName = new ComponentName(context,
				ClockWidgetProvider.class);
		AppWidgetManager.getInstance(context).updateAppWidget(
				componentName, views);
	}

}
