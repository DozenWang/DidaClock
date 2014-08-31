package com.example.widgetclock;

import com.example.cropimage.ContactPhotoUtils;
import com.example.widgetclock.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class ClockWidgetProvider extends AppWidgetProvider{
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		
    	Log.i("Dozen", "upadate ...");
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

		Intent mainIntent = createMainIntent(context);
		if(mainIntent != null) {
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
			views.setOnClickPendingIntent(R.id.layout, pendingIntent);
		}

	    ComponentName componentName = new ComponentName(context,
				ClockWidgetProvider.class);
		
		appWidgetManager.updateAppWidget(
				componentName, views);
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
	
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
	}
	
	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
	}
	
	
	private Intent createMainIntent(Context context) {
		Intent intent = new Intent();
		String packageName = "com.example.widgetclock";
		String className = "com.example.widgetclock.MainActivity";
		ComponentName cn = new ComponentName(packageName, className);
		intent.setComponent(cn);
		return intent;
	}
}
