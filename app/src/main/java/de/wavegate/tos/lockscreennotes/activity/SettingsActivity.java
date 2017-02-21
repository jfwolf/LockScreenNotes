package de.wavegate.tos.lockscreennotes.activity;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import de.wavegate.tos.lockscreennotes.R;
import de.wavegate.tos.lockscreennotes.util.NotesNotificationManager;
import de.wavegate.tos.lockscreennotes.util.listener.SettingsBindPreferenceSummaryToValueListener;
import timber.log.Timber;

public class SettingsActivity extends AppCompatPreferenceActivity {

	public static final SettingsBindPreferenceSummaryToValueListener defaultListener = new SettingsBindPreferenceSummaryToValueListener();

	private boolean showNotifications;

	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	public static void bindPreferenceURLAsAction(Preference preference, final Uri uri) {
		preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
				preference.getContext().startActivity(browserIntent);
				return true;
			}
		});
	}

	public static void bindPreferenceURLAsAction(Preference preference) {
		String summary = preference.getSummary().toString();
		bindPreferenceURLAsAction(preference, Uri.parse(summary));
	}

	public static void bindPreferenceSummaryToValue(Preference preference) {
		bindPreferenceSummaryToValue(preference, null);
	}

	public static void bindPreferenceSummaryToValue(Preference preference, Integer resource) {
		if (preference == null) return;

		SettingsBindPreferenceSummaryToValueListener listener = defaultListener;
		if (resource != null) {
			listener = new SettingsBindPreferenceSummaryToValueListener(resource);
		}

		preference.setOnPreferenceChangeListener(listener);
		listener.onPreferenceChange(preference,
				PreferenceManager
						.getDefaultSharedPreferences(preference.getContext())
						.getString(preference.getKey(), preference.getContext().getString(R.string.error_unknown)));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setShowNotifications(true);
		setupActionBar();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isShowNotifications())
			new NotesNotificationManager(this).showNotifications();
	}

	@Override
	protected void onResume() {
		super.onResume();
		new NotesNotificationManager(this).hideNotifications();
		setShowNotifications(true);
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// Show the Up button in the action bar.
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	public boolean isShowNotifications() {
		return showNotifications;
	}

	public void setShowNotifications(boolean showNotifications) {
		this.showNotifications = showNotifications;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<PreferenceActivity.Header> target) {
		loadHeadersFromResource(R.xml.prefs_headers, target);
	}

	protected boolean isValidFragment(String fragmentName) {
		return PreferenceFragment.class.getName().equals(fragmentName)
				|| GeneralPreferenceFragment.class.getName().equals(fragmentName)
				|| NotificationsPreferenceFragment.class.getName().equals(fragmentName)
				|| DateAndTimePreferenceFragment.class.getName().equals(fragmentName)
				|| InfoAndAboutPreferenceFragment.class.getName().equals(fragmentName);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				super.onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_general);
			setHasOptionsMenu(true);

			bindPreferenceSummaryToValue(findPreference("prefs_homescreen_lines"), R.string.prefs_homescreen_lines_summary);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class NotificationsPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_notifications);
			setHasOptionsMenu(true);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class DateAndTimePreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_time);
			setHasOptionsMenu(true);

			bindPreferenceSummaryToValue(findPreference("prefs_time_detail"));
			bindPreferenceSummaryToValue(findPreference("prefs_date_detail"));
			bindPreferenceSummaryToValue(findPreference("prefs_time_locale"));
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class InfoAndAboutPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_info);
			setHasOptionsMenu(true);

			String versionName = getString(R.string.error_unknown);
			String appName = getString(R.string.app_name);

			PackageManager manager = getActivity().getPackageManager();
			try {
				PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
				versionName = info.versionName;
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}

			findPreference("pref_about").setSummary(String.format(getString(R.string.prefs_about_summary), appName, versionName));

			bindPreferenceURLAsAction(findPreference("pref_view_on_github"), Uri.parse(getString(R.string.const_github_url)));
			bindPreferenceURLAsAction(findPreference("prefs_credits_font_awesome"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_text_drawable"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_timber"));
		}

	}

}
