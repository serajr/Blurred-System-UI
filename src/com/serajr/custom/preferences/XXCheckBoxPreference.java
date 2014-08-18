package com.serajr.custom.preferences;

import com.serajr.blurred.system.ui.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;

public class XXCheckBoxPreference extends CheckBoxPreference {
	
	public XXCheckBoxPreference(Context context) {
    	super(context);
        
    }
	
	public XXCheckBoxPreference(Context context, AttributeSet attrs) {
    	super(context, attrs);
        
    }
	
	public XXCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
        
    }
	
	@Override
    protected void onBindView(View view) {
		super.onBindView(view);
		
        Context context = view.getContext();
        
        // obtém o drawable do attributo (0)
        int[] attrs = new int[] { android.R.attr.selectableItemBackground };
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs);
        Drawable bg = typedArray.getDrawable(0);
        typedArray.recycle();
        
        // cria
        Drawable[] layers = new Drawable[2];
        layers[0] = bg;
        layers[1] = context.getResources().getDrawable(R.drawable.reboot_top_right);
        
        // seta
        setBackgroundAndKeepPadding(view, new LayerDrawable(layers));
        
	}
	
	private void setBackgroundAndKeepPadding(View view, Drawable background) {
		
	    Rect drawablePadding = new Rect();
	    
	    int top = view.getPaddingTop() + drawablePadding.top;
	    int left = view.getPaddingLeft() + drawablePadding.left;
	    int right = view.getPaddingRight() + drawablePadding.right;
	    int bottom = view.getPaddingBottom() + drawablePadding.bottom;

	    view.setBackgroundDrawable(background);
	    view.setPadding(left, top, right, bottom);
	    
	}
}