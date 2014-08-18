package com.serajr.blurred.system.ui;

import com.serajr.blurred.system.ui.hooks.SystemUI_BaseStatusBar;
import com.serajr.blurred.system.ui.hooks.SystemUI_NotificationPanelView;
import com.serajr.blurred.system.ui.hooks.SystemUI_PanelView;
import com.serajr.blurred.system.ui.hooks.SystemUI_PhoneStatusBar;
import com.serajr.blurred.system.ui.hooks.SystemUI_RecentsPanelView;
import com.serajr.blurred.system.ui.hooks.SystemUI_TranslucentBackground;

import android.content.res.XModuleResources;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Xposed implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
	
	public static String MODULE_PACKAGE_NAME = "com.serajr.blurred.system.ui";
	public static String ANDROID_PACKAGE_NAME = "android";
	public static String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
	
	private static String mModulePath;
	private static ClassLoader mClassLoader;
	private static XSharedPreferences mXSharedPreferences;
	private static XModuleResources mXModuleResources;
	private static InitPackageResourcesParam mInitPackageResourcesParam;
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		
		mModulePath = startupParam.modulePath;
		mXSharedPreferences = new XSharedPreferences(MODULE_PACKAGE_NAME);
		
		// recarregam as preferências
		//mXSharedPreferences.reload();
		
	}
	
	@Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
    	
    	mClassLoader = lpparam.classLoader;
    	
    	// SYSTEM UI
    	if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE_NAME)) {
    		
    		// setam os class loaderes parentes
    		setParentClassLoaders(lpparam);
    	
    		// recarregam as preferências
    		mXSharedPreferences.reload();
    		
    		// hooks
    		SystemUI_PhoneStatusBar.hook();
    		SystemUI_BaseStatusBar.hook();
    		SystemUI_PanelView.hook();
    		SystemUI_NotificationPanelView.hook();
    		SystemUI_RecentsPanelView.hook();
    		SystemUI_TranslucentBackground.hook(true, false);
    		
    	}
    }
    
    @Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
    	
		mInitPackageResourcesParam = resparam;
    	mXModuleResources = XModuleResources.createInstance(mModulePath, resparam.res);
    	
    	// SYSTEM UI
    	if (resparam.packageName.equals(SYSTEM_UI_PACKAGE_NAME)) {
    		
    		// recarregam as preferências
    		mXSharedPreferences.reload();
    		
    		// hooks
    		SystemUI_TranslucentBackground.hook(false, true);
    		
    	}
    }
    
    private void setParentClassLoaders(LoadPackageParam lpparam) throws Throwable {
    	
    	// todos os classloaders
    	ClassLoader packge = lpparam.classLoader;
    	ClassLoader module = getClass().getClassLoader();
    	ClassLoader xposed = module.getParent();
    	
    	// package classloader parente é: xposed classloader 
    	XposedHelpers.setObjectField(packge, "parent", xposed);
    	
    	// módulo parente classcloader é: package classloader
    	XposedHelpers.setObjectField(module, "parent", packge);
    	
    }
    
    public static ClassLoader getXposedClassLoader() {
    	
    	return mClassLoader;
    	
    }
    
    public static XSharedPreferences getXposedXSharedPreferences() {
    	
    	return mXSharedPreferences;
    	
    }
    
    public static String getXposedModulePath() {
    	
    	return mModulePath;
    	
    }
    
    public static XModuleResources getXposedModuleResources() {
    	
    	return mXModuleResources;
    	
    }
    
    public static InitPackageResourcesParam getXposedInitPackageResourcesParam() {
    	
    	return mInitPackageResourcesParam;
    	
    }
}