package com.slipkprojects.sockshttp.util;

import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
import android.annotation.TargetApi;
import android.os.Build;

public class GoogleFeedbackUtils {

	private static final String TAG = GoogleFeedbackUtils.class.getSimpleName();

	private GoogleFeedbackUtils() {}

	/**
	 * Binds the Google Feedback service.
	 * 
	 * @param context the context
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static void bindFeedback(Context context) {
		Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
		intent.setComponent(new ComponentName("com.google.android.gms", "com.google.android.gms.feedback.LegacyBugReportService"));
		ServiceConnection serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				try {
					service.transact(Binder.FIRST_CALL_TRANSACTION, Parcel.obtain(), null, 0);
				} catch (RemoteException e) {
					Log.e(TAG, "RemoteException", e);
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {}
		};
		// Bind to the service after creating it if necessary
		context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}
}
