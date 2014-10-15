package com.serajr.blurred.system.ui.activities;

import com.serajr.blurred.system.ui.R;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.Utils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class BlurSettings_Activity extends Activity {
	
	public static String BLURRED_SYSTEM_UI_KILL_SYSTEM_UI_INTENT = "com.serajr.blurred.system.ui.KILL_SYSTEM_UI";
	
	private String APP_THEME_SETTINGS_TAG = "serajr_blurred_system_ui_app_theme";
	private String APP_THEME_SETTINGS_DEFAULT = "light";
	
	private String mAppInfo;
	private String mAppTheme;
	
	@Override
    	public void onCreate(Bundle savedInstanceState) {
		
		// seta o tema escolhido ou o padrão
		int theme = R.style.DeviceDefault_Light;
		int lightTheme = Utils.isSonyXperiaRom() ? R.style.DeviceDefault_Light_Xperia : R.style.DeviceDefault_Light;
		if (Settings.System.getString(getContentResolver(), APP_THEME_SETTINGS_TAG) != null)
			theme = Settings.System.getString(getContentResolver(), APP_THEME_SETTINGS_TAG).equals(APP_THEME_SETTINGS_DEFAULT)
					? lightTheme
					: R.style.DeviceDefault;
		setTheme(theme);
        
		super.onCreate(savedInstanceState);
		
        	// action bar
      		ActionBar actionBar = getActionBar();
      		if (actionBar != null) {
      		
	      		actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	      		actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	      	
	      		try {
	      		
	      			mAppInfo = getString(R.string.app_name) + " - v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
				actionBar.setTitle(mAppInfo);
				
			} catch (NameNotFoundException e) {
				
				e.printStackTrace();
				
			}
      		}
      	
      		// mostra o fragmento como sendo o layout
        	getFragmentManager().beginTransaction().replace(android.R.id.content, new BlurSettings_Fragment()).commit();
      	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
	    		
			case R.id.theme_menu:
		        
				// mostra o tema
				showThemeDialog();
				return true;
			
			case R.id.restart_menu:
		        
				// mostra o restart
				showRestartSystemUIDialog();
				return true;
		
			case R.id.about_menu:
		        
				// mostra o about
				showAboutDialog();
				return true;
				
			case R.id.exit_menu:
		        
				// finaliza
				finish();
				return true;
				
	    }
		
	    return super.onOptionsItemSelected(item);
	    
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
		
	}
	
	private void showThemeDialog() {
		
		// tema atual
		mAppTheme = APP_THEME_SETTINGS_DEFAULT;
		if (Settings.System.getString(getContentResolver(), APP_THEME_SETTINGS_TAG) != null)
			mAppTheme = Settings.System.getString(getContentResolver(), APP_THEME_SETTINGS_TAG);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.theme_menu_title);
        	builder.setSingleChoiceItems(getResources().getStringArray(R.array.theme_entries), mAppTheme.equals(APP_THEME_SETTINGS_DEFAULT) ? 0 : 1, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				// salva o tipo de informação selecionada
				String[] values = getResources().getStringArray(R.array.theme_values);
				mAppTheme = values[which];
				
			}
		});
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				// salva
				Settings.System.putString(getContentResolver(), APP_THEME_SETTINGS_TAG, mAppTheme);
				
			    	// fecha
			    	dialog.dismiss();
			    
			    	// mostra a menssagem
			    	Toast.makeText(BlurSettings_Activity.this, getString(R.string.theme_menu_message), Toast.LENGTH_LONG).show();
			    
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.cancel();
				
			}
		});
        
        	AlertDialog dialog = builder.create();
        	dialog.show();
		
	}
	
	private void showRestartSystemUIDialog() {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.restart_menu_title);
        	builder.setMessage(R.string.restart_menu_message);
        	builder.setNegativeButton(android.R.string.cancel, null);
        	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        	
	            	@Override
	            	public void onClick(DialogInterface dialog, int which) {
	            	
	            		// envia o intent
				Intent intent = new Intent(BLURRED_SYSTEM_UI_KILL_SYSTEM_UI_INTENT);
				BlurSettings_Activity.this.sendBroadcast(intent);
	            	
				// termina a app
				finish();
	                
	    		}
        	});
        
        	AlertDialog dialog = builder.create();
        	dialog.show();
		
	}
	
	private void showAboutDialog() {
		
		// monta
		StringBuilder about = new StringBuilder();
		about.append("Created by: SERAJR (2014)");
		about.append("\n");
		about.append("\n");
		about.append("Special thanks to:");
		about.append("\n");
		about.append("xda@rovo89");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.blurred_system_ui);
		builder.setTitle(mAppInfo);
	        builder.setMessage(about);
	        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        	
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	
	            	// dismiss
	    			dialog.dismiss();
	            	
	    		}
	        });
	        builder.setCancelable(false);
	        
	        AlertDialog dialog = builder.create();
	        dialog.show();
		
	}
}
