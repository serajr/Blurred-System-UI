package com.serajr.blurred.system.ui.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.view.View;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
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
	public static int mCloseHandleHeight;
	public static int mNavigationBarHeight;
	public static StatusBarWindowView mStatusBarWindow;
	
	private static boolean mBlurredStatusBarExpandedEnabled;
	
	public static void hook() {
		
		try {
			
			// makeStatusBarView
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "makeStatusBarView", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
					
					// guarda
					mStatusBarWindow = (StatusBarWindowView) XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindow");
					
					// obtém os campos
					Context context = mStatusBarWindow.getContext();
					Resources res = context.getResources();
					
					// somente <= JB 4.1
					if (Utils.getAndroidAPILevel() <= 16 &&
						SystemUI_NotificationPanelView.mNotificationPanelView == null) {
						
						// guarda
						SystemUI_NotificationPanelView.mNotificationPanelView = (View) XposedHelpers.getObjectField(param.thisObject, "mNotificationPanel");
						
						// onMeasure
						XposedHelpers.findAndHookMethod(SystemUI_NotificationPanelView.mNotificationPanelView.getClass(), "onMeasure", int.class, int.class, new XC_MethodHook() {
							
							@Override
							protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							
								// fade in/out ?
								if (param.thisObject == SystemUI_NotificationPanelView.mNotificationPanelView)
									SystemUI_PanelView.handleFadeInOut();
								
							}
						});
						
						// cria o ImageView que conterá o bitmap com o efeito gaussian blur
						SystemUI_NotificationPanelView.createBlurredBackground();
						
					}
					
					// dimensões
					mStatusBarHeight = res.getDimensionPixelSize(res.getIdentifier("status_bar_height", "dimen", Xposed.ANDROID_PACKAGE_NAME));
					mNavigationBarHeight = Utils.deviceHasOnScreenButtons(context) ? res.getDimensionPixelSize(res.getIdentifier("navigation_bar_height", "dimen", "android")) : 0;
					mCloseHandleHeight = res.getDimensionPixelSize(res.getIdentifier("close_handle_height", "dimen", Xposed.SYSTEM_UI_PACKAGE_NAME));
					
					// inicia o blur
					BlurUtils.init(context);
					
					// receiver
					BroadcastReceiver receiver = new BroadcastReceiver() {
            			
                        @Override
                        public void onReceive(Context context, Intent intent) {
                        	
                        	String action = intent.getAction();
                        	Handler handler = new Handler();
                        	
    						// alterou a rotação
                        	if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {

                        		// atualiza o layout do imageview ?
                        		SystemUI_NotificationPanelView.updateBlurredBackgroundLayoutParams();
                        		
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
        						if (mBlurredStatusBarExpandedEnabled && mExpandedVisible) {
        					
        							// fecha o painel
        							XposedHelpers.callMethod(param.thisObject, 
        									Utils.getAndroidAPILevel() >= 17
        										// >= 4.2
        										? "makeExpandedInvisible"
        										// <= 4.1
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
                    context.registerReceiver(receiver, intent);
                    
            		// atualizam as preferências
            		updatePreferences();
            		
            		// fundos transparentes (preferências necessitam de reboot)
            		// então somente passa por aqui no primeiro boot da rom !!!
            		SystemUI_TranslucentBackground.handleTranslucentBackgroundPrefs();
            		
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
					if (!mBlurredStatusBarExpandedEnabled)
						return;
					
					// não continua se o blur ja foi aplicado (previne um segundo blur) !!!
					if (SystemUI_NotificationPanelView.mBlurredBackground.getTag().toString().equals("blur_applied"))
						return;
					
					// blur
					BlurUtils.BlurTask.setOnBlurTaskCallback(new BlurUtils.BlurTask.BlurTaskCallback() {
						
						@Override
						public void blurTaskDone(Bitmap blurredBitmap) {
							
							if (blurredBitmap != null) {
							
								// -------------------------
								// bitmap criado com sucesso
								// -------------------------
								
								// seta o bitmap já com o efeito de desfoque
								SystemUI_NotificationPanelView.mBlurredBackground.setImageBitmap(blurredBitmap);
								
								// seta o tag de: blur aplicado 
								SystemUI_NotificationPanelView.mBlurredBackground.setTag("blur_applied");
							
							} else {
						
								// ----------------------------
								// bitmap nulo por algum motivo
								// ----------------------------
								
								// seta o filtro de cor
								SystemUI_NotificationPanelView.mBlurredBackground.setImageDrawable(new ColorDrawable(BlurUtils.BlurTask.mColorFilter));
								
								// torna visível
								if (SystemUI_NotificationPanelView.mBlurredBackground.getAlpha() != 1.0f)
									SystemUI_NotificationPanelView.mBlurredBackground.setAlpha(1.0f);
								
								// seta o tag de: erro
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
						
						// seta o tag de: pronto para receber o blur
						SystemUI_NotificationPanelView.mBlurredBackground.setTag("ready_to_blur");
						
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
		mBlurredStatusBarExpandedEnabled = prefs.getBoolean(BlurSettings_Fragment.STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_DEFAULT);
		SystemUI_NotificationPanelView.updatePreferences(prefs);
		SystemUI_BaseStatusBar.updatePreferences(prefs);
		SystemUI_PanelView.updatePreferences(prefs);
		SystemUI_RecentsPanelView.updatePreferences(prefs);
		BlurUtils.BlurTask.updatePreferences(prefs);
		
		// visível ?
		if (SystemUI_NotificationPanelView.mBlurredBackground != null)
			SystemUI_NotificationPanelView.mBlurredBackground.setVisibility(mBlurredStatusBarExpandedEnabled ? View.VISIBLE : View.GONE);
		
	}
}