package io.dcloud.feature.jPush;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.jpush.android.api.BasicPushNotificationBuilder;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.TagAliasCallback;
import cn.jpush.android.data.JPushLocalNotification;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.util.JSUtil;

public class JPushService extends StandardFeature {
	public static final String TAG = "JPushService";

	public static String notificationTitle;
	public static String notificationAlert;
	public static Map<String, Object> notificationExtras = new HashMap<String, Object>();

	public static String openNotificationTitle;
	public static String openNotificationAlert;
	public static Map<String, Object> openNotificationExtras = new HashMap<String, Object>();
	
	private static IWebview mIWebview;
    private static boolean shouldCacheMsg = false;
	
	@Override
	public void onStart(Context context, Bundle arg1, String[] arg2) {
		Log.d("Jimmy", "onStart");
		JPushInterface.init(context);
		JPushInterface.setDebugMode(false);
	}
    
	// 需要手动调用
	public void init(IWebview webview, JSONArray data) {
		Log.d("Jimmy", "init");
		mIWebview = webview;
        //如果同时缓存了打开事件 openNotificationAlert 和消息事件 notificationAlert，
		//只向 UI 发打开事件，这样做是为了和 iOS 统一。
        if (openNotificationAlert != null) {
            notificationAlert = null;
            transmitNotificationOpen(openNotificationTitle,
            		openNotificationAlert, openNotificationExtras);
        }
        if (notificationAlert != null) {
            transmitNotificationReceive(notificationTitle,
            		notificationAlert, notificationExtras);
        }
	}
	
	@Override
	public void onPause() {
		Log.d("Jimmy", "onPause");
		super.onPause();
		shouldCacheMsg = true;
	}
	
	@Override
	public void onResume() {
		Log.d("Jimmy", "onResume");
		super.onResume();
		shouldCacheMsg = false;
        if (openNotificationAlert != null) {
            notificationAlert = null;
            transmitNotificationOpen(openNotificationTitle,
            		openNotificationAlert, openNotificationExtras);
        }
        if (notificationAlert != null) {
            transmitNotificationReceive(notificationTitle,
            		notificationAlert, notificationExtras);
        }
	}
	
	public void stopPush(IWebview webview, JSONArray data) {
		JPushInterface.stopPush(webview.getContext());
	}
	
	public void resumePush(IWebview webview, JSONArray data) {
		JPushInterface.resumePush(webview.getContext());
	}
	
	public void isPushStopped(IWebview webview, JSONArray data) {
		try {
			String callbackId = data.getString(0);
			boolean isPushStopped = JPushInterface.isPushStopped(webview.getContext());
			if (isPushStopped) {
				JSUtil.execCallback(webview, callbackId, 1, JSUtil.OK, false);
			} else {
				JSUtil.execCallback(webview, callbackId, 0, JSUtil.OK, false);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static void transmitMessageReceive(String msg, 
			Map<String, Object> extras) {
        JSONObject data = getMessageObject(msg, extras);
        String format = "plus.Push.receiveMessageInAndroidCallback(%s);";
        final String js = String.format(format, data.toString());
        mIWebview.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIWebview.loadUrl("javascript:" + js);
            }
        });
	}

