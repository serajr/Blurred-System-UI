package com.serajr.blurred.system.ui.fragments;

import com.serajr.blurred.system.ui.R;
import com.serajr.blurred.system.ui.activities.BlurSettings_Activity;
import com.serajr.custom.preferences.XXCheckBoxPreference;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class BlurSettings_Fragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	
	public static String BLURRED_SYSTEM_UI_UPDATE_INTENT = "com.serajr.blurred.system.ui.UPDATE_PREFERENCES";
	
	public static String STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_KEY = "hook_system_ui_blurred_status_bar_expanded_enabled_pref";
	public static boolean STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_DEFAULT = true;
	
	public static String RECENT_APPS_ENABLED_PREFERENCE_KEY = "hook_system_ui_blurred_recent_app_enabled_pref";
	public static boolean RECENT_APPS_ENABLED_PREFERENCE_DEFAULT = true;
	
	public static String BLUR_SCALE_PREFERENCE_KEY = "hook_system_ui_blurred_expanded_panel_scale_pref";
	public static String BLUR_SCALE_PREFERENCE_DEFAULT = "20";
	
	public static String BLUR_RADIUS_PREFERENCE_KEY = "hook_system_ui_blurred_expanded_panel_radius_pref";
	public static String BLUR_RADIUS_PREFERENCE_DEFAULT = "4";
	
	public static String BLUR_COLOR_PREFERENCE_KEY = "hook_system_ui_blurred_expanded_panel_color_pref";
	public static int BLUR_COLOR_PREFERENCE_DEFAULT = Color.GRAY;
	
	public static String TRANSLUCENT_HEADER_PREFERENCE_KEY = "hook_system_ui_translucent_header_pref";
	public static boolean TRANSLUCENT_HEADER_PREFERENCE_DEFAULT = false;
	
	public static String TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_KEY = "hook_system_ui_translucent_quick_settings_pref";
	public static boolean TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_DEFAULT = false;
	
	public static String TRANSLUCENT_NOTIFICATIONS_PREFERENCE_KEY = "hook_system_ui_translucent_notifications_pref";
	public static boolean TRANSLUCENT_NOTIFICATIONS_PREFERENCE_DEFAULT = false;
	
	public static String PORTRAIT_MARGIN_PREFERENCE_KEY = "hook_system_ui_portrait_margin_pref";
	public static boolean PORTRAIT_MARGIN_PREFERENCE_DEFAULT = true;
	
	public static String LANDSCAPE_MARGIN_PREFERENCE_KEY = "hook_system_ui_landscape_margin_pref";
	public static boolean LANDSCAPE_MARGIN_PREFERENCE_DEFAULT = true;
	
	public static String BLURRED_FADE_IN_OUT_PREFERENCE_KEY = "hook_system_ui_blurred_fade_in_out_pref";
	public static boolean BLURRED_FADE_IN_OUT_PREFERENCE_DEFAULT = true;
	
	private CharSequence[] mScaleEntries = { "10 (1:10)", "20 (1:20)", "30 (1:30)", "40 (1:40)", "50 (1:50)" };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // muito importante !!!
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        
        // adiciona em branco
        addPreferencesFromResource(R.xml.preferences);
        
        // prefs
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        
        // categoria - painéis
        PreferenceCategory blur = new PreferenceCategory(BlurSettings_Activity.mContext);
        blur.setTitle(R.string.panels_category);
        getPreferenceScreen().addPreference(blur);
        
        // status bar expandida
        SwitchPreference statusBarExpanded = new SwitchPreference(BlurSettings_Activity.mContext);
        statusBarExpanded.setKey(STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_KEY);
        statusBarExpanded.setTitle(R.string.panels_status_bar_expanded_title);
        statusBarExpanded.setDefaultValue(STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(statusBarExpanded);
        
        // aplicações recentes
        SwitchPreference enabled = new SwitchPreference(BlurSettings_Activity.mContext);
        enabled.setKey(RECENT_APPS_ENABLED_PREFERENCE_KEY);
        enabled.setTitle(R.string.panels_recent_apps_title);
        enabled.setDefaultValue(RECENT_APPS_ENABLED_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(enabled);
        
        // categoria - configurações do desfoque
        PreferenceCategory blurSettings = new PreferenceCategory(BlurSettings_Activity.mContext);
        blurSettings.setTitle(R.string.blur_settings_category);
        getPreferenceScreen().addPreference(blurSettings);
        
        // escala
        CharSequence[] scaleEntryValues = { "10", "20", "30", "40", "50" };
        ListPreference scale = new ListPreference(BlurSettings_Activity.mContext);
        scale.setKey(BLUR_SCALE_PREFERENCE_KEY);
        scale.setTitle(R.string.blur_scale_title);
        scale.setEntries(mScaleEntries);
        scale.setEntryValues(scaleEntryValues);
        scale.setDefaultValue(BLUR_SCALE_PREFERENCE_DEFAULT);
        scale.setSummary(getScaleSummary(prefs.getString(BLUR_SCALE_PREFERENCE_KEY, BLUR_SCALE_PREFERENCE_DEFAULT)));
        getPreferenceScreen().addPreference(scale);
        
        // raio
        CharSequence[] radiusEntries = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25" };
        ListPreference radius = new ListPreference(BlurSettings_Activity.mContext);
        radius.setKey(BLUR_RADIUS_PREFERENCE_KEY);
        radius.setTitle(R.string.blur_radius_title);
        radius.setEntries(radiusEntries);
        radius.setEntryValues(radiusEntries);
        radius.setDefaultValue(BLUR_RADIUS_PREFERENCE_DEFAULT);
        radius.setSummary(prefs.getString(BLUR_RADIUS_PREFERENCE_KEY, BLUR_RADIUS_PREFERENCE_DEFAULT));
        getPreferenceScreen().addPreference(radius);
        
        // cor
        ColorPickerPreference color = new ColorPickerPreference(BlurSettings_Activity.mContext);
        color.setKey(BLUR_COLOR_PREFERENCE_KEY);
        color.setTitle(R.string.blur_color_title);
        color.setDefaultValue(prefs.getInt(BLUR_COLOR_PREFERENCE_KEY, BLUR_COLOR_PREFERENCE_DEFAULT));
        color.setAlphaSliderEnabled(false);
        color.setHexValueEnabled(true);
        getPreferenceScreen().addPreference(color);
        
        // categoria - fundo transparente
        PreferenceCategory notifications = new PreferenceCategory(BlurSettings_Activity.mContext);
        notifications.setTitle(R.string.translucent_background_category);
        getPreferenceScreen().addPreference(notifications);
        
        // header transparente
        XXCheckBoxPreference translucentHeader = new XXCheckBoxPreference(BlurSettings_Activity.mContext);
        translucentHeader.setKey(TRANSLUCENT_HEADER_PREFERENCE_KEY);
        translucentHeader.setTitle(R.string.translucent_header_title);
        translucentHeader.setSummary(R.string.translucent_header_summary);
        translucentHeader.setDefaultValue(TRANSLUCENT_HEADER_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(translucentHeader);
        
        // quick settings transparente
        XXCheckBoxPreference translucentQuickSettings = new XXCheckBoxPreference(BlurSettings_Activity.mContext);
        translucentQuickSettings.setKey(TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_KEY);
        translucentQuickSettings.setTitle(R.string.translucent_quick_settings_title);
        translucentQuickSettings.setSummary(R.string.translucent_quick_settings_summary);
        translucentQuickSettings.setDefaultValue(TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(translucentQuickSettings);
        
        // notificações transparentes
        CheckBoxPreference translucentNotifications = new CheckBoxPreference(BlurSettings_Activity.mContext);
        translucentNotifications.setKey(TRANSLUCENT_NOTIFICATIONS_PREFERENCE_KEY);
        translucentNotifications.setTitle(R.string.translucent_notifications_title);
        translucentNotifications.setSummary(R.string.translucent_notifications_summary);
        translucentNotifications.setDefaultValue(TRANSLUCENT_NOTIFICATIONS_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(translucentNotifications);
        
        // categoria - ajustes
        PreferenceCategory adjustments = new PreferenceCategory(BlurSettings_Activity.mContext);
        adjustments.setTitle(R.string.adjustments_category);
        getPreferenceScreen().addPreference(adjustments);
        
        // fade in/out
        CheckBoxPreference fadeInOut = new CheckBoxPreference(BlurSettings_Activity.mContext);
        fadeInOut.setKey(BLURRED_FADE_IN_OUT_PREFERENCE_KEY);
        fadeInOut.setTitle(R.string.adjustments_fade_in_out_title);
        fadeInOut.setSummary(R.string.adjustments_fade_in_out_summary);
        fadeInOut.setDefaultValue(BLURRED_FADE_IN_OUT_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(fadeInOut);
        
        // margem - portrait
        CheckBoxPreference portraitMargin = new CheckBoxPreference(BlurSettings_Activity.mContext);
        portraitMargin.setKey(PORTRAIT_MARGIN_PREFERENCE_KEY);
        portraitMargin.setTitle(R.string.adjustments_start_margin_portrait_title);
        portraitMargin.setSummary(R.string.adjustments_start_margin_summary);
        portraitMargin.setDefaultValue(PORTRAIT_MARGIN_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(portraitMargin);
        
        // margem - landscape
        CheckBoxPreference landscapeMargin = new CheckBoxPreference(BlurSettings_Activity.mContext);
        landscapeMargin.setKey(LANDSCAPE_MARGIN_PREFERENCE_KEY);
        landscapeMargin.setTitle(R.string.adjustments_start_margin_landscape_title);
        landscapeMargin.setSummary(R.string.adjustments_start_margin_summary);
        landscapeMargin.setDefaultValue(LANDSCAPE_MARGIN_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(landscapeMargin);
        
    }
	
	@Override
	public void onResume() {
	    super.onResume();
	    
	    // registra
	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	    
	}

	@Override
	public void onPause() {
		super.onPause();
		
		// desregistra
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			
		// se a preferência alterada necessita
		// reiniciar, não envia o broadcast !!
		if (key.equals(TRANSLUCENT_HEADER_PREFERENCE_KEY) ||
			key.equals(TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_KEY)) {
			
			// para por aqui !!
			return;
			
		}
		
		// escala
		if (key.equals(BLUR_SCALE_PREFERENCE_KEY)) {
		
			// atualiza o summary
			getPreferenceScreen().findPreference(key).setSummary(getScaleSummary(sharedPreferences.getString(key, BLUR_SCALE_PREFERENCE_DEFAULT)));
			
		}
		
		// raio
		if (key.equals(BLUR_RADIUS_PREFERENCE_KEY)) {
			
			// atualiza o summary
			getPreferenceScreen().findPreference(key).setSummary(sharedPreferences.getString(key, BLUR_RADIUS_PREFERENCE_DEFAULT));
			
		}
		
		// envia um intent para atualizar as preferências
		Intent intent = new Intent(BLURRED_SYSTEM_UI_UPDATE_INTENT);
		BlurSettings_Activity.mContext.sendBroadcast(intent);
		
	}
	
	private String getScaleSummary(String value) {
		
		if (value.equals("10")) {
			
			value = (String) mScaleEntries[0];
			
		} else if (value.equals("20")) {
			
			value = (String) mScaleEntries[1];
			
		} else if (value.equals("30")) {
			
			value = (String) mScaleEntries[2];
			
		} else if (value.equals("40")) {
			
			value = (String) mScaleEntries[3];
			
		} else if (value.equals("50")) {
			
			value = (String) mScaleEntries[4];
			
		}
		
		return value;
		
	}
}
