package com.slipkprojects.sockshttp.preference;

import android.support.v7.preference.PreferenceFragmentCompat;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.support.v7.preference.Preference;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.EditTextPreference;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.CheckBoxPreference;
import android.content.Intent;
import com.slipkprojects.sockshttp.SocksHttpApp;
import com.slipkprojects.sockshttp.R;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.ultrasshservice.config.SettingsConstants;
import com.slipkprojects.ultrasshservice.config.Settings;
import com.slipkprojects.ultrasshservice.logger.ConnectionStatus;
import android.os.Handler;
import android.support.v7.preference.ListPreference;
import android.support.v7.app.AppCompatDelegate;
import android.content.Context;
import android.os.Build;

public class SettingsAdvancedPreference extends PreferenceFragmentCompat
	implements SettingsConstants, SkStatus.StateListener,
		Preference.OnPreferenceChangeListener
{
	private SharedPreferences mPref;

	@Override
    public void onCreatePreferences(Bundle bundle, String s)
	{
        // Load the Preferences from the XML file
        setPreferencesFromResource(R.xml.advanced_settings_preference, s);

		mPref = getPreferenceManager()
			.getDefaultSharedPreferences(getContext());
			
		Settings config = new Settings(getContext());

		/*ListPreference numberMaxThreads = (ListPreference)
			findPreference(MAXIMO_THREADS_KEY);
		numberMaxThreads.setOnPreferenceChangeListener(this);*/
		
		CheckBoxPreference checkDebug = (CheckBoxPreference) findPreference(MODO_DEBUG_KEY);
		checkDebug.setOnPreferenceChangeListener(this);
		
		// update views
		getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
		if (!SkStatus.isTunnelActive()) {
			if (new Settings(getContext()).getPrefsPrivate()
					.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				findPreference(MODO_DEBUG_KEY).setEnabled(false);
			}
		}
		
		// desativa se não suportar
		if (Build.VERSION.SDK_INT < 21) {
			String[] list = {
				FILTER_APPS,
				FILTER_BYPASS_MODE,
				FILTER_APPS_LIST
			};
			for (String key : list) {
				findPreference(key).setEnabled(false);
			}
		}
		else {
			CheckBoxPreference pref = (CheckBoxPreference) findPreference(FILTER_APPS);
			pref.setOnPreferenceChangeListener(this);
			
			enableFilterLayout(config.getIsFilterApps());
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		SkStatus.addStateListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		SkStatus.removeStateListener(this);
	}

	@Override
	public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent)
	{
		getView().post(new Runnable() {
			@Override
			public void run() {
				getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
			}
		});
	}
	

	/**
	 * Preference.OnPreferenceChangeListener
	 * Implementação
	 */

	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue)
	{
		switch(pref.getKey()) {
			case MODO_DEBUG_KEY:
				boolean isDebug = (boolean) newValue;

				if (isDebug) {
					Toast.makeText(getContext(), "Desative após terminar os testes",
						Toast.LENGTH_SHORT).show();
				}
			break;

			case FILTER_APPS:
				boolean isEnabled = (boolean) newValue;
				
				enableFilterLayout(isEnabled);
			break;
		}

		return true;
	}
	
	private void enableFilterLayout(boolean is) {
		String[] list = {
			FILTER_BYPASS_MODE,
			FILTER_APPS_LIST
		};

		for (String key : list) {
			findPreference(key).setEnabled(is);
		}
	}
	

	/**
	 * Utils
	 */

	public static void setListPreferenceSummary(ListPreference pref, String value) {
		int index = pref.findIndexOfValue(value);
		if (index >= 0) {
			pref.setSummary(pref.getEntries()[index]);
		}
	}
}