    public static void transmitNotificationOpen(String title, String alert,
    		Map<String, Object> extras) {
        if (shouldCacheMsg) {
            return;
        }
        JSONObject data = getNotificationObject(title, alert, extras);
        String format = "plus.Push.openNotificationInAndroidCallback(%s);";
        final String js = String.format(format, data.toString());
        mIWebview.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIWebview.loadUrl("javascript:" + js);
            }
        });
        openNotificationTitle = null;
        openNotificationAlert = null;
    }

    public static void transmitNotificationReceive(String title, String alert,
    		Map<String, Object> extras) {
        JSONObject data = getNotificationObject(title, alert, extras);
        String format = "plus.Push.receiveNotificationInAndroidCallback(%s);";
        final String js = String.format(format, data.toString());
        mIWebview.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIWebview.loadUrl("javascript:" + js);
            }
        });
        notificationTitle = null;
        notificationAlert = null;
    }
	
	public void getRegistrationID(IWebview webview, JSONArray data) {
		try {
			String callbackId = data.getString(0);
			String regId = JPushInterface.getRegistrationID(webview.getContext());
			JSUtil.execCallback(webview, callbackId, regId, JSUtil.OK, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void addLocalNotification(IWebview webview, JSONArray data) {
		try {
	        int builderId = data.getInt(1);
	        String content = data.getString(2);
	        String title = data.getString(3);
	        int notificationId = data.getInt(4);
	        int broadcastTime = data.getInt(5);
	        String extrasStr = data.isNull(6) ? "" : data.getString(6);
	        JSONObject extras = new JSONObject();
	        if (!TextUtils.isEmpty(extrasStr)) {
	        	extras = new JSONObject(extrasStr);
			}
	        
	        JPushLocalNotification jLocalNoti = new JPushLocalNotification();
	        jLocalNoti.setBuilderId(builderId);
	        jLocalNoti.setContent(content);
	        jLocalNoti.setTitle(title);
	        jLocalNoti.setNotificationId(notificationId);
	        jLocalNoti.setBroadcastTime(System.currentTimeMillis() + broadcastTime);
	        jLocalNoti.setExtras(extras.toString());

	        JPushInterface.addLocalNotification(webview.getActivity(), jLocalNoti);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void removeLocalNotification(IWebview webview, JSONArray data) {
		try {
			int notificationId = data.getInt(1);
			JPushInterface.removeLocalNotification(webview.getContext(),
					notificationId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void clearLocalNotifications(IWebview webview, JSONArray data) {
		JPushInterface.clearLocalNotifications(webview.getContext());
	}
	
	public void clearAllNotification(IWebview webview, JSONArray data) {
		JPushInterface.clearAllNotifications(webview.getContext());
	}
	
	public void clearNotificationById(IWebview webview, JSONArray data) {
		int id = -1;
		try {
			id = data.getInt(1);
			if (id != -1) {
				JPushInterface.clearNotificationById(webview.getActivity(), id);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void setTags(IWebview webview, JSONArray data) {
		try {
			String callbackId = data.getString(0);
			HashSet<String> tags = new HashSet<String>();
			for (int i = 1, len = data.length(); i < len; i++) {
				tags.add(data.getString(i));
			}
			JPushInterface.setTags(webview.getContext(), tags, mTagAliasCallback);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void setAlias(IWebview webview, JSONArray data) {
		try {
			String callbackId = data.getString(0);
			String alias = data.getString(1);
			JPushInterface.setAlias(webview.getContext(), alias, mTagAliasCallback);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void setTagsWithAlias(IWebview webview, JSONArray data) {
		HashSet<String> tags = new HashSet<String>();
		try { 
			String alias = data.getString(1);
			JSONArray tagsJson = data.getJSONArray(2);
			for (int i = 0; i < tagsJson.length(); i++) {
				tags.add(tagsJson.getString(i));
			}
			JPushInterface.setAliasAndTags(webview.getContext(), alias, tags,
					mTagAliasCallback);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void setDebugMode(IWebview webview, JSONArray data) {
		try {
			String callbackId = data.getString(0);
			boolean isOpenDebugMode = data.getBoolean(1);
			JPushInterface.setDebugMode(isOpenDebugMode);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 设置通知是否触发声音、震动、呼吸灯。 
	 * 方法中的"1"代表该设置的编号，需要服务器端同时指定要发送通知的 builderId = 1, 才会触发。
	 * 可根据需要自行修改，具体请参考：http://docs.Push.io/client/android_tutorials/#_11
	 */
	public void setBasicPushNotificationBuilder(IWebview webview, JSONArray data) {
		BasicPushNotificationBuilder builder = new BasicPushNotificationBuilder(
				webview.getContext());
		builder.developerArg0 = "Basic builder 1";
		JPushInterface.setPushNotificationBuilder(1, builder);
	}
	
	/**
	 * 设置通知使用自定义样式, 具体使用方法同上。
	 */
	public void setCustomPushNotificationBuilder(IWebview webview, JSONArray data) {
		// 需要自行修改
//		CustomPushNotificationBuilder builder = new CustomPushNotificationBuilder(
//				webview.getActivity(), R.layout.layout, R.id.icon, R.id.title,
//				R.id.text);
//		PushInterface.setPushNotificationBuilder(2, builder);
	}
	
	public void setLatestNotificationNum(IWebview webview, JSONArray data) {
		int num = -1;
		try {
			num = data.getInt(1);
			if (num != -1) {
				JPushInterface.setLatestNotificationNumber(webview.getContext(), num);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void setPushTime(IWebview webview, JSONArray data) {
		try {
			String callbackId = data.getString(0);

			JSONArray weekDaysArr = data.isNull(1) ? null : data.getJSONArray(1);
			if (weekDaysArr.length() > 7) {
				JSUtil.execCallback(webview, callbackId, "允许推送日期设置不正确",
						JSUtil.ERROR, false);
				return;
			}

			int startHour = data.getInt(1);
			if (isValidHour(startHour)) {
				JSUtil.execCallback(webview, callbackId, "允许推送开始时间设置不正确",
						JSUtil.ERROR, false);
			}

			int endHour = data.getInt(2);
			if (isValidHour(endHour)) {
				JSUtil.execCallback(webview, callbackId, "允许推送结束时间设置不正确",
						JSUtil.ERROR, false);
			}
			
			Set<Integer> weekDays = weekDaysArr == null ? null : new HashSet<Integer>();
			for(int i = 0; i < weekDaysArr.length(); i++) {
				weekDays.add(weekDaysArr.getInt(i));
			}
			JPushInterface.setPushTime(webview.getContext(), weekDays,
					startHour, endHour);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void setSilenceTime(IWebview webview, JSONArray data) {
		try {
			String callbackId = data.getString(0);
			int startHour = data.getInt(1);
			int startMin = data.getInt(2);
			int endHour = data.getInt(3);
			int endMin = data.getInt(4);
			if (!isValidHour(startHour) || !isValidHour(endHour) 
					|| !isValidMinute(startMin) || !isValidMinute(endMin)) {
				JSUtil.execCallback(webview, callbackId, "时间设置不正确",
						JSUtil.ERROR, false);
				return;
			}
			JPushInterface.setSilenceTime(webview.getContext(), startHour,
					startMin, endHour, endMin);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *  用于 Android 6.0 以上系统申请权限，具体可参考：
	 *  http://docs.Push.io/client/android_api/#android-60
	 */
	public void requestPermission(IWebview webview, JSONArray data) {
//		int currentVersion = android.os.Build.VERSION.SDK_INT;
//		if (currentVersion > android.os.Build.VERSION_CODES.LOLLIPOP) {
//			PushInterface.requestPermission(webview.getContext());
//		}
	}
	
	private static JSONObject getMessageObject(String msg,
			Map<String, Object> extras) {
        JSONObject data = new JSONObject();
        try {
            data.put("message", msg);
            JSONObject jExtras = new JSONObject();
            for (Entry<String, Object> entry : extras.entrySet()) {
                if (entry.getKey().equals("cn.JPush.android.EXTRA")) {
                    JSONObject jo = new JSONObject((String) entry.getValue());
                    String key;
                    Iterator<String> keys = jo.keys();
                    while(keys.hasNext()) {
                        key = keys.next().toString();
                        jExtras.put(key, jo.getString(key));
                    }
                    jExtras.put("cn.JPush.android.EXTRA", jo);
                } else {
                    jExtras.put(entry.getKey(), entry.getValue());
                }
            }
            if (jExtras.length() > 0) {
                data.put("extras", jExtras);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }
	
    private static JSONObject getNotificationObject(String title, String alert,
            Map<String, Object> extras) {
        JSONObject data = new JSONObject();
        try {
            data.put("title", title);
            data.put("alert", alert);
            JSONObject jExtras = new JSONObject();
            for (Entry<String, Object> entry : extras.entrySet()) {
                if (entry.getKey().equals("cn.JPush.android.EXTRA")) {
                    JSONObject jo = new JSONObject((String) entry.getValue());
                    String key;
                    Iterator<String> keys = jo.keys();
                    while(keys.hasNext()) {
                        key = keys.next().toString();
                        jExtras.put(key, jo.getString(key));
                    }
                    jExtras.put("cn.JPush.android.EXTRA", jo);
                } else {
                    jExtras.put(entry.getKey(), entry.getValue());
                }
            }
            if (jExtras.length() > 0) {
                data.put("extras", jExtras);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data; 
    }
    
    private TagAliasCallback mTagAliasCallback = new TagAliasCallback() {
		@Override
		public void gotResult(int code, String alias, Set<String> tags) {
			JSONObject data = new JSONObject(); 
			try {
				data.put("resultCode", code);
				data.put("tags", tags);
				data.put("alias", alias);
                final String jsEvent = String.format(
                        "plus.Push.fireDocumentEvent('jpush.setTagsWithAlias', %s)",
                        data.toString());
                mIWebview.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIWebview.loadUrl("javascript:" + jsEvent);
                    }
                });
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	};
	
	private boolean isValidHour(int hour) {
		if (hour < 0 || hour > 23) {
			return false;
		}
		return true;
	}
	
	private boolean isValidMinute(int min) {
		if (min < 0 || min > 59) {
			return false;
		}
		return true;
	}
	
}
