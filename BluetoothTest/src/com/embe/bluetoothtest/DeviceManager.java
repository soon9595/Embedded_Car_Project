package com.embe.bluetoothtest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class DeviceManager extends Service{
	public static final String 
	ACTION_DEVICE_SERVICE = "DeviceManager",
	TOGGLE_KEY = "toggleKey";
	
	private int fd;
	int bt;
	
	Thread mNativeReader = new Thread(new NativeReader());
	
	public native int openDriver();
	public native int SwitchOpen(int fd);
	public native int readDriver(int fd);
	public native void writeDriver(int fd, int direction,int mode);
	public native void closeDriver(int fd);
	

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("SERVICE", "on CREATE");
		System.loadLibrary("OpenJNI");
		fd = openDriver();
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mNativeReader.start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		closeDriver(fd);
		super.onDestroy();
	}
	
	private void sendBroadcastMessage(int ret) {
		Intent intent = new Intent();
		
		intent.setAction(ACTION_DEVICE_SERVICE);
		intent.putExtra(TOGGLE_KEY, ret) ;		
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}
	
	private class NativeReader implements Runnable {

		@Override
		public void run() {
			while (true) {
				int ret = SwitchOpen(fd);
				if (ret != -1)
					sendBroadcastMessage(ret);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}
}
