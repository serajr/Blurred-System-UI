package com.serajr.blurred.system.ui.hooks;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.serajr.blurred.system.ui.R;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_BaseStatusBar {
	
	public static boolean mTranslucentNotifications;
	protected static NotificationData mNotificationData;
	
	@SuppressLint("NewApi")
	public static void hook() {
		
		try {
			
			// constructor
			XposedBridge.hookAllConstructors(BaseStatusBar.class, new XC_MethodHook() {
				
				@Override
	            		protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// guarda
					mNotificationData = (NotificationData) XposedHelpers.getObjectField(param.thisObject, "mNotificationData");
					
				}
			});
			
			// toggleRecentApps
			XposedHelpers.findAndHookMethod(BaseStatusBar.class, "toggleRecentApps", new XC_MethodHook() {
				
				@Override
	            		protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// blur
					SystemUI_RecentsPanelView.blur();
					
				}
			});
			
			// acha o método exato
			Method inflateViews;
			try {
				
				// 2 parâmetros - Entry.class, ViewGroup.class
				inflateViews = XposedHelpers.findMethodExact(BaseStatusBar.class, "inflateViews", Entry.class, ViewGroup.class);
				
			} catch (NoSuchMethodError e) {
				
				// erro, não encontrou com os 2 parâmetros acima !
				
				// 3 parâmetros - Entry.class, ViewGroup.class, int.class
				inflateViews = XposedHelpers.findMethodExact(BaseStatusBar.class, "inflateViews", Entry.class, ViewGroup.class, int.class);
				
			}
			
			// inflateViews
			XposedBridge.hookMethod(inflateViews, new XC_MethodHook() {
				
				@Override
	            		protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// notificações transparentes ?
					if (!mTranslucentNotifications)
						return;
				
					// heads up ?
					ViewGroup parent = (ViewGroup) param.args[1];
					if (SystemUI_HeadsUpNotificationView.mContentHolder != null) {
						
						// é ?
						if (parent == SystemUI_HeadsUpNotificationView.mContentHolder)
							return;
					
					}
					
					// obtém os dados da notificação
					Entry entry = (Entry) param.args[0];
		            
					// seta o fundo transparente
					if (entry != null)
						setTranslucentNotificationBackground(entry);
					
				}
			});
			
			// >= 4.4
			if (Utils.getAndroidAPILevel() >= 19) {
				
				// updateNotificationViews
				XposedHelpers.findAndHookMethod(BaseStatusBar.class, "updateNotificationViews", Entry.class, StatusBarNotification.class, new XC_MethodHook() {
					
					@Override
		            		protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						
						// notificações transparentes ?
						if (!mTranslucentNotifications)
							return;
					
						// obtém os dados da notificação
						Entry entry = (Entry) param.args[0];
			            
						// seta o fundo transparente
						if (entry != null)
							setTranslucentNotificationBackground(entry);
						
					}
				});	
				
			// <= 4.3
			} else {
				
				// updateNotification
				XposedHelpers.findAndHookMethod(BaseStatusBar.class, "updateNotification",
						IBinder.class, 
						Utils.getAndroidAPILevel() == 18
							// = 4.3
							? StatusBarNotification.class
							// <= 4.2
							: XposedHelpers.findClass("com.android.internal.statusbar.StatusBarNotification", Xposed.getXposedClassLoader()), 
						new XC_MethodHook() {
					
					@Override
		            		protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						
						// notificações transparentes ?
						if (!mTranslucentNotifications)
							return;
					
						// obtém os dados da notificação
						Entry entry = mNotificationData.findByKey((IBinder) param.args[0]);
			            
						// seta o fundo transparente
						if (entry != null)
							setTranslucentNotificationBackground(entry);
						
					}
				});
			}
					
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	public static void updatePreferences(XSharedPreferences prefs) {
		
		// atualiza
		mTranslucentNotifications = prefs.getBoolean(BlurSettings_Fragment.TRANSLUCENT_NOTIFICATIONS_PREFERENCE_KEY, BlurSettings_Fragment.TRANSLUCENT_NOTIFICATIONS_PREFERENCE_DEFAULT);
		
		// atualiza tb as notificações visíveis (se necessário)
		updateVisibleNotificationsBackground();
		
	}
		
	private static void updateVisibleNotificationsBackground() {
		
		// continua ?
		if (mNotificationData == null)
			return;
		
		// passa por todas as notificações visíveis
		for (int i = 0; i < mNotificationData.size(); i++) {
			
			// obtém os dados da notificação
			Entry entry = mNotificationData.get(i);
            
			// seta o fundo transparente
			if (entry != null)
				setTranslucentNotificationBackground(entry);
			
        	}
	}
	
	private static void setTranslucentNotificationBackground(Entry entry) {
		
		// vista root da notificação - somente remove os backgrounds !!
		View row = Utils.getAndroidAPILevel() >= 19
				// >= 4.4
				// ExpandableNotificationRow row
				? entry.row
				// <= 4.3
				// View row 
				: (View) XposedHelpers.getObjectField(entry, "row");
		
		// row
		if (row != null)
			setTranslucentNotificationBackground(row, false);
		
		// content
		if (entry.content != null)
			setTranslucentNotificationBackground(entry.content, true);
		
		// expanded
		if (entry.expanded != null)
			setTranslucentNotificationBackground(entry.expanded, true);
		
		// >= 4.4
		if (Utils.getAndroidAPILevel() >= 19) {
		
			// expandedBig
			if (entry.getBigContentView() != null)
				setTranslucentNotificationBackground(entry.getBigContentView(), true);
				
		// <= 4.3
		} else {
			
			// expandedLarge
			View expandedLarge = (View) XposedHelpers.callMethod(entry, "getLargeView");
			if (expandedLarge != null)
				setTranslucentNotificationBackground(expandedLarge, true);
			
		}
	}
	
	private static void setTranslucentNotificationBackground(View view, boolean translucent) {
		
		// notificações transparentes ?
		if (mTranslucentNotifications) {
			
			// obtém o resources
			Resources res = view.getResources();
			
			// obtém todos os views dessa notificação
			ArrayList<View> children = Utils.getAllChildrenViews(view);
			for (View child : children) {
		    	
			    	// continua ?
			    	if (child != null) { 
			    		
			    		// obtém o id
			    		int resId = child.getId();
			    		
			    		// id válido ?
			    		if (resId != 0) {
			    			
			    			try {
			    		
			    				// acha o nome do id
			    				String nameResIs = resId != 0 ? res.getResourceEntryName(resId) : "";
					    		
					    		// existe um background ?
					    		if (child.getBackground() != null) {
					    			
					    			//Log.d("child_id_name", nameResIs + " | " + child.getClass().toString());
					    			
					    			// despresa se se for um desses id's...
					    			if (nameResIs.contains("icon") ||
					    				nameResIs.contains("glow") ||
					    				nameResIs.contains("divider") ||
					    				// heads up notifications background !!
					    				nameResIs.contains("content_holder"))
					    				continue;
					    				
				    				// remove o fundo e limpa o cache
				    				child.setBackground(null);
				    				child.destroyDrawingCache();
				    				
				    				// resedenha
				    				child.invalidate();
					    			
					    		}
			    				
		    				} catch (NotFoundException e) {
		    				
		    					// erro !!!
		    					continue;
		    				
		    				}
		    			}
		    		}
			}
			
		    	// seta o background transparente
			if (translucent)
				view.setBackground(Xposed.getXposedModuleResources().getDrawable(R.drawable.notification_bg));
			
		} else {
			
			// seta o background padrão do framework
			if (translucent)
				view.setBackgroundResource(com.android.internal.R.drawable.notification_bg);
			
		}

		// resedenha
		if (translucent) {
			
			view.invalidate();
			view.setPressed(false);
			
		}
	}
}
