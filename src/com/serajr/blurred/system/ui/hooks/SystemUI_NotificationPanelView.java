package com.serajr.blurred.system.ui.hooks;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_NotificationPanelView {

	public static ImageView mBlurredBackground;
	public static View mNotificationPanelView;
	
	public static void hook() {
		
		// esta classe não existe no JB 4.1 !!
		if (Utils.getAndroidAPILevel() <= 16)
			return;
		
		try {
			
			// onFinishInflate
			XposedHelpers.findAndHookMethod(NotificationPanelView.class, "onFinishInflate", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				
					// guarda
					mNotificationPanelView = (View) param.thisObject;
					
					// cria o ImageView que conterá o bitmap com o efeito gaussian blur
					createBlurredBackground();
		        	
				}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	public static void createBlurredBackground() {
		
		// cria um novo imageview para o blurred
		mBlurredBackground = new ImageView(mNotificationPanelView.getContext());
		mBlurredBackground.setScaleType(ScaleType.CENTER_CROP);
		mBlurredBackground.setTag("ok");
		
    	// insere na posição 0 (antes de todas as vistas)
    	FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); 
    	((ViewGroup) mNotificationPanelView).addView(mBlurredBackground, 0, lp);
		
	}
}