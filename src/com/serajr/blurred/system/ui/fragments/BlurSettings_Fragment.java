package com.serajr.blurred.system.ui.fragments;

import com.serajr.blurred.system.ui.R;
import com.serajr.custom.preferences.XXCheckBoxPreference;
import com.serajr.utils.DisplayUtils;

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
import android.view.Gravity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

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
	
	public static String BLUR_LIGHT_COLOR_PREFERENCE_KEY = "hook_system_ui_blurred_expanded_panel_light_color_pref";
	public static int BLUR_LIGHT_COLOR_PREFERENCE_DEFAULT = Color.DKGRAY;
	
	public static String BLUR_MIXED_COLOR_PREFERENCE_KEY = "hook_system_ui_blurred_expanded_panel_mixed_color_pref";
	public static int BLUR_MIXED_COLOR_PREFERENCE_DEFAULT = Color.GRAY;
	
	public static String BLUR_DARK_COLOR_PREFERENCE_KEY = "hook_system_ui_blurred_expanded_panel_dark_color_pref";
	public static int BLUR_DARK_COLOR_PREFERENCE_DEFAULT = Color.LTGRAY;
	
	public static String TRANSLUCENT_HEADER_PREFERENCE_KEY = "hook_system_ui_translucent_header_pref";
	public static boolean TRANSLUCENT_HEADER_PREFERENCE_DEFAULT = false;
	
	public static String TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_KEY = "hook_system_ui_translucent_quick_settings_pref";
	public static boolean TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_DEFAULT = false;
	
	public static String TRANSLUCENT_NOTIFICATIONS_PREFERENCE_KEY = "hook_system_ui_translucent_notifications_pref";
	public static boolean TRANSLUCENT_NOTIFICATIONS_PREFERENCE_DEFAULT = false;
	
	public static String DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_KEY = "hook_system_ui_drag_handle_translucency_pref";
	public static String DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_DEFAULT = "1.0";
	
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
        PreferenceCategory blur = new PreferenceCategory(getActivity());
        blur.setTitle(R.string.panels_category);
        getPreferenceScreen().addPreference(blur);
	        
        // status bar expandida
        SwitchPreference statusBarExpanded = new SwitchPreference(getActivity());
        statusBarExpanded.setKey(STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_KEY);
        statusBarExpanded.setTitle(R.string.panels_status_bar_expanded_title);
        statusBarExpanded.setDefaultValue(STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(statusBarExpanded);
        
        // aplicações recentes
        SwitchPreference enabled = new SwitchPreference(getActivity());
        enabled.setKey(RECENT_APPS_ENABLED_PREFERENCE_KEY);
        enabled.setTitle(R.string.panels_recent_apps_title);
        enabled.setDefaultValue(RECENT_APPS_ENABLED_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(enabled);
        
        // categoria - configurações do desfoque
        PreferenceCategory blurSettings = new PreferenceCategory(getActivity());
        blurSettings.setTitle(R.string.blur_settings_category);
        getPreferenceScreen().addPreference(blurSettings);
        
        // escala
        CharSequence[] scaleEntryValues = { "10", "20", "30", "40", "50" };
        ListPreference scale = new ListPreference(getActivity());
        scale.setKey(BLUR_SCALE_PREFERENCE_KEY);
        scale.setTitle(R.string.blur_scale_title);
        scale.setEntries(mScaleEntries);
        scale.setEntryValues(scaleEntryValues);
        scale.setDefaultValue(BLUR_SCALE_PREFERENCE_DEFAULT);
        scale.setSummary(getScaleSummary(prefs.getString(BLUR_SCALE_PREFERENCE_KEY, BLUR_SCALE_PREFERENCE_DEFAULT)));
        getPreferenceScreen().addPreference(scale);
        
        // raio
        CharSequence[] radiusEntries = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25" };
        ListPreference radius = new ListPreference(getActivity());
        radius.setKey(BLUR_RADIUS_PREFERENCE_KEY);
        radius.setTitle(R.string.blur_radius_title);
        radius.setEntries(radiusEntries);
        radius.setEntryValues(radiusEntries);
        radius.setDefaultValue(BLUR_RADIUS_PREFERENCE_DEFAULT);
        radius.setSummary(prefs.getString(BLUR_RADIUS_PREFERENCE_KEY, BLUR_RADIUS_PREFERENCE_DEFAULT));
        getPreferenceScreen().addPreference(radius);
        
        // cor clara
        ColorPickerPreference lightColor = new ColorPickerPreference(getActivity());
        lightColor.setKey(BLUR_LIGHT_COLOR_PREFERENCE_KEY);
        lightColor.setTitle(R.string.blur_light_color_title);
        lightColor.setDefaultValue(prefs.getInt(BLUR_LIGHT_COLOR_PREFERENCE_KEY, BLUR_LIGHT_COLOR_PREFERENCE_DEFAULT));
        lightColor.setAlphaSliderEnabled(false);
        lightColor.setHexValueEnabled(true);
        getPreferenceScreen().addPreference(lightColor);
        
        // cor mista
        ColorPickerPreference mixedColor = new ColorPickerPreference(getActivity());
        mixedColor.setKey(BLUR_MIXED_COLOR_PREFERENCE_KEY);
        mixedColor.setTitle(R.string.blur_mixed_color_title);
        mixedColor.setDefaultValue(prefs.getInt(BLUR_MIXED_COLOR_PREFERENCE_KEY, BLUR_MIXED_COLOR_PREFERENCE_DEFAULT));
        mixedColor.setAlphaSliderEnabled(false);
        mixedColor.setHexValueEnabled(true);
        getPreferenceScreen().addPreference(mixedColor);
        
        // cor escura
        ColorPickerPreference darkColor = new ColorPickerPreference(getActivity());
        darkColor.setKey(BLUR_DARK_COLOR_PREFERENCE_KEY);
        darkColor.setTitle(R.string.blur_dark_color_title);
        darkColor.setDefaultValue(prefs.getInt(BLUR_DARK_COLOR_PREFERENCE_KEY, BLUR_DARK_COLOR_PREFERENCE_DEFAULT));
        darkColor.setAlphaSliderEnabled(false);
        darkColor.setHexValueEnabled(true);
        getPreferenceScreen().addPreference(darkColor);
        
        // categoria - fundo transparente
        PreferenceCategory notifications = new PreferenceCategory(getActivity());
        notifications.setTitle(R.string.translucent_background_category);
        getPreferenceScreen().addPreference(notifications);
        
        // header transparente
        XXCheckBoxPreference translucentHeader = new XXCheckBoxPreference(getActivity());
        translucentHeader.setKey(TRANSLUCENT_HEADER_PREFERENCE_KEY);
        translucentHeader.setTitle(R.string.translucent_header_title);
        translucentHeader.setSummary(R.string.translucent_header_summary);
        translucentHeader.setDefaultValue(TRANSLUCENT_HEADER_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(translucentHeader);
        
        // quick settings transparente
        XXCheckBoxPreference translucentQuickSettings = new XXCheckBoxPreference(getActivity());
        translucentQuickSettings.setKey(TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_KEY);
        translucentQuickSettings.setTitle(R.string.translucent_quick_settings_title);
        translucentQuickSettings.setSummary(R.string.translucent_quick_settings_summary);
        translucentQuickSettings.setDefaultValue(TRANSLUCENT_QUICK_SETTINGS_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(translucentQuickSettings);
        
        // notificações transparentes
        CheckBoxPreference translucentNotifications = new CheckBoxPreference(getActivity());
        translucentNotifications.setKey(TRANSLUCENT_NOTIFICATIONS_PREFERENCE_KEY);
        translucentNotifications.setTitle(R.string.translucent_notifications_title);
        translucentNotifications.setSummary(R.string.translucent_notifications_summary);
        translucentNotifications.setDefaultValue(TRANSLUCENT_NOTIFICATIONS_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(translucentNotifications);
        
        // barra da alça de arraste transparente
        CharSequence[] alphaEntries = { "0.0 - " + getString(R.string.translucent_title), "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0 - " + getString(R.string.opaque_title) };
        CharSequence[] alphaEntryValues = { "0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0" };
        ListPreference dragHandle = new ListPreference(getActivity());
        dragHandle.setKey(DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_KEY);
        dragHandle.setTitle(R.string.translucent_drag_handle_title);
        dragHandle.setEntries(alphaEntries);
        dragHandle.setEntryValues(alphaEntryValues);
        dragHandle.setDefaultValue(DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_DEFAULT);
        dragHandle.setSummary(getDragHandleSummary(prefs.getString(DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_KEY, DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_DEFAULT)));
        getPreferenceScreen().addPreference(dragHandle);
        
        // categoria - ajustes
        PreferenceCategory adjustments = new PreferenceCategory(getActivity());
        adjustments.setTitle(R.string.adjustments_category);
        getPreferenceScreen().addPreference(adjustments);
        
        // fade in/out
        CheckBoxPreference fadeInOut = new CheckBoxPreference(getActivity());
        fadeInOut.setKey(BLURRED_FADE_IN_OUT_PREFERENCE_KEY);
        fadeInOut.setTitle(R.string.adjustments_fade_in_out_title);
        fadeInOut.setSummary(R.string.adjustments_fade_in_out_summary);
        fadeInOut.setDefaultValue(BLURRED_FADE_IN_OUT_PREFERENCE_DEFAULT);
        getPreferenceScreen().addPreference(fadeInOut);
	        
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
    	super.onViewCreated(view, savedInstanceState);

    	// adiciona o cabeçalho
    	ListView lv = getListView();
    	TextView tv = new TextView(view.getContext());
        tv.setPadding(0, 0, 0, DisplayUtils.getDimensionForDensity(view.getResources(), 4));
        tv.setText(R.string.app_description);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        lv.addHeaderView(tv);
        	
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
			
			// para por aqui !
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
		
		// alça de arraste
		if (key.equals(DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_KEY)) {
			
			// atualiza o summary
			getPreferenceScreen().findPreference(key).setSummary(getDragHandleSummary(sharedPreferences.getString(key, DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_DEFAULT)));
			
		}
		
		// envia um intent para atualizar as preferências
		Intent intent = new Intent(BLURRED_SYSTEM_UI_UPDATE_INTENT);
		getActivity().sendBroadcast(intent);
		
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
	
	private String getDragHandleSummary(String value) {
		
		if (value.equals("0.0"))
			value = value + " - " + getString(R.string.translucent_title); 
			
		if (value.equals("1.0"))
			value = value + " - " + getString(R.string.opaque_title);
		
		return value;
		
	}
}
