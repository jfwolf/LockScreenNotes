package de.nilsfo.lockscreennotes.activity;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import de.nilsfo.lockscreennotes.io.FileManager;
import de.nilsfo.lockscreennotes.receiver.alarms.LSNAlarmManager;
import de.nilsfo.lockscreennotes.util.NotesNotificationManager;
import de.nilsfo.lockscreennotes.util.VersionManager;
import de.nilsfo.lockscreennotes.util.listener.SettingsBindPreferenceSummaryToValueListener;
import de.nilsfo.lsn.R;
import timber.log.Timber;

import static de.nilsfo.lockscreennotes.io.backups.BackupManager.AUTO_DELETE_MAX_FILE_COUNT;

public class SettingsActivity extends AppCompatPreferenceActivity {

	public static final SettingsBindPreferenceSummaryToValueListener defaultListener = new SettingsBindPreferenceSummaryToValueListener();
	public static final int LAST_UPDATE_DATE_UNKNOWN = -1;
	private boolean showNotifications;

	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	public static void bindPreferenceURLAsAction(Preference preference, final Uri uri, final boolean chooser) {
		preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
				if (chooser) {
					browserIntent = Intent.createChooser(browserIntent, preference.getContext().getString(R.string.share_via));
				}

				preference.getContext().startActivity(browserIntent);
				return true;
			}
		});
	}

	public static void bindPreferenceURLAsAction(Preference preference, final Uri uri) {
		bindPreferenceURLAsAction(preference, uri, false);
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
		listener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), preference.getContext().getString(R.string.error_unknown)));
	}

	private static int getUpdateDays(Context context) {
		Date now = new Date();
		Date lastUpdate = null;
		try {
			lastUpdate = VersionManager.getCurrentVersionDate(context);
		} catch (Exception e) {
			e.printStackTrace();
			Timber.e(e);
			return LAST_UPDATE_DATE_UNKNOWN;
		}

		long diffTime = now.getTime() - lastUpdate.getTime();
		long diffDays = diffTime / (1000 * 60 * 60 * 24);
		return (int) diffDays;
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
			new NotesNotificationManager(this).showNoteNotifications();
	}

	@Override
	protected void onResume() {
		super.onResume();
		new NotesNotificationManager(this).hideAllNotifications();
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
				|| AutoBackupPreferenceFragment.class.getName().equals(fragmentName)
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
			bindPreferenceSummaryToValue(findPreference("prefs_action_bar_icon_scale"));

			final Preference resetTutorial = findPreference("prefs_reset_tutorial");
			resetTutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Context context = preference.getContext();
					resetTutorial(context);
					Toast.makeText(context, R.string.prefs_reset_tutorial_success, Toast.LENGTH_LONG).show();
					return true;
				}
			});
		}

		private void resetTutorial(Context context) {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putBoolean("prefs_hide_tutorial", false);
			editor.putBoolean("prefs_ignore_tutorial_autosave", false);
			editor.apply();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class NotificationsPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_notifications);
			setHasOptionsMenu(true);

			findPreference("prefs_system_notifications").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Context context = preference.getContext();
					Intent intent = new Intent();
					if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
						intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
						intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
					} else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
						intent.putExtra("app_package", context.getPackageName());
						intent.putExtra("app_uid", context.getApplicationInfo().uid);
					} else {
						intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.addCategory(Intent.CATEGORY_DEFAULT);
						intent.setData(Uri.parse("package:" + context.getPackageName()));
					}

					context.startActivity(intent);
					return true;
				}
			});
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class AutoBackupPreferenceFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_auto_backup);
			setHasOptionsMenu(true);

			FileManager manager = new FileManager(getActivity());
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

			findPreference("pref_auto_backup_info").setSummary(getString(R.string.pref_auto_backup_info_summary, manager.getNoteBackupDir()));
			findPreference("pref_auto_backups_delete_old").setSummary(getString(R.string.pref_auto_backups_delete_old_summary, String.valueOf(AUTO_DELETE_MAX_FILE_COUNT)));
			findPreference("pref_auto_backups_enabled").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object o) {
					boolean b = Boolean.valueOf(o.toString());
					Timber.i("'auto backups' new value: " + b);
					return true;
				}
			});

			Preference schedulePreference = findPreference("pref_auto_backups_schedule_days");
			schedulePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object o) {
					String s = o.toString();
					Timber.i("Schedule of days changed. New value: " + s);
					preference.setSummary(preference.getContext().getString(R.string.pref_auto_backups_schedule_days_summary, s));

					return true;
				}
			});
			schedulePreference.setSummary(getActivity().getString(R.string.pref_auto_backups_schedule_days_summary, preferences.getString("pref_auto_backups_schedule_days", "3")));
		}

		@Override
		public void onPause() {
			super.onPause();
			Timber.i("Stopping the Backup preference activity. Let's see if a alarm will be scheduled...");
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

			LSNAlarmManager manager = new LSNAlarmManager(getActivity());
			if (preferences.getBoolean("pref_auto_backups_enabled", false)) {
				//if (scheduleChanged) {
				Timber.i("Changes detected. Requesting a new backup timer!");
				manager.cancelNextAutoBackup();
				manager.scheduleNextAutoBackup();
				//} else {
				//	Timber.i("Backups are enabled, but the schedule didn't change. So no new schedule is requested.");
				//}
			} else {
				manager.cancelNextAutoBackup();
				Timber.i("A next auto backup alarm will not be scheduled and any old ones will be disabled.");
			}
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

			int versionCode = 0;
			String versionName = getString(R.string.error_unknown);
			String appName = getString(R.string.app_name);

			PackageManager manager = getActivity().getPackageManager();
			try {
				PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
				versionName = info.versionName;
				versionCode = info.versionCode;
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}

			final int finalVersionCode = versionCode;
			Preference.OnPreferenceClickListener displayVersionUpdateNewsAction = new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					VersionManager.displayVersionUpdateNews(preference.getContext(), finalVersionCode);
					return true;
				}
			};

			Preference aboutPreference = findPreference("pref_about");
			aboutPreference.setSummary(String.format(getString(R.string.prefs_about_summary), appName, versionName));
			aboutPreference.setOnPreferenceClickListener(displayVersionUpdateNewsAction);

			Preference externalFeedPreference = findPreference("pref_github_releases_feed");
			externalFeedPreference.setSummary(getString(R.string.pref_github_releases_summary) + "\n" + getString(R.string.const_github_update_feed_url));
			//bindPreferenceURLAsAction(externalFeedPreference, Uri.parse(getString(R.string.const_github_url)), true);
			externalFeedPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					ClipboardManager clipboard = (ClipboardManager) preference.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText(getString(R.string.app_name), getString(R.string.const_github_update_feed_url));
					if (clipboard != null) {
						clipboard.setPrimaryClip(clip);
						Toast.makeText(preference.getContext(), R.string.action_github_url_success, Toast.LENGTH_LONG).show();
					}else{
						Toast.makeText(preference.getContext(),R.string.error_clipboard_unavailable,Toast.LENGTH_LONG).show();
					}
					return true;
				}
			});

			bindPreferenceURLAsAction(findPreference("pref_view_on_github"), Uri.parse(getString(R.string.const_github_url)));
			bindPreferenceURLAsAction(findPreference("prefs_credits_font_awesome"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_text_drawable"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_timber"));
			bindPreferenceURLAsAction(findPreference("prefs_credits_debug_db"));
			bindPreferenceURLAsAction(findPreference("pref_view_on_play_store"), Uri.parse(getString(R.string.const_google_play_url)));

			findPreference("pref_share_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					String text = getString(R.string.pref_share_app_text, getString(R.string.app_name), getString(R.string.const_google_play_url));

					Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, text);
					sendIntent.setType("text/plain");
					startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.action_share)));
					return true;
				}
			});

			Preference updateDayCounter = findPreference("pref_update_day_counter");
			int lastUpdateDays = getUpdateDays(getActivity());
			if (lastUpdateDays == LAST_UPDATE_DATE_UNKNOWN) {
				Timber.w("Failed to get the last date this app was updated! Doing nothing, as this will result in the text that an error occured!");
			} else {
				updateDayCounter.setSummary(getString(R.string.pref_update_day_counter_summary, String.valueOf(lastUpdateDays)));
			}
			updateDayCounter.setOnPreferenceClickListener(displayVersionUpdateNewsAction);
		}
	}
}
