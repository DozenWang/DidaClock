package com.example.widgetclock;

import android.content.Context;
import android.content.SharedPreferences;

public class WidgetPreferenceManager {

	private static final String CLOCK_PREFERENCE = "clock_preference";
	
	public static final String PHOTO_WIDGET_PATH1 = "widget_photo_path1";
	public static final String PHOTO_WIDGET_PATH2 = "widget_photo_path2";


	public static SharedPreferences getClockPreferences(Context context) {
		return context.getSharedPreferences(CLOCK_PREFERENCE,
				Context.MODE_PRIVATE);
	}

	public static String getString(Context context, String key, String defValue) {
		SharedPreferences sp = WidgetPreferenceManager
				.getClockPreferences(context);
		return sp.getString(key, defValue);
	}

	public static void putString(Context context, String key, String value) {
		SharedPreferences sp = WidgetPreferenceManager
				.getClockPreferences(context);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(key, value);
		editor.commit();
	}
}
