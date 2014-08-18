package com.serajr.blurred.system.ui.hooks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.recent.RecentsPanelView;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.BlurUtils;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_RecentsPanelView {
	
	private static Bitmap mBlurredBitmap;
	private static RecentsPanelView mRecentsPanelView;
	private static boolean mBlurredRecentsPanelEnabled;
	
	public static void hook() {
		
		try {
			
			// constructor
			XposedBridge.hookAllConstructors(RecentsPanelView.class, new XC_MethodHook() {
				
				@Override
	            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// guarda
					mRecentsPanelView = (RecentsPanelView) param.thisObject;
					
				}
			});
			
			// dispatchDraw
			XposedHelpers.findAndHookMethod(
					Utils.getAndroidAPILevel() >= 19
						// >= 4.4
						? RecentsPanelView.class
						// <= 4.3
						: ViewGroup.class,
					"dispatchDraw", Canvas.class, new XC_MethodHook() {
				
				@Override
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// ------------------
					// beforeHookedMethod
					// ------------------
					
					// objeto é o mRecentsPanelView ?
					if (param.thisObject == mRecentsPanelView) {
					
						// ----------------------------------------------------
						// necessário essa verificação, pois android <= 4.3 não
						// tem o método dispatchDraw dentro do RecentsPanelView
						// então o hook acontece dentro do ViewGroup, que passa
						// por todo o sistema. é mRecentsPanelView ? continua !  
						// ----------------------------------------------------
						
						// continua ?
						if (mBlurredBitmap != null) {
							
							Canvas canvas = (Canvas) param.args[0]; 
							
							// limites
							Rect src = new Rect(0, SystemUI_PhoneStatusBar.mStatusBarHeight, canvas.getWidth(), SystemUI_PhoneStatusBar.mStatusBarHeight + canvas.getHeight());
							Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
							
							// desenha
							((Canvas) param.args[0]).drawBitmap(mBlurredBitmap, src, dst, null);
							
						}
					}
				}
			});
			
			// showIfReady
			XposedHelpers.findAndHookMethod(RecentsPanelView.class, "showIfReady", new XC_MethodHook() {
				
				@Override
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// continua ?
					if (!mBlurredRecentsPanelEnabled)
						return;
					
					// obtém o recents_bg_protect
					View recentsBgProtect = null;
					int resId = mRecentsPanelView.getResources().getIdentifier("recents_bg_protect", "id", Xposed.SYSTEM_UI_PACKAGE_NAME);
					if (resId != 0)						
						recentsBgProtect = mRecentsPanelView.findViewById(resId);
					
					// seta o fundo padrão transparente para o recents_bg_protect
					if (recentsBgProtect != null)
						recentsBgProtect.setBackground(new ColorDrawable(Color.TRANSPARENT));
					
				}
			});
			
			// dismiss
			XposedHelpers.findAndHookMethod(RecentsPanelView.class, "dismiss", new XC_MethodHook() {
				
				@Override
	            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// padrões
					defaults();
					
				}
			});
			
			// >= 4.2 (<= 4.1 não tem esse método)
			if (Utils.getAndroidAPILevel() >= 17) {
				
				// dismissAndGoBack
				XposedHelpers.findAndHookMethod(RecentsPanelView.class, "dismissAndGoBack", new XC_MethodHook() {
					
					@Override
		            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						
						// padrões
						defaults();
						
					}
				});
			}
							
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	public static void updatePreferences(XSharedPreferences prefs) {
		
		// atualiza
		mBlurredRecentsPanelEnabled = prefs.getBoolean(BlurSettings_Fragment.BLUR_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_ENABLED_PREFERENCE_DEFAULT);
		
		// padrões
		defaults();
		
	}
	
	public static void onConfigurationChanged() {
		
		// -----------------
		// alterou a rotação
		// -----------------
		
		// padrões
		defaults();
		
	}
	
	public static void blur() {
		
		// continua ?
		if (!mBlurredRecentsPanelEnabled)
			return;
		
		// blur (true - retorna o bitmap no tamanho da tela !!!)
		BlurUtils.BlurTask.setOnBlurTaskCallback(new BlurUtils.BlurTask.BlurTaskCallback() {
	
			@Override
			public void screenshotTaken(Bitmap screenBitmap) {}
			
			@Override
			public void blurTaskDone(Bitmap blurredBitmap) {
				
				// guarda
				mBlurredBitmap = blurredBitmap;
				
				// redesenha ?
				if (mRecentsPanelView != null)
					mRecentsPanelView.postInvalidate();
				
			}
		}, true);
		new BlurUtils.BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		
	}
	
	private static void defaults() {
		
		// continua ?
		if (mRecentsPanelView == null)
			return;
		
		// ------------------------
		// seta o background padrão
		// ------------------------
		
		int resId;
		Resources res = mRecentsPanelView.getResources();
		
		// obtém o recents_bg_protect
		View recentsBgProtect = null;
		resId = res.getIdentifier("recents_bg_protect", "id", Xposed.SYSTEM_UI_PACKAGE_NAME);
		if (resId != 0)						
			recentsBgProtect = mRecentsPanelView.findViewById(resId);
		
		// obtém o fundo padrão
		Drawable recentsBg = null;
		resId = res.getIdentifier(
				Utils.getAndroidAPILevel() >= 17
					// >= 4.2 
					? "status_bar_recents_background"
					// <= 4.1
					: "recents_bg_protect_tile",
				"drawable", Xposed.SYSTEM_UI_PACKAGE_NAME);
		if (resId != 0)
			recentsBg = res.getDrawable(resId);
		
		// seta o fundo padrão ou transparente para o recents_bg_protect
		if (recentsBgProtect != null && recentsBg != null)
			recentsBgProtect.setBackground(recentsBg);
		
		// recicla e anula ?
		if (mBlurredBitmap != null) {
			
			mBlurredBitmap.recycle();
			mBlurredBitmap = null;
			
		}
	}
}