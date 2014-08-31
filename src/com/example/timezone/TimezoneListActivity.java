package com.example.timezone;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.timezone.CityZoneHelper.CityTimezoneItem;
import com.example.widgetclock.MainActivity;
import com.example.widgetclock.R;

public class TimezoneListActivity extends Activity{
    private ListView mListView;
    private TimezoneAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timezone_list);
		mListView = (ListView) findViewById(R.id.list_view);
		mAdapter = new TimezoneAdapter(this, false);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(mAdapter);
	}

	   private class TimezoneAdapter extends BaseAdapter implements OnItemClickListener{
	        private Context mContext;
	        private LayoutInflater mInflater;
	        private ArrayList<CityTimezoneItem> mTimezones;

	        public TimezoneAdapter(Context context, boolean sortByName) {
	            mContext = context;
	            mInflater = LayoutInflater.from(mContext);
	            mTimezones = CityZoneHelper.getInstance(mContext).queryCityTimezoneItems("");
	        }

	        @Override
	        public int getCount() {
	            return mTimezones.size();
	        }

	        @Override
	        public Object getItem(int position) {
	            return mTimezones.get(position);
	        }

	        @Override
	        public long getItemId(int position) {
	            return 0;
	        }

	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            View view;
	            if (convertView == null) {
	                view = mInflater.inflate(R.layout.timezone_list_item, parent, false);
	            } else {
	                view = convertView;
	            }

	            CityTimezoneItem item = (CityTimezoneItem) getItem(position);
	            ((TextView) view.findViewById(R.id.timezone_name)).setText(item.getCityNameByLocale(false));
	            ((TextView) view.findViewById(R.id.timezone_info)).setText(generateTimezoneName(item.mTimezone));
	            return view;
	        }

	        private String generateTimezoneName(String timezoneId) {
	            final TimeZone tz = TimeZone.getTimeZone(timezoneId);
	            final int offset = tz.getOffset(Calendar.getInstance().getTimeInMillis());
	            final int p = Math.abs(offset);
	            final StringBuilder name = new StringBuilder();
	            name.append("GMT");

	            if (offset < 0) {
	                name.append('-');
	            } else {
	                name.append('+');
	            }

	            name.append(p / (DateUtils.HOUR_IN_MILLIS));
	            name.append(':');

	            long min = p / DateUtils.MINUTE_IN_MILLIS;
	            min %= 60;

	            if (min < 10) {
	                name.append('0');
	            }
	            name.append(min);
	            return name.toString();
	        }

	        public void notifyTimezonesListOnQueryChange(String query) {
	            mTimezones = CityZoneHelper.getInstance(mContext).queryCityTimezoneItems(query);
	        }
	        
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            CityTimezoneItem item = (CityTimezoneItem) mAdapter.getItem(position);
	            setResult(RESULT_OK, new Intent().putExtra(Intent.EXTRA_TEXT, item.mId));
	            Log.i("Dozen", " onItemClick : " + item.mCityNameEN);
	            finish();
	        }

	   }
}
