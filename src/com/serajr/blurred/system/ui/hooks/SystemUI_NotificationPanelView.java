package com.serajr.blurred.system.ui.hooks;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.phone.NotificationPanelView;
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

public class SystemUI_NotificationPanelView {
	
	public static float mHandleBarAlpha;
	public static FrameLayout mBlurredView;
	public static View mNotificationPanelView;
	public static boolean mBlurredStatusBarExpandedEnabled;
	
	private static int mBlurScale;
	private static int mBlurRadius;
	private static BlurUtils mBlurUtils;
	private static boolean mBlurFadeInOut;
	private static ColorFilter mColorFilter;
	private static int mBlurDarkColorFilter;
	private static int mBlurMixedColorFilter;
	private static int mBlurLightColorFilter;
	private static Bitmap mBlurredScreenBitmap;
	private static FrameLayout mInnerBlurredView;
	
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
					
					// cria o BlurredView
					createBlurredView();
					
				}
			});
			
			// draw
			XposedHelpers.findAndHookMethod(NotificationPanelView.class, "draw", Canvas.class, new XC_MethodHook() {
				
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				
					// 1° - mHandleBar
					try {
					
						// obtém os campos
						Drawable mHandleBar = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mHandleBar");
						
						// seta o alpha
						if (mHandleBar.getAlpha() != (int) (255 * mHandleBarAlpha))
							mHandleBar.setAlpha((int) (255 * mHandleBarAlpha));
						
						return;
						
					} catch (NoSuchFieldError e) {}
					
					// 2° - mHandleView
					try {
						
						// obtém os campos
						View mHandleView = (View) XposedHelpers.getObjectField(param.thisObject, "mHandleView");
						
						// seta o alpha
						if (mHandleView.getAlpha() != mHandleBarAlpha)
							mHandleView.setAlpha(mHandleBarAlpha);
					
						return;
						
					} catch (NoSuchFieldError e) {}
					
				}
			});
			
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	public static void updatePreferences(XSharedPreferences prefs) {
		
		// atualiza
		mBlurScale = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_DEFAULT));
		mBlurRadius = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_DEFAULT));
		mBlurFadeInOut = prefs.getBoolean(BlurSettings_Fragment.BLURRED_FADE_IN_OUT_PREFERENCE_KEY, BlurSettings_Fragment.BLURRED_FADE_IN_OUT_PREFERENCE_DEFAULT);
		mHandleBarAlpha = Float.parseFloat(prefs.getString(BlurSettings_Fragment.DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_KEY, BlurSettings_Fragment.DRAG_HANDLE_TRANSLUCENCY_PREFERENCE_DEFAULT));
		mBlurDarkColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_DARK_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_DARK_COLOR_PREFERENCE_DEFAULT);
		mBlurMixedColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_MIXED_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_MIXED_COLOR_PREFERENCE_DEFAULT);
		mBlurLightColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_LIGHT_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_LIGHT_COLOR_PREFERENCE_DEFAULT);
		mBlurredStatusBarExpandedEnabled = prefs.getBoolean(BlurSettings_Fragment.STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.STATUS_BAR_EXPANDED_ENABLED_PREFERENCE_DEFAULT);
		
	}
	
	public static void createBlurredView() {
		
		Context context = mNotificationPanelView.getContext();
		
		// inicia
		mBlurUtils = new BlurUtils(context);
		
		// cria os views
		mBlurredView = new FrameLayout(context);
		mInnerBlurredView = new FrameLayout(context);
		
		// parâmetros
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		
		// insere o mInnerBlurredView no mBlurredView
		mBlurredView.addView(mInnerBlurredView, lp);
		
    	// insere o mBlurredView no mNotificationPanelView (posição 0)
    	((ViewGroup) mNotificationPanelView).addView(mBlurredView, 0, lp);
    	
    	// layout
    	mNotificationPanelView.requestLayout();
    	
    	// seta o tag de: pronto para receber o blur
    	mBlurredView.setTag("ready_to_blur");
    	
	}
	
	public static void handleFadeInOut() {
		
		// blur
		if (mBlurredStatusBarExpandedEnabled) {
     	
			// erro ao criar o bitmap ?
			if (mBlurredView.getTag().toString().equals("error"))
				return;
			
			// dimensões
     		int panelHeight = mNotificationPanelView.getMeasuredHeight();
     		int viewHeight = mBlurredView.getMeasuredHeight();
				
			// alpha
			float alpha = -1f;
			
			// fade in-out ?
			if (mBlurFadeInOut) {
				
				// calcula o alpha (regra de 3)
				alpha = viewHeight > 0 && panelHeight > 0 ? (float) panelHeight / viewHeight : 0f;
				
			}
			
			// seta o alpha
			mBlurredView.setAlpha(alpha >= 0f && alpha <= 1f ? alpha : 1f);
			
		}
	}
	
	public static void startBlurTask() {
		
		// callback
		BlurTask.setBlurTaskCallback(new BlurTaskCallback() {
			
			@Override
			public void blurTaskDone(Bitmap blurredBitmap) {
				
				if (blurredBitmap != null) {
					
					// -------------------------
					// bitmap criado com sucesso
					// -------------------------
					
					// fixa o width do mBlurredView
					if (mBlurredView.getLayoutParams().width != mNotificationPanelView.getWidth()) {
						
						mBlurredView.getLayoutParams().width = mNotificationPanelView.getWidth();
						mBlurredView.requestLayout();
						
					}
					
					// corrige as dimensões do mInnerBlurredView 
					int[] dimens = BlurTask.getRealScreenDimensions();
					if (mNotificationPanelView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
						mInnerBlurredView.getLayoutParams().width = dimens[0];
					mInnerBlurredView.getLayoutParams().height = dimens[1];
					mInnerBlurredView.requestLayout();
					
					// cria o drawable com o filtro de cor
					BitmapDrawable drawable = new BitmapDrawable(blurredBitmap);
					drawable.setColorFilter(mColorFilter);
					
					// seta o drawable
					mInnerBlurredView.setBackground(drawable);
					
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
		BlurTask.setBlurEngine(BlurEngine.RenderScriptBlur);
		
		// blur
		new BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		
	}
	
	public static void recycle() {
	
		// limpa o background
		mBlurredView.setBackground(null);
		
		// limpa e recicla
		if (mInnerBlurredView != null &&
			mInnerBlurredView.getBackground() != null) {
	
			// bitmap ?
			if (mInnerBlurredView.getBackground() instanceof BitmapDrawable) {
				
				// recicla
			    Bitmap bitmap = ((BitmapDrawable) mInnerBlurredView.getBackground()).getBitmap();
			    if (bitmap != null) {
			    	
			    	bitmap.recycle();
			    	bitmap = null;
			    	
			    }
			}
			
			// limpa
			mInnerBlurredView.setBackground(null);
			
		}
		
		// recicla ?
		if (mBlurredScreenBitmap != null) {
			
			mBlurredScreenBitmap.recycle();
			mBlurredScreenBitmap = null;
			
		}
		
		// seta o tag de: pronto para receber o blur
		mBlurredView.setTag("ready_to_blur");
		
	}
	
	public static class BlurTask extends AsyncTask<Void, Void, Bitmap> {
		
		private static int[] mScreenDimens;
		private static BlurEngine mBlurEngine;
		private static BlurTaskCallback mCallback;
		
		//private Bitmap mSmallBitmap;
		private Bitmap mScreenBitmap;
			
		public static void setBlurEngine(BlurEngine blurEngine) {
			
			mBlurEngine = blurEngine;
			
		}
		
		public static void setBlurTaskCallback(BlurTaskCallback callBack) {
			
		    mCallback = callBack;
		    
		}
		
		public static int[] getRealScreenDimensions() {
			
			return mScreenDimens;
			
		}
		
		@Override
		protected void onPreExecute() {
			
			Context context = mNotificationPanelView.getContext(); 
			
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
					
				return mScreenBitmap;
				
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