package com.serajr.blurred.system.ui.hooks;

import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.DisplayUtils;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_NotificationPanelView {
	
	public static ImageView mBlurredBackground;
	public static View mNotificationPanelView;
	
	private static boolean mAdjustmentsStartMarginPortrait;
	private static boolean mAdjustmentsStartMarginLandscape;
	
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
	
	public static void updatePreferences(XSharedPreferences prefs) {
		
		// atualiza
		mAdjustmentsStartMarginPortrait = prefs.getBoolean(BlurSettings_Fragment.PORTRAIT_MARGIN_PREFERENCE_KEY, BlurSettings_Fragment.PORTRAIT_MARGIN_PREFERENCE_DEFAULT);
		mAdjustmentsStartMarginLandscape = prefs.getBoolean(BlurSettings_Fragment.LANDSCAPE_MARGIN_PREFERENCE_KEY, BlurSettings_Fragment.LANDSCAPE_MARGIN_PREFERENCE_DEFAULT);
		
		// atualiza os paddings se necessário
		updateBlurredBackgroundPaddings();
		
	}
	
	public static void createBlurredBackground() {
		
		// cria um novo imageview para o blurred
		mBlurredBackground = new ImageView(mNotificationPanelView.getContext());
		mBlurredBackground.setScaleType(ScaleType.CENTER_CROP);
		
		// seta o tag de: pronto para receber o blur
		mBlurredBackground.setTag("ready_to_blur");
		
    	// insere na posição 0 (antes de todas as vistas)
    	FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    	((ViewGroup) mNotificationPanelView).addView(mBlurredBackground, 0, lp);
		
    	// atualiza o layout se necessário
    	updateBlurredBackgroundLayoutParams();
    	
	}
	
	public static void updateBlurredBackgroundLayoutParams() {
		
		// somente <= 4.1
		if (Utils.getAndroidAPILevel() <= 16) {
		
			// seta o height
			int[] screenDimens = DisplayUtils.getRealScreenDimensions(mBlurredBackground.getContext());
			int height = mNotificationPanelView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
					// portrait
					? screenDimens[1] - SystemUI_PhoneStatusBar.mNavigationBarHeight
					// landscape
					: screenDimens[1];
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height);
			mBlurredBackground.setLayoutParams(lp);
			mBlurredBackground.requestLayout();
			
		}
    }
	
	private static void updateBlurredBackgroundPaddings() {
		
		// paddings
		int left = SystemUI_PhoneStatusBar.mCloseHandleHeight;
		int top = SystemUI_PhoneStatusBar.mCloseHandleHeight;
		int orientation = mNotificationPanelView.getResources().getConfiguration().orientation;
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			
			left = 0;
			
			// não utilizar o padding ?
			if (!mAdjustmentsStartMarginPortrait)
				top = 0;
			
		} else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			
			top = 0;
			
			// não utilizar o padding ?
			if (!mAdjustmentsStartMarginLandscape)
				left = 0;
			
		}
		
		// seta o padding da ImageView de acordo com a rotação e escolha do usuário
		SystemUI_NotificationPanelView.mBlurredBackground.setPadding(left, top, 0, 0);
		
	}
}