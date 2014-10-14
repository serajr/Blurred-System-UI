package com.serajr.utils;

import java.io.File;
import java.util.ArrayList;

import android.view.View;
import android.view.ViewGroup;

public class Utils {
		
	public static boolean isSonyXperiaRom() {
		
		File file = new File("/system/framework/SemcGenericUxpRes.apk");
		return file.exists();
		
	}
	
	public static int getAndroidAPILevel() {
		
		// retorna o número da API en que o programa está rodando
		return android.os.Build.VERSION.SDK_INT;
		
	}
	
	public static ArrayList<View> getAllChildrenViews(View view) {

	    if (!(view instanceof ViewGroup)) {
	    	
	        ArrayList<View> viewArrayList = new ArrayList<View>();
	        viewArrayList.add(view);
	        
	        return viewArrayList;
	        
	    }

	    ArrayList<View> result = new ArrayList<View>();

	    ViewGroup viewGroup = (ViewGroup) view;
	    for (int i = 0; i < viewGroup.getChildCount(); i++) {

	        View child = viewGroup.getChildAt(i);

	        ArrayList<View> viewArrayList = new ArrayList<View>();
	        viewArrayList.add(view);
	        viewArrayList.addAll(getAllChildrenViews(child));

	        result.addAll(viewArrayList);
	        
	    }
	    
	    return result;
	    
	}
}