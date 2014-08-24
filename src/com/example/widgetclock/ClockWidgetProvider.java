package com.example.widgetclock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.example.widgetclock.R;

import android.R.string;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

public class ClockWidgetProvider extends AppWidgetProvider{
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		
		RemoteViews views= new RemoteViews(context.getPackageName(),
                R.layout.clock_gadget);

		Intent alarmIntent = createAlarmIntent(context);
		if(alarmIntent != null) {
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, alarmIntent, 0);
			views.setOnClickPendingIntent(R.id.layout, pendingIntent);
		}

	    ComponentName componentName = new ComponentName(context,
				ClockWidgetProvider.class);
		
		appWidgetManager.updateAppWidget(
				componentName, views);
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
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
