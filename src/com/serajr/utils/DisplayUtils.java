package com.serajr.utils;

import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

public class DisplayUtils {
	
	private static int mBottomOffset = 0;
	private static int mRightOffset = 0;
	private static int mTopOffset = 0;
	//private static Bitmap mPortraitScreenBitmap;
	//private static Bitmap mLandscapeScreenBitmap;
	
	public static void setFullScreenActivity(Window window, View view) {
		
		int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | 
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		
		// setam os flags
		window.getDecorView().setSystemUiVisibility(flags);
		
		// setam as margens
		view.setPadding(0, mTopOffset, mRightOffset, mBottomOffset);
		view.requestLayout();
		
	}
	
	public static boolean deviceHasOnScreenButtons(Context context) {
		
		return !ViewConfiguration.get(context).hasPermanentMenuKey();
		
	}
	
	public static int getActionBarHeight(Context context) {
		
		TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
        new int[] { android.R.attr.actionBarSize });
		int actionBarSize = (int) styledAttributes.getDimension(0, 0);
		styledAttributes.recycle();
		
		return actionBarSize;
		
	}
	
	public static void updateConfiguration(Context context, Display display, int actionBarHeight) {
		
		Resources res = context.getResources();
		boolean landscape = display.getRotation() == Configuration.ORIENTATION_LANDSCAPE ? true : false;
		
		// top
		int statusBarHeightResId = res.getIdentifier("status_bar_height", "dimen", "android");
		mTopOffset = statusBarHeightResId > 0 ? res.getDimensionPixelSize(statusBarHeightResId) : 0;
		
        // adiciona a action bar ao top
        mTopOffset = mTopOffset + actionBarHeight;
        
        // bottom
        int navigationBarHeightResId = res.getIdentifier("navigation_bar_height", "dimen", "android");
        int bottomOffset = navigationBarHeightResId > 0 ? res.getDimensionPixelSize(navigationBarHeightResId) : 0;
        
        // não tem os botões na tela !!!
        if (!deviceHasOnScreenButtons(context))
        	bottomOffset = 0;
        
        // landscape ?
        if (landscape) {
        	
        	mRightOffset = 0;
        	mBottomOffset = bottomOffset;
            return;
            
        }
        
        Point point = new Point();
        Point point1 = new Point();
        display.getSize(point);
        display.getRealSize(point1);
        
        // inverte ?
        if (point.x < point1.x) {
        	
        	mRightOffset = bottomOffset;
        	mBottomOffset = 0;
            return;
            
        } else {
        	
        	mRightOffset = 0;
        	mBottomOffset = bottomOffset;
            return;
            
        }
    }
	
	public static int[] getRealScreenDimensions(Context context) {
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getRealMetrics(metrics);
		        
		return new int[] { metrics.widthPixels, metrics.heightPixels };
		
	}
	
	public static Bitmap takeSurfaceScreenshot(Context context) {
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		Matrix displayMatrix = new Matrix();
		
		Bitmap screenBitmap = null;
		
		display.getRealMetrics(metrics);
        float[] dims = { metrics.widthPixels, metrics.heightPixels };
        float degrees = getDegreesForRotation(display.getRotation());
        boolean requiresRotation = (degrees > 0);
        
        if (requiresRotation) {
        	
            // Get the dimensions of the device in its native orientation
        	displayMatrix.reset();
        	displayMatrix.preRotate(-degrees);
        	displayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
            
        }
        
        if (Utils.getAndroidAPILevel() >= 18) {
        	
        	// >= 4.3
        	screenBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);
        	
        } else {
        	
        	// <= 4.2.2
        	try {
        	
        		// reflection
        		Class<?> Surface = Class.forName("android.view.Surface");
        		screenBitmap = (Bitmap) XposedHelpers.callStaticMethod(Surface, "screenshot", (int) dims[0], (int) dims[1]); 
        	    
        	} catch (ClassNotFoundException e) {
        		
        	    e.printStackTrace();
        		
        	}
        }
        
        // possível app que precisa de segurança rodando, ou
        // o context não tem previlégios suficientes par tal 
        if (screenBitmap == null) {
        	
        	// informa e retorna
        	Log.i("serajr_blurred_system_ui", "Cannot take surface screenshot! Skipping blur feature!!");
        	return null;
        	
        }
        
        if (requiresRotation) {
        	
            // Rotate the screenshot to the current orientation
            Bitmap ss = Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
            c.rotate(360f - degrees);
            c.translate(-dims[0] / 2, -dims[1] / 2);
            c.drawBitmap(screenBitmap, 0, 0, null);
            c.setBitmap(null);
            screenBitmap = ss;
            
        }
        
        // Optimizations
        screenBitmap.setHasAlpha(false);
        screenBitmap.prepareToDraw();
        
        // retorna
        return screenBitmap;
        
    }
	
	private static float getDegreesForRotation(int value) {
		
        switch (value) {
        
	        case Surface.ROTATION_90:
	            return 90f;
	            
	        case Surface.ROTATION_180:
	            return 180f;
	            
	        case Surface.ROTATION_270:
	            return 270f;
	            
        }
        
        return 0f;
        
    }
}