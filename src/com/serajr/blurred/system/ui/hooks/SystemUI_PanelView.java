package com.serajr.blurred.system.ui.hooks;

import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.PanelView;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_PanelView {
	
	public static void hook() {
		
		// esta classe (PanelView) não existe no JB 4.1 !!
		if (Utils.getAndroidAPILevel() <= 16)
			return;
		
		try {
			
			// onMeasure
			XposedHelpers.findAndHookMethod(PanelView.class, "onMeasure", int.class, int.class, new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// repassa
					if (param.thisObject instanceof NotificationPanelView)
						SystemUI_NotificationPanelView.handleFadeInOut();
					
		        	}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
}
