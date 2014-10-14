package com.serajr.blurred.system.ui.hooks;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.recent.RecentsPanelView;
import com.serajr.blurred.system.ui.R;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.BlurUtils;
import com.serajr.utils.DisplayUtils;
import com.serajr.utils.Utils;
import com.serajr.utils.BlurUtils.BlurEngine;
import com.serajr.utils.BlurUtils.BlurTaskCallback;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_RecentsPanelView {
	
	private static Rect mSrc;
	private static Rect mDst;
	private static Paint mPaint;
	private static int mBlurScale;
	private static int mBlurRadius;
	private static BlurUtils mBlurUtils;
	private static int mBlurDarkColorFilter;
	private static int mBlurMixedColorFilter;
	private static int mBlurLightColorFilter;
	private static Bitmap mBlurredScreenBitmap;
	private static boolean mBlurredRecentAppsEnabled;
	private static RecentsPanelView mRecentsPanelView;
	
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
						if (mBlurredScreenBitmap != null) {
							
							// parâmetros
							Canvas canvas = (Canvas) param.args[0];
							
							// verifica a localização (x, y) do mRecentsPanelView 
							int[] location = new int[2];
							mRecentsPanelView.getLocationOnScreen(location);
							int x = location[0];
							int y = location[1];
							
							//Log.d("location", "x: " + location[0] + " | y: " y);
							
							int canvasWidth = canvas.getWidth();
							int canvasHeight = canvas.getHeight();
							
							// rects
							mSrc.set(x, y, x + canvasWidth, y + canvasHeight);
							mDst.set(0, 0, canvasWidth, canvasHeight);
							
							// desenha
							canvas.drawBitmap(mBlurredScreenBitmap, mSrc, mDst, mPaint);
							
						}
					}
				}
			});
			
			// showIfReady
			XposedHelpers.findAndHookMethod(RecentsPanelView.class, "showIfReady", new XC_MethodHook() {
				
				@Override
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// continua ?
					if (!mBlurredRecentAppsEnabled)
						return;
					
					// obtém o recents_bg_protect
					View view = null;
					int viewResId = mRecentsPanelView.getResources().getIdentifier("recents_bg_protect", "id", Xposed.SYSTEM_UI_PACKAGE_NAME);
					if (viewResId != 0)						
						view = mRecentsPanelView.findViewById(viewResId);
					
					// seta o fundo padrão transparente para o recents_bg_protect
					if (view != null)
						view.setBackground(new ColorDrawable(Color.TRANSPARENT));
					
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
		mBlurScale = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_DEFAULT));
		mBlurRadius = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_DEFAULT));
		mBlurDarkColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_DARK_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_DARK_COLOR_PREFERENCE_DEFAULT);
		mBlurMixedColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_MIXED_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_MIXED_COLOR_PREFERENCE_DEFAULT);
		mBlurLightColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_LIGHT_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_LIGHT_COLOR_PREFERENCE_DEFAULT);
		mBlurredRecentAppsEnabled = prefs.getBoolean(BlurSettings_Fragment.RECENT_APPS_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.RECENT_APPS_ENABLED_PREFERENCE_DEFAULT);
		
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
		if (!mBlurredRecentAppsEnabled)
			return;
		
		// guarda ?
		if (mBlurUtils == null) {
			
			mBlurUtils = new BlurUtils(SystemUI_PhoneStatusBar.mStatusBarWindow.getContext());
			
			// cria o paint e os rects
			mPaint = new Paint();
			mPaint.setFlags(Paint.FILTER_BITMAP_FLAG);
			mSrc = new Rect();
			mDst = new Rect();
			
		}
		
		// callback
		BlurTask.setBlurTaskCallback(new BlurTaskCallback() {
			
			@Override
			public void blurTaskDone(Bitmap blurredBitmap) {
				
				// seta o bitmap
				mBlurredScreenBitmap = blurredBitmap;
				
				// ui thread
				if (mRecentsPanelView != null) {
				
					mRecentsPanelView.post(new Runnable() {

						@Override
						public void run() {
							
							mRecentsPanelView.invalidate();
							
						}
					});
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
						mPaint.setColorFilter(new PorterDuffColorFilter(mBlurLightColorFilter, PorterDuff.Mode.MULTIPLY));
						
					} else if (lightness >= 0.34 && lightness <= 0.66) {
						
						// imagem mista
						mPaint.setColorFilter(new PorterDuffColorFilter(mBlurMixedColorFilter, PorterDuff.Mode.MULTIPLY));
						
					} else if (lightness >= 0.67 && lightness <= 1.0) {
						
						// imagem clara (mais perto do preto)
						mPaint.setColorFilter(new PorterDuffColorFilter(mBlurDarkColorFilter, PorterDuff.Mode.MULTIPLY));
						
					}
					
				} else {
					
					// -------
					// erro !!
					// -------
					
					// seta a cor mista
					mPaint.setColorFilter(new PorterDuffColorFilter(mBlurMixedColorFilter, PorterDuff.Mode.MULTIPLY));
					
				}
			}
		});
		
		// engine
		BlurTask.setBlurEngine(BlurEngine.RenderScriptBlur);
		
		// blur
		new BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		
	}
	
	private static void defaults() {
		
		// continua ?
		if (mRecentsPanelView == null)
			return;
		
		// ------------------------
		// seta o background padrão
		// ------------------------
		
		Resources res = mRecentsPanelView.getResources();
		
		// obtém o recents_bg_protect
		View view = null;
		int viewResId = res.getIdentifier("recents_bg_protect", "id", Xposed.SYSTEM_UI_PACKAGE_NAME);
		if (viewResId != 0)
			view = mRecentsPanelView.findViewById(viewResId);
		
		// obtém o fundo padrão
		Drawable viewBg = null;
		int viewBgResId = res.getIdentifier("status_bar_recents_background", "drawable", Xposed.SYSTEM_UI_PACKAGE_NAME);
		if (viewBgResId != 0) {
			
			try {
				
				viewBg = res.getDrawable(viewBgResId);
				
			} catch (NotFoundException e) {
				
				// --------------------
				// erro não esperado !!
				// --------------------
				
				e.printStackTrace();
				
			}
		}
		
		// não encontrou o drawable por
		// algum motivo inesperado !!!!
		if (viewBg == null) {
			
			// carrega o drawable do módulo
			viewBg = Xposed.getXposedModuleResources().getDrawable(R.drawable.status_bar_recents_background);
			
		}
		
		// seta o fundo padrão para o recents_bg_protect
		if (view != null && viewBg != null)
			view.setBackground(viewBg);
		
		// recicla e anula ?
		if (mBlurredScreenBitmap != null) {
			
			mBlurredScreenBitmap.recycle();
			mBlurredScreenBitmap = null;
			
		}
	}
	
	private static class BlurTask extends AsyncTask<Void, Void, Bitmap> {
		
		private static BlurEngine mBlurEngine;
		private static BlurTaskCallback mCallback;
		
		private int[] mScreenDimens;
		private Bitmap mScreenBitmap;
			
		public static void setBlurEngine(BlurEngine blurEngine) {
			
			mBlurEngine = blurEngine;
			
		}
		
		public static void setBlurTaskCallback(BlurTaskCallback callBack) {
			
		    mCallback = callBack;
		    
		}
		
		@Override
		protected void onPreExecute() {
			
			Context context = mBlurUtils.getContext();
			
			// obtém o tamamho real da tela
			mScreenDimens = DisplayUtils.getRealScreenDimensions(context);
			
			// obtém a screenshot da tela com escala reduzida
			mScreenBitmap = DisplayUtils.takeSurfaceScreenshot(context, mBlurScale);
			
		}
		
		@Override
		protected Bitmap doInBackground(Void... arg0) {
			
			try {
				
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
				
				// retorna escalado no tamanho da tela
				return Bitmap.createScaledBitmap(mScreenBitmap, mScreenDimens[0], mScreenDimens[1], true);
				
			} catch (OutOfMemoryError e) {
				
				// erro
				return null;
				
			}
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