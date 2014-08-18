package com.serajr.blurred.system.ui.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.view.View;

import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.serajr.blurred.system.ui.R;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.activities.BlurSettings_Activity;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.BlurUtils;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_PhoneStatusBar {
	
	public static int mStatusBarHeight;
	
	private static Context mContext;
	private static Resources mResources;
	private static int mCloseHandleHeight;
	private static StatusBarWindowView mStatusBarWindow;
	private static boolean mAdjustmentsStartMarginPortrait;
	private static boolean mAdjustmentsStartMarginLandscape;
	private static boolean mBlurredNotificationPanelEnabled;
	
	public static void hook() {
		
		try {
			
			// makeStatusBarView
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "makeStatusBarView", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
					
					// guarda
					mStatusBarWindow = (StatusBarWindowView) XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindow");
					
					// somente <= JB 4.1
					if (Utils.getAndroidAPILevel() <= 16 &&
						SystemUI_NotificationPanelView.mNotificationPanelView == null) {
						
						// guarda
						SystemUI_NotificationPanelView.mNotificationPanelView = (View) XposedHelpers.getObjectField(param.thisObject, "mNotificationPanel");
						
						// onMeasure
						XposedHelpers.findAndHookMethod(SystemUI_NotificationPanelView.mNotificationPanelView.getClass(), "onMeasure", int.class, int.class, new XC_MethodHook() {
							
							@Override
							protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
							
								// fade in/out ?
								if (param.thisObject == SystemUI_NotificationPanelView.mNotificationPanelView)
									SystemUI_PanelView.handleFadeInOut();
								
							}
						});
						
						// cria o ImageView que conterá o bitmap com o efeito gaussian blur
						SystemUI_NotificationPanelView.createBlurredBackground();
						
					}
					
					// obtém os campos
					mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
					mResources = mContext.getResources();
					
					// dimensões
					mStatusBarHeight = mResources.getDimensionPixelSize(mResources.getIdentifier("status_bar_height", "dimen", Xposed.ANDROID_PACKAGE_NAME));
					mCloseHandleHeight = mResources.getDimensionPixelSize(mResources.getIdentifier("close_handle_height", "dimen", Xposed.SYSTEM_UI_PACKAGE_NAME));
					
					// inicia o blur
					BlurUtils.init(mContext);
					
					// receiver
					BroadcastReceiver br = new BroadcastReceiver() {
            			
                        @Override
                        public void onReceive(Context context, Intent intent) {
                        	
                        	String action = intent.getAction();
                        	Handler handler = new Handler();
                        	
    						// alterou a rotação
                        	if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {

                        		// recents
                        		SystemUI_RecentsPanelView.onConfigurationChanged();
                        		
                        		// -----------------------------------------------------------------------
        						// se na rotação do celular o mod estiver habilitado e o painel expandido
        						// estiver aberto, fecha o painel expandido, forçando o usuário a expandir
        						// o painel novamente para obtér a imagem desfocada com a rotação atual !!
        						// -----------------------------------------------------------------------
                        		
        						// obtém os campos
        						boolean mExpandedVisible = XposedHelpers.getBooleanField(param.thisObject, "mExpandedVisible");
        						
        						// habilitado ?
        						if (mBlurredNotificationPanelEnabled && mExpandedVisible) {
        					
        							// fecha o painel
        							XposedHelpers.callMethod(param.thisObject, 
        									Utils.getAndroidAPILevel() >= 17
        										// >= 4.2.2
        										? "makeExpandedInvisible"
        										// <= 4.1.2
        										: "performCollapse");
        						
        						}
                        	}
                        	
                        	// atualiza
                        	if (action.equals(BlurSettings_Fragment.BLURRED_SYSTEM_UI_UPDATE_INTENT)) {
                        		
                            	handler.postDelayed(new Runnable() {
                                	
                                    @Override
                                    public void run() {
                                    	
                                    	// recarregam as preferências
                                		Xposed.getXposedXSharedPreferences().reload();
                                		
                                		// atualizam as preferências
                                		updatePreferences();
                    					
                                    }
                                }, 100);
                        	}
                        	
                        	// mata o SystemUI.apk
                        	if (action.equals(BlurSettings_Activity.BLURRED_SYSTEM_UI_KILL_SYSTEM_UI_INTENT)) {
				
                        		// atrasa em meio segundo
                        		handler.postDelayed(new Runnable() {
                                	
                                    @Override
                                    public void run() {
                                    	
                                    	// mata
                                    	Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
                                    	
                                    }
                             	}, 100);
                        	}
                        }
                    };
                    	
                    // registra o receiver
                    IntentFilter intent = new IntentFilter();
                    intent.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
                    intent.addAction(BlurSettings_Fragment.BLURRED_SYSTEM_UI_UPDATE_INTENT);
                    intent.addAction(BlurSettings_Activity.BLURRED_SYSTEM_UI_KILL_SYSTEM_UI_INTENT);
                    mContext.registerReceiver(br, intent);
                    
                    // atualizam as preferências
            		updatePreferences();
            		
            		// fundos transparentes (preferências necessitam de reboot)
            		// então somente passa por aqui no primeiro boot da rom !!!
            		handleTranslucentHeaderPref();
            		
				}
			});
			
			// makeExpandedVisible
			XposedBridge.hookMethod(
					Utils.getAndroidAPILevel() >= 19
						// >= 4.4
						? XposedHelpers.findMethodExact(PhoneStatusBar.class, "makeExpandedVisible")
						// <= 4.3
						: XposedHelpers.findMethodExact(PhoneStatusBar.class, "makeExpandedVisible", boolean.class), 
					new XC_MethodHook() {
						
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// habilitado ?
					if (!mBlurredNotificationPanelEnabled)
						return;
					
					// paddings
					int left = mCloseHandleHeight;
					int top = mCloseHandleHeight;
					int orientation = mResources.getConfiguration().orientation;
					
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
					
					// blur
					BlurUtils.BlurTask.setOnBlurTaskCallback(new BlurUtils.BlurTask.BlurTaskCallback() {
						
						@Override
						public void screenshotTaken(Bitmap screenBitmap) {}
						
						@Override
						public void blurTaskDone(Bitmap blurredBitmap) {
							
							// tudo ok ?
							if (blurredBitmap != null) {
							
								// seta o bitmap já com o efeito de desfoque
								SystemUI_NotificationPanelView.mBlurredBackground.setImageBitmap(blurredBitmap);
								
								// reseta o tag
								SystemUI_NotificationPanelView.mBlurredBackground.setTag("ok");
							
							} else {
						
								// seta o filtro de cor
								SystemUI_NotificationPanelView.mBlurredBackground.setImageDrawable(new ColorDrawable(BlurUtils.BlurTask.mColorFilter));
								
								// torna visível
								if (SystemUI_NotificationPanelView.mBlurredBackground.getAlpha() != 1.0f)
									SystemUI_NotificationPanelView.mBlurredBackground.setAlpha(1.0f);
								
								// seta o tag de erro
								SystemUI_NotificationPanelView.mBlurredBackground.setTag("error");
								
							}
						}
						
					}, false);
					new BlurUtils.BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					
				}
			});
			
			// makeExpandedInvisible
			// performCollapse
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, 
					Utils.getAndroidAPILevel() >= 17 
						// >= 4.2
						? "makeExpandedInvisible"
						// >= 4.1
						: "performCollapse",
					new XC_MethodHook() {
						
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// limpa a imageview e a memória utilizada
					if (SystemUI_NotificationPanelView.mBlurredBackground != null &&
						SystemUI_NotificationPanelView.mBlurredBackground.getDrawable() != null) {
				
						// bitmap ?
						if (SystemUI_NotificationPanelView.mBlurredBackground.getDrawable() instanceof BitmapDrawable) {
							
							// recicla
						    Bitmap bitmap = ((BitmapDrawable) SystemUI_NotificationPanelView.mBlurredBackground.getDrawable()).getBitmap();
						    if (bitmap != null) {
						    	
						    	bitmap.recycle();
						    	bitmap = null;
						    	
						    }
						}
						
						// limpa
						SystemUI_NotificationPanelView.mBlurredBackground.setImageDrawable(null);
						
					}
				}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	private static void updatePreferences() {
		
		XSharedPreferences prefs = Xposed.getXposedXSharedPreferences();
		
		// atualiza
		mBlurredNotificationPanelEnabled = prefs.getBoolean(BlurSettings_Fragment.BLUR_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_ENABLED_PREFERENCE_DEFAULT);
		
		// atualiza
		mAdjustmentsStartMarginPortrait = prefs.getBoolean(BlurSettings_Fragment.PORTRAIT_MARGIN_PREFERENCE_KEY, BlurSettings_Fragment.PORTRAIT_MARGIN_PREFERENCE_DEFAULT);
		mAdjustmentsStartMarginLandscape = prefs.getBoolean(BlurSettings_Fragment.LANDSCAPE_MARGIN_PREFERENCE_KEY, BlurSettings_Fragment.LANDSCAPE_MARGIN_PREFERENCE_DEFAULT);
		
		// atualiza
		BlurUtils.BlurTask.updatePreferences(prefs);
		
		// atualiza
		SystemUI_BaseStatusBar.updatePreferences(prefs);
		
		// atualiza
		SystemUI_PanelView.updatePreferences(prefs);
		
		// atualiza
		SystemUI_RecentsPanelView.updatePreferences(prefs);
		
		// ImageView visível ?
		if (SystemUI_NotificationPanelView.mBlurredBackground != null)
			SystemUI_NotificationPanelView.mBlurredBackground.setVisibility(mBlurredNotificationPanelEnabled ? View.VISIBLE : View.GONE);
		
	}
		
	private static void handleTranslucentHeaderPref() {
		
		boolean translucent = Xposed.getXposedXSharedPreferences().getBoolean(BlurSettings_Fragment.TRANSLUCENT_HEADER_PREFERENCE_KEY, BlurSettings_Fragment.TRANSLUCENT_HEADER_PREFERENCE_DEFAULT); 
		
		// ------
		// header
		// ------
		
		// procura
		View header = mStatusBarWindow.findViewById(mResources.getIdentifier("header", "id", Xposed.SYSTEM_UI_PACKAGE_NAME));
		
		// continua ?
		if (header == null)
			return;
		
		// habilitado ?
		if (translucent)
			header.setBackground(new ColorDrawable(Color.TRANSPARENT));
		
		// ---------------------------------
		// botão de limpar todos os recentes
		// ---------------------------------
		
		// xperia e <= 4.3 ?
		if (Utils.isSonyXperiaRom() &&
			Utils.getAndroidAPILevel() <= 18) {
			
			// procura
			View clearAll = mStatusBarWindow.findViewById(mResources.getIdentifier("clear_all_button", "id", Xposed.SYSTEM_UI_PACKAGE_NAME));
			
			// continua ?
			if (clearAll == null)
				return;
			
			// habilitado ?
			if (translucent)
				clearAll.setBackground(Xposed.getXposedModuleResources().getDrawable(R.drawable.somc_quick_settings_btn_default));
			
		}
	}
}