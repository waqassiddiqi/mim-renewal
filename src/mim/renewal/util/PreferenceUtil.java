package mim.renewal.util;

import java.util.prefs.Preferences;

public class PreferenceUtil {
	public static final String PREF_APP_ITERATION = "pref_app_iteration";
	
	public static int getInt(String key, int defaultValue) {
		Preferences prefs = Preferences.userNodeForPackage(mim.renewal.RenewalDaemon.class);
		return prefs.getInt(key, defaultValue);
	}
	
	public static void setInt(String key, int value) {
		Preferences prefs = Preferences.userNodeForPackage(mim.renewal.RenewalDaemon.class);
		prefs.putInt(key, value);
	}
	
	public static boolean getBoolean(String key, boolean defaultValue) {
		Preferences prefs = Preferences.userNodeForPackage(mim.renewal.RenewalDaemon.class);
		return prefs.getBoolean(key, defaultValue);
	}
	
	public static void setBoolean(String key, boolean value) {
		Preferences prefs = Preferences.userNodeForPackage(mim.renewal.RenewalDaemon.class);
		prefs.putBoolean(key, value);
	}
}
