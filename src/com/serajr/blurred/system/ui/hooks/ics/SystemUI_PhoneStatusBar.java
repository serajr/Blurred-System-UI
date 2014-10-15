package com.serajr.blurred.system.ui.hooks.ics;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.activities.BlurSettings_Activity;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.BlurUtils;
import com.serajr.utils.DisplayUtils;
import com.serajr.utils.Utils;
import com.serajr.utils.BlurUtils.BlurEngine;
import com.serajr.utils.BlurUtils.BlurTaskCallback;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_PhoneStatusBar {
	
	public static PhoneStatusBarView mStatusBarView;
	
	private static int mBlurScale;
	private static int mBlurRadius;
	private static View mExpandedView;
	private static BlurUtils mBlurUtils;
	private static boolean mBlurFadeInOut;
	private static ColorFilter mColorFilter;
	private static FrameLayout mBlurredView;
	private static int mBlurDarkColorFilter;
	private static int mBlurMixedColorFilter;
	private static int mBlurLightColorFilter;
	private static boolean mBlurredStatusBarExpandedEnabled;
	
	public static void hook() {
		
		try {
			
			// makeStatusBarView
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "makeStatusBarView", new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
					
					// guarda
					mStatusBarView = (PhoneStatusBarView) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView");
					mExpandedView = (View) XposedHelpers.getObjectField(param.thisObject, "mExpandedView");
					
					// LinearLayout - onMeasure
					XposedHelpers.findAndHookMethod(LinearLayout.class, "onMeasure", int.class, int.class, new XC_MethodHook() {
						
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						
							// repassa
							if (param.thisObject == mExpandedView)
								handleFadeInOut();
							
						}
					});
					
					// cria
					createBlurStuff(mStatusBarView.getContext());
					
					// receiver
					BroadcastReceiver receiver = new BroadcastReceiver() {
            			
			                        @Override
			                        public void onReceive(Context context, Intent intent) {
			                        	
			                        	String action = intent.getAction();
			                        	Handler handler = new Handler();
			                        	
			    				// alterou a rotação
			                        	if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
			                        		
			                        		// recents
			                        		//SystemUI_RecentsPanelView.onConfigurationChanged();
			                        		
			                        		// -----------------------------------------------------------------------
								// se na rotação do celular o mod estiver habilitado e o painel expandido
								// estiver aberto, fecha o painel expandido, forçando o usuário a expandir
								// o painel novamente para obtér a imagem desfocada com a rotação atual !!
								// -----------------------------------------------------------------------
			        		
								// obtém os campos
								boolean mExpandedVisible = XposedHelpers.getBooleanField(param.thisObject, "mExpandedVisible");
								
								// fecha o painel
								if (mBlurredStatusBarExpandedEnabled && mExpandedVisible)
									XposedHelpers.callMethod(param.thisObject, "performCollapse");
			        						
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
			                mStatusBarView.getContext().registerReceiver(receiver, intent);
                    
            				// atualizam as preferências
            				updatePreferences();
            		
		            		// fundos transparentes (preferências necessitam de reboot)
		            		// então somente passa por aqui no primeiro boot da rom !!!
		            		//SystemUI_TranslucentBackground.handleTranslucentBackgroundPrefs();
            		
				}
			});
							
			// makeExpandedVisible
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "makeExpandedVisible", new XC_MethodHook() {
						
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// habilitado ?
					if (!mBlurredStatusBarExpandedEnabled)
						return;
					
					// não continua se o blur ja foi aplicado (previne um segundo blur) !!!
					if (mBlurredView.getTag().toString().equals("blur_applied"))
						return;
					
					// callback
					BlurTask.setBlurTaskCallback(new BlurTaskCallback() {
						
						@Override
						public void blurTaskDone(Bitmap blurredBitmap) {
							
							if (blurredBitmap != null) {
								
								// -------------------------
								// bitmap criado com sucesso
								// -------------------------
									
								// cria o drawable com o filtro de cor
								BitmapDrawable drawable = new BitmapDrawable(blurredBitmap);
								drawable.setColorFilter(mColorFilter);
								
								// seta o drawable
								mBlurredView.setBackgroundDrawable(drawable);
								
								// seta o tag de: blur aplicado 
								mBlurredView.setTag("blur_applied");
								
							} else {
						
								// ----------------------------
								// bitmap nulo por algum motivo
								// ----------------------------
								
								// seta o filtro de cor
								mBlurredView.setBackgroundColor(mBlurLightColorFilter);
								
								// seta o tag de: erro
								mBlurredView.setTag("error");
								
							}
						}

						@Override
						public void dominantColor(int color) {
							
							// obtém a luminosidade da cor dominante
							double lightness = DisplayUtils.getColorLightness(color);
							
							if (lightness >= 0.0 && color <= 1.0) {
								
								// --------------------------------------------------
								// seta o filtro de cor de acordo com a cor dominante
								// --------------------------------------------------
								
								if (lightness <= 0.33) {
								
									// imagem clara (mais perto do branco)
									mColorFilter = new PorterDuffColorFilter(mBlurLightColorFilter, PorterDuff.Mode.MULTIPLY);
									
								} else if (lightness >= 0.34 && lightness <= 0.66) {
									
									// imagem mista
									mColorFilter = new PorterDuffColorFilter(mBlurMixedColorFilter, PorterDuff.Mode.MULTIPLY);
									
								} else if (lightness >= 0.67 && lightness <= 1.0) {
									
									// imagem clara (mais perto do preto)
									mColorFilter = new PorterDuffColorFilter(mBlurDarkColorFilter, PorterDuff.Mode.MULTIPLY);
									
								}
								
							} else {
								
								// -------
								// erro !!
								// -------
								
								// seta a cor mista
								mColorFilter = new PorterDuffColorFilter(mBlurMixedColorFilter, PorterDuff.Mode.MULTIPLY);
								
							}
						}
					});
					
					// engine
					BlurTask.setBlurEngine(BlurEngine.StackBlur);
					
					// blur
					new BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					
				}
			});
			
			// performCollapse
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "performCollapse", new XC_MethodHook() { 
						
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// limpa e recicla
					if (mBlurredView != null &&
						mBlurredView.getBackground() != null) {
				
						// bitmap ?
						if (mBlurredView.getBackground() instanceof BitmapDrawable) {
							
							// recicla
						    Bitmap bitmap = ((BitmapDrawable) mBlurredView.getBackground()).getBitmap();
						    if (bitmap != null) {
						    	
						    	bitmap.recycle();
						    	bitmap = null;
						    	
						    }
						}
						
						// limpa
						mBlurredView.setBackgroundDrawable(null);
						
					}
					
					// seta o tag de: pronto para receber o blur
					mBlurredView.setTag("ready_to_blur");
					
				}
			});
			
			// onTrackingViewAttached
			XposedHelpers.findAndHookMethod(PhoneStatusBar.class, "onTrackingViewAttached", new XC_MethodReplacement() {

				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				
					// obtém os campos
					Display mDisplay = (Display) XposedHelpers.getObjectField(param.thisObject, "mDisplay");
					Dialog mExpandedDialog = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mExpandedDialog");
					DisplayMetrics mDisplayMetrics = (DisplayMetrics) XposedHelpers.getObjectField(param.thisObject, "mDisplayMetrics");
					
					// adiciona o mExpandedView no mBlurView
					createBlurredExpandedView();
					
					// código original, fonte:
					// https://code.google.com/p/android-source-browsing/source/browse/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBar.java?repo=platform--frameworks--base&name=android-4.0.3_r1.1
					
					WindowManager.LayoutParams lp;
				        int pixelFormat;
	
				        // expanded view
				        pixelFormat = PixelFormat.TRANSLUCENT;
	
				        lp = mExpandedDialog.getWindow().getAttributes();
				        lp.x = 0;
				        
				        // sufficiently large negative
				        XposedHelpers.setIntField(param.thisObject, "mTrackingPosition", lp.y = mDisplayMetrics.heightPixels);
				        
				        lp.type = WindowManager.LayoutParams.TYPE_STATUS_BAR_SUB_PANEL;
				        lp.flags = 0
				                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
				                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				                | WindowManager.LayoutParams.FLAG_DITHER
				                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
				        
				        
				        if ((Boolean) XposedHelpers.callStaticMethod(ActivityManager.class, "isHighEndGfx", mDisplay))
				        	lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
				        
				        lp.format = pixelFormat;
				        lp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
				        lp.setTitle("StatusBarExpanded");
				        XposedHelpers.setObjectField(param.thisObject, "mExpandedParams", lp);
				        XposedHelpers.callMethod(param.thisObject, "updateExpandedSize");
				        
				        mExpandedDialog.getWindow().setFormat(pixelFormat);
				        mExpandedDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	
				        // adiciona o mBlurredView ao invés do mExpandedView
				        mExpandedDialog.setContentView(mBlurredView,
				                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				                                           ViewGroup.LayoutParams.MATCH_PARENT));
				        
				        mExpandedDialog.getWindow().setBackgroundDrawable(null);
				        mExpandedDialog.show();
					
					return null;
					
				}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	private static void updatePreferences() {
		
		XSharedPreferences prefs = Xposed.getXposedXSharedPreferences();
		
		// atualiza
		mBlurScale = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_DEFAULT));
		mBlurRadius = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_DEFAULT));
		mBlurFadeInOut = prefs.getBoolean(BlurSettings_Fragment.BLURRED_FADE_IN_OUT_PREFERENCE_KEY, BlurSettings_Fragment.BLURRED_FADE_IN_OUT_PREFERENCE_DEFAULT);
		mBlurDarkColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_DARK_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_DARK_COLOR_PREFERENCE_DEFAULT);
		mBlurMixedColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_MIXED_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_MIXED_COLOR_PREFERENCE_DEFAULT);
		mBlurLightColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_LIGHT_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_LIGHT_COLOR_PREFERENCE_DEFAULT);
		mBlurredStatusBarExpandedEnabled = prefs.getBoolean(BlurSettings_Fragment.STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_DEFAULT);
		
	}
	
	private static void createBlurStuff(Context context) {
		
		// inicia
		mBlurUtils = new BlurUtils(context);
		
		// cria os views
		mBlurredView = new FrameLayout(context);
		
	    	// seta o tag de: pronto para receber o blur
	    	mBlurredView.setTag("ready_to_blur");
    	
	}
	
	private static void createBlurredExpandedView() {
		
		// remove as vistas
		mBlurredView.removeAllViews();
		
		// insere o mExpandedView no mBlurView
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		mBlurredView.addView(mExpandedView, lp);
    	
    		// layout
		mBlurredView.requestLayout();
		
	}
	
	private static void handleFadeInOut() {
		
		// blur
		if (mBlurredStatusBarExpandedEnabled) {
     	
			// erro ao criar o bitmap ?
			if (mBlurredView.getTag().toString().equals("error"))
				return;
			
			// dimensões
	     		int panelHeight = mExpandedView.getMeasuredHeight();
	     		int viewHeight = mBlurredView.getMeasuredHeight();
					
	     		// alpha
			float alpha = -1f;
			
			// fade in-out ?
			if (mBlurFadeInOut) {
				
				// calcula o alpha (regra de 3)
				alpha = viewHeight > 0 && panelHeight > 0 ? (float) panelHeight / viewHeight : 0f;
				
				//Log.d("alpha_alpha", panelHeight + " | " + viewHeight + " | " + alpha);
				
			}
			
			// seta o alpha
			mBlurredView.setAlpha(alpha >= 0f && alpha <= 1f ? alpha : 1f);
			
		}
	}
	
	private static class BlurTask extends AsyncTask<Void, Void, Bitmap> {
		
		private static BlurEngine mBlurEngine;
		private static BlurTaskCallback mCallback;
		
		private Bitmap mScreenBitmap;
			
		public static void setBlurEngine(BlurEngine blurEngine) {
			
			mBlurEngine = blurEngine;
			
		}
		
		public static void setBlurTaskCallback(BlurTaskCallback callBack) {
			
		    mCallback = callBack;
		    
		}
		
		@Override
		protected void onPreExecute() {
			
			Context context = mExpandedView.getContext(); 
			
			// obtém a screenshot da tela com escala reduzida
			mScreenBitmap = DisplayUtils.takeSurfaceScreenshot(context, mBlurScale);
			
		}
		
		@Override
		protected Bitmap doInBackground(Void... arg0) {
			
			// continua ?
			if (mScreenBitmap == null)
				return null;
			
			// calback
			mCallback.dominantColor(DisplayUtils.getDominantColorByPixelsSampling(mScreenBitmap, 10, 10));
			
			// blur engine
			if (mBlurEngine == BlurEngine.RenderScriptBlur) {
			
				if (Utils.getAndroidAPILevel() >= 17) {
				
					// >= 4.2.2
					mScreenBitmap = mBlurUtils.renderScriptBlur(mScreenBitmap, mBlurRadius);
				
				} else {
					
					// <= 4.1.2
					mScreenBitmap = mBlurUtils.stackBlur(mScreenBitmap, mBlurRadius);
					
				}
				
			} else if (mBlurEngine == BlurEngine.StackBlur) {
				
				mScreenBitmap = mBlurUtils.stackBlur(mScreenBitmap, mBlurRadius);
				
			} else if (mBlurEngine == BlurEngine.FastBlur) {
				
				mBlurUtils.fastBlur(mScreenBitmap, mBlurRadius);
				
			}
				
			// retorna
			return mScreenBitmap;
	        
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			
			if (bitmap != null) {
				
				// -----------------------------
				// bitmap criado com sucesso !!!
				// -----------------------------
				
				// callback
				mCallback.blurTaskDone(bitmap);
				
			} else {
				
				// --------------------------
				// erro ao criar o bitmap !!!
				// --------------------------
					
				// callback
				mCallback.blurTaskDone(null);
				
			}
		}
	}
}
