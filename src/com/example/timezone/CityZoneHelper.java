package com.example.timezone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.util.Log;

public class CityZoneHelper {
	private static final String CITY_ZONE_FILE = "city_timezone";

	private static CityZoneHelper ourInstance;

	private ArrayList<CityTimezoneItem> mCityTimezoneItems = new ArrayList<CityTimezoneItem>();

	public static CityZoneHelper getInstance(Context context) {
		if (null == ourInstance) {
			ourInstance = new CityZoneHelper(context);
		}
		return ourInstance;
	}

	private CityZoneHelper(Context context) {
		parseCityTimezones(context);
	}

	/**
	 * Parse cities's timezone from assert file
	 * {@link CityZoneHelper#CITY_ZONE_FILE} The format is like: id timezone
	 * citynameEN citynameCN cityNameTW PinYin 0 Asia/Beijing Beijing, China 中国,
	 * 北京 中國, 北京 beijing
	 * 
	 * @param context
	 */
	private void parseCityTimezones(Context context) {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(context
					.getAssets().open(CITY_ZONE_FILE)));
			if (bufferedReader != null) {
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					String[] items = line.split("\t");
					if (items != null && items.length == 6) {
						mCityTimezoneItems.add(new CityTimezoneItem(Integer
								.parseInt(items[0]), items[1], items[2],
								items[3], items[4], items[5]));
					}
				}
			}
		} catch (IOException e) {
			Log.e("Dozen", "parse city timezone error", e);
		} finally {
			if (null != bufferedReader) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					Log.e("Dozen", "close parse city timezone error", e);
				}
			}
		}
	}

	public CityTimezoneItem getCityTimezoneItemById(int cityId) {
		for (CityTimezoneItem item : mCityTimezoneItems) {
			if (item.mId == cityId) {
				return item;
			}
		}
		return null;
	}

	public ArrayList<CityTimezoneItem> queryCityTimezoneItems(String query) {
		ArrayList<CityTimezoneItem> queriedCityItems = new ArrayList<CityTimezoneItem>();
		query = query.toLowerCase();
		for (CityTimezoneItem item : mCityTimezoneItems) {
			if (item.mCityNameEN.toLowerCase().contains(query)
					|| item.mCityNameCN.toLowerCase().contains(query)
					|| item.mCityNameTW.toLowerCase().contains(query)
					|| item.mPinYin.toLowerCase().contains(query)) {
				queriedCityItems.add(item);
			}
		}
		return queriedCityItems;
	}

	public static class CityTimezoneItem {
		public int mId;
		public String mTimezone;
		public String mCityNameEN;
		public String mCityNameCN;
		public String mCityNameTW;
		public String mPinYin;

		public CityTimezoneItem(int id, String timezone, String cityEN,
				String cityCN, String cityTW, String pinyin) {
			mId = id;
			mTimezone = timezone;
			mCityNameEN = cityEN;
			mCityNameCN = cityCN;
			mCityNameTW = cityTW;
			mPinYin = pinyin;
		}

		/*
		 * public String getCityNameByLocale(boolean shortName) { String
		 * cityName = null; if (Locale.getDefault().equals(Locale.TAIWAN)) { if
		 * (shortName && mCityNameTW.indexOf(",") >= 0) { cityName =
		 * mCityNameTW.split(",")[1]; } else { cityName = mCityNameTW; } } else
		 * if (Locale.getDefault().equals(Locale.CHINESE) ||
		 * Locale.getDefault().equals(Locale.CHINA)) { if (shortName &&
		 * mCityNameCN.indexOf(",") >= 0) { cityName =
		 * mCityNameCN.split(",")[1]; } else { cityName = mCityNameCN; } } else
		 * { if (shortName && mCityNameEN.indexOf(",") >= 0) { cityName =
		 * mCityNameEN.split(",")[0]; } else { cityName = mCityNameEN; } }
		 * return cityName; }
		 */
		public String getCityNameByLocale(boolean shortName) {
			String cityName = null;
			if (shortName && mCityNameEN.indexOf(",") >= 0) {
				cityName = mCityNameEN.split(",")[0];
			} else {
				cityName = mCityNameEN;
			}
			return cityName;
		}
	}
}
