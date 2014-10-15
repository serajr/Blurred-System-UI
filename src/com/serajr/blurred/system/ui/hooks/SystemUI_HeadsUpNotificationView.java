package com.serajr.blurred.system.ui.hooks;

import android.view.ViewGroup;

import com.android.systemui.statusbar.policy.HeadsUpNotificationView;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_HeadsUpNotificationView {
	
	public static ViewGroup mContentHolder;
	
	public static void hook() {
		
		// esta classe (HeadsUpNotificationView) não existe no JB 4.3!
		if (Utils.getAndroidAPILevel() <= 18)
			return;
		
		try {
			
			// onAttachedToWindow
			XposedHelpers.findAndHookMethod(HeadsUpNotificationView.class, "onAttachedToWindow", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// guarda
					mContentHolder = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mContentHolder"); 
					
		        	}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
}
