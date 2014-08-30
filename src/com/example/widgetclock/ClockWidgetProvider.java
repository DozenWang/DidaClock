package com.example.widgetclock;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.example.cropimage.ContactPhotoUtils;
import com.example.widgetclock.R;

import android.R.string;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
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
    		views.setImageViewResource(R.id.imageView1, R.drawable.kxy);
    	}
    	
    	if(!TextUtils.isEmpty(photoPath2)) {
    		Bitmap ret2 = ContactPhotoUtils.getWidgetPhotoBitmap(context, photoPath2);
    		views.setImageViewBitmap(R.id.imageView2, ret2);
    	} else {
    		views.setImageViewResource(R.id.imageView2, R.drawable.kxy);
    	}


//		Intent alarmIntent = createAlarmIntent(context);
//		if(alarmIntent != null) {
//			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, alarmIntent, 0);
//			views.setOnClickPendingIntent(R.id.layout, pendingIntent);
//		}

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
	
	
	private Intent createAlarmIntent(Context context) {
		try {
			PackageManager packageManager = context.getPackageManager();
			Intent alarmClockIntent = new Intent(Intent.ACTION_MAIN)
					.addCategory(Intent.CATEGORY_LAUNCHER);
			String packageName = "com.android.deskclock";
			String className = "com.android.deskclock.AlarmClock";
			ComponentName cn = new ComponentName(packageName, className);
			ActivityInfo aInfo = packageManager.getActivityInfo(cn,
					PackageManager.GET_META_DATA);
			alarmClockIntent.setComponent(cn);

			return alarmClockIntent;

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
