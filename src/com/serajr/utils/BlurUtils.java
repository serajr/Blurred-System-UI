package com.serajr.utils;

import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import de.robv.android.xposed.XSharedPreferences;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BlurUtils {
	
	private static Context mContext;
	
	public static void init(Context context) {
		
		// guarda
		mContext = context;
		
		// inicia
		if (Utils.getAndroidAPILevel() >= 17)
			BlurScript.init();
		
	}
	
	public static class BlurTask extends AsyncTask<Void, Void, Bitmap> {
		
		public static int mColorFilter;
		
		private static int mScale;
		private static int mRadius;
		private static BlurTaskCallback mCallback;
		private static boolean mOriginalBitmapSize;
		
		private int[] mScreenDimens;
		private Bitmap mScreenBitmap;
		
		public static void setOnBlurTaskCallback(BlurTaskCallback callBack, boolean originalBitmapSize) {
			
		    mCallback = callBack;
		    mOriginalBitmapSize = originalBitmapSize;
		    
		}
		
		public static void updatePreferences(XSharedPreferences prefs) {
			
			// atualiza
			mScale = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_SCALE_PREFERENCE_DEFAULT));
			mRadius = Integer.parseInt(prefs.getString(BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_RADIUS_PREFERENCE_DEFAULT));
			mColorFilter = prefs.getInt(BlurSettings_Fragment.BLUR_COLOR_PREFERENCE_KEY, BlurSettings_Fragment.BLUR_COLOR_PREFERENCE_DEFAULT);
			
		}
			
		@Override
		protected void onPreExecute() {
			
			// obtém a screenshot
			mScreenBitmap = DisplayUtils.takeSurfaceScreenshot(mContext);
			
			// obtém o tamamho real da tela
			mScreenDimens = DisplayUtils.getRealScreenDimensions(mContext);
			
		}
		
		@Override
		protected Bitmap doInBackground(Void... arg0) {
			
			// continua ?
			if (mScreenBitmap == null)
				return null;
			
			// diminui o bitmap
			Bitmap scaled = Bitmap.createScaledBitmap(mScreenBitmap, mScreenDimens[0] / mScale, mScreenDimens[1] / mScale, true);
			
			// blur
			if (Utils.getAndroidAPILevel() >= 17) {
			
				// >= 4.2.2
				scaled = BlurScript.renderScriptBlur(scaled, mRadius);
			
			} else {
				
				// <= 4.1.2
				scaled = BlurScript.stackBlur(scaled, mRadius);
				
			}
			
			// aumenta o bitmap ?
			if (mOriginalBitmapSize)
				scaled = Bitmap.createScaledBitmap(scaled, mScreenDimens[0], mScreenDimens[1], true);
			
			return scaled;
			
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			
			if (bitmap != null) {
				
				// -----------------------------
				// bitmap criado com sucesso !!!
				// -----------------------------
				
				// aplica o filtro de cor
				Paint paint = new Paint();
			    paint.setColorFilter(new PorterDuffColorFilter(mColorFilter, PorterDuff.Mode.MULTIPLY));   
			    Canvas canvas = new Canvas(bitmap);
			    canvas.drawBitmap(bitmap, 0, 0, paint);
				
				// callback
				mCallback.blurTaskDone(bitmap);
				
				// recicla e anula o bitmap original
				mScreenBitmap.recycle();
				mScreenBitmap = null;
				
			} else {
				
				// --------------------------
				// erro ao criar o bitmap !!!
				// --------------------------
					
				// callback
				mCallback.blurTaskDone(null);
				
			}
		}
		
		// interface
		public static abstract interface BlurTaskCallback {
			
		    public abstract void blurTaskDone(Bitmap blurredBitmap);
		    
		}
	}
	
	@SuppressLint("NewApi")
	public static class BlurScript {

		private static RenderScript mRenderScript;
	    private static ScriptIntrinsicBlur mScriptIntrinsicBlur;
	    
		public static void init() {
	    	
	    	// inicia os campos
    		mRenderScript = RenderScript.create(mContext);
    		mScriptIntrinsicBlur = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4 (mRenderScript));
	    	
	    }
	    
	    public static Bitmap renderScriptBlur(Bitmap bitmap, int radius) {
	    	
	    	// não iniciado ?
			if (mRenderScript == null)
				return null;
						
			Allocation input = Allocation.createFromBitmap(mRenderScript, bitmap);
			Allocation output = Allocation.createTyped(mRenderScript, input.getType());
			mScriptIntrinsicBlur.setRadius(radius);
			mScriptIntrinsicBlur.setInput(input);
			mScriptIntrinsicBlur.forEach(output);
			output.copyTo(bitmap);
			
			return bitmap;
			
	    }
		
		public static Bitmap stackBlur(Bitmap bitmap, int radius) {
	        
			// raio válido ?
	        if (radius < 1)
	        	return (null);
	        
	        int w = bitmap.getWidth();
	        int h = bitmap.getHeight();

	        int[] pix = new int[w * h];
	        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

	        int wm = w - 1;
	        int hm = h - 1;
	        int wh = w * h;
	        int div = radius + radius + 1;

	        int r[] = new int[wh];
	        int g[] = new int[wh];
	        int b[] = new int[wh];
	        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
	        int vmin[] = new int[Math.max(w, h)];

	        int divsum = (div + 1) >> 1;
	        divsum *= divsum;
	        int dv[] = new int[256 * divsum];
	        
	        for (i = 0; i < 256 * divsum; i++) {
	        	
	            dv[i] = (i / divsum);
	            
	        }

	        yw = yi = 0;

	        int[][] stack = new int[div][3];
	        int stackpointer;
	        int stackstart;
	        int[] sir;
	        int rbs;
	        int r1 = radius + 1;
	        int routsum, goutsum, boutsum;
	        int rinsum, ginsum, binsum;

	        for (y = 0; y < h; y++) {
	        	
	            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
	            for (i = -radius; i <= radius; i++) {
	            	
	                p = pix[yi + Math.min(wm, Math.max(i, 0))];
	                sir = stack[i + radius];
	                sir[0] = (p & 0xff0000) >> 16;
	                sir[1] = (p & 0x00ff00) >> 8;
	                sir[2] = (p & 0x0000ff);
	                rbs = r1 - Math.abs(i);
	                rsum += sir[0] * rbs;
	                gsum += sir[1] * rbs;
	                bsum += sir[2] * rbs;
	                
	                if (i > 0) {
	                	
	                    rinsum += sir[0];
	                    ginsum += sir[1];
	                    binsum += sir[2];
	                    
	                } else {
	                	
	                    routsum += sir[0];
	                    goutsum += sir[1];
	                    boutsum += sir[2];
	                    
	                }
	            }
	            
	            stackpointer = radius;

	            for (x = 0; x < w; x++) {

	                r[yi] = dv[rsum];
	                g[yi] = dv[gsum];
	                b[yi] = dv[bsum];

	                rsum -= routsum;
	                gsum -= goutsum;
	                bsum -= boutsum;

	                stackstart = stackpointer - radius + div;
	                sir = stack[stackstart % div];

	                routsum -= sir[0];
	                goutsum -= sir[1];
	                boutsum -= sir[2];
	                
	                if (y == 0) {
	                	
	                    vmin[x] = Math.min(x + radius + 1, wm);
	                    
	                }
	                
	                p = pix[yw + vmin[x]];

	                sir[0] = (p & 0xff0000) >> 16;
	                sir[1] = (p & 0x00ff00) >> 8;
	                sir[2] = (p & 0x0000ff);

	                rinsum += sir[0];
	                ginsum += sir[1];
	                binsum += sir[2];

	                rsum += rinsum;
	                gsum += ginsum;
	                bsum += binsum;

	                stackpointer = (stackpointer + 1) % div;
	                sir = stack[(stackpointer) % div];

	                routsum += sir[0];
	                goutsum += sir[1];
	                boutsum += sir[2];

	                rinsum -= sir[0];
	                ginsum -= sir[1];
	                binsum -= sir[2];

	                yi++;
	                
	            }
	            
	            yw += w;
	            
	        }
	        
	        for (x = 0; x < w; x++) {
	        	
	            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
	            yp = -radius * w;
	            
	            for (i = -radius; i <= radius; i++) {
	            	
	                yi = Math.max(0, yp) + x;

	                sir = stack[i + radius];

	                sir[0] = r[yi];
	                sir[1] = g[yi];
	                sir[2] = b[yi];

	                rbs = r1 - Math.abs(i);

	                rsum += r[yi] * rbs;
	                gsum += g[yi] * rbs;
	                bsum += b[yi] * rbs;

	                if (i > 0) {
	                	
	                    rinsum += sir[0];
	                    ginsum += sir[1];
	                    binsum += sir[2];
	                    
	                } else {
	                	
	                    routsum += sir[0];
	                    goutsum += sir[1];
	                    boutsum += sir[2];
	                    
	                }

	                if (i < hm) {
	                	
	                    yp += w;
	                    
	                }
	            }
	            
	            yi = x;
	            stackpointer = radius;
	            
	            for (y = 0; y < h; y++) {
	                
	                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

	                rsum -= routsum;
	                gsum -= goutsum;
	                bsum -= boutsum;

	                stackstart = stackpointer - radius + div;
	                sir = stack[stackstart % div];

	                routsum -= sir[0];
	                goutsum -= sir[1];
	                boutsum -= sir[2];

	                if (x == 0) {
	                	
	                    vmin[y] = Math.min(y + r1, hm) * w;
	                    
	                }
	                
	                p = x + vmin[y];

	                sir[0] = r[p];
	                sir[1] = g[p];
	                sir[2] = b[p];

	                rinsum += sir[0];
	                ginsum += sir[1];
	                binsum += sir[2];

	                rsum += rinsum;
	                gsum += ginsum;
	                bsum += binsum;

	                stackpointer = (stackpointer + 1) % div;
	                sir = stack[stackpointer];

	                routsum += sir[0];
	                goutsum += sir[1];
	                boutsum += sir[2];

	                rinsum -= sir[0];
	                ginsum -= sir[1];
	                binsum -= sir[2];

	                yi += w;
	                
	            }
	        }

	        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
	        
	        return bitmap;
	        
	    }
	}
}
