package com.serajr.blurred.system.ui.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Process;
import android.view.View;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.activities.BlurSettings_Activity;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_PhoneStatusBar {
	
	public static StatusBarWindowView mStatusBarWindow;
	
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
							protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							
								// repassa
								if (param.thisObject == SystemUI_NotificationPanelView.mNotificationPanelView)
									SystemUI_NotificationPanelView.handleFadeInOut();
								
							}
						});
						
						// cria o BlurredView
						SystemUI_NotificationPanelView.createBlurredView();
						
					}
					
					// receiver
					BroadcastReceiver receiver = new BroadcastReceiver() {
            			
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
	        						if (mExpandedVisible &&
	        							SystemUI_NotificationPanelView.mBlurredStatusBarExpandedEnabled) {
	        					
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
			                mStatusBarWindow.getContext().registerReceiver(receiver, intent);
                    
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
					if (!SystemUI_NotificationPanelView.mBlurredStatusBarExpandedEnabled)
						return;
					
					// não continua se o blur ja foi aplicado (previne um segundo blur) !!!
					if (SystemUI_NotificationPanelView.mBlurredView.getTag().toString().equals("blur_applied"))
						return;
					
					// blur
					SystemUI_NotificationPanelView.startBlurTask();
					
				}
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			
					// <= 4.1
					if (Utils.getAndroidAPILevel() <= 16) {
						
						// obtém os campos
						View mCloseView = (View) XposedHelpers.getObjectField(param.thisObject, "mCloseView");
						
						// seta o alpha
						if (mCloseView != null)
							mCloseView.setAlpha((int) (255 * SystemUI_NotificationPanelView.mHandleBarAlpha));
						
					}
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
					
					// recicla
					SystemUI_NotificationPanelView.recycle();
					
				}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	private static void updatePreferences() {
		
		XSharedPreferences prefs = Xposed.getXposedXSharedPreferences();
		
		// atualiza
		SystemUI_NotificationPanelView.updatePreferences(prefs);
		SystemUI_BaseStatusBar.updatePreferences(prefs);
		SystemUI_RecentsPanelView.updatePreferences(prefs);
		
	}
}
