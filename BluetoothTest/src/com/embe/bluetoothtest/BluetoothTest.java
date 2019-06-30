package com.embe.bluetoothtest;

import java.io.InputStream;import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class BluetoothTest extends Activity {
	static final int REQUEST_ENABLE_BT = 10;
	int mPariedDeviceCount = 0;
	static int fd;
	Set<BluetoothDevice> mDevices;
	BluetoothAdapter mBluetoothAdapter;
	public native int openDriver();
	public native void writeDriver(int fd, int direction,int mode);
	BluetoothDevice mRemoteDevie;
	BluetoothSocket mSocket = null;
	OutputStream mOutputStream = null;
	InputStream mInputStream = null;
	String mStrDelimiter = "\n";
	char mCharDelimiter =  '\n';

	Thread mWorkerThread = null;
	byte[] readBuffer;
	int readBufferPosition;

	EditText mEditReceive, mEditSend;
	Button mButtonSend;

	Button mButtonG, mButtonB, mButtonL, mButtonR, mButtonS; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_bluetooth_test);
		setTextViews();
		setButtons();

		setBroadcastManager();
		System.loadLibrary("OpenJNI");
		Intent intent = new Intent(getApplicationContext(), DeviceManager.class);
		stopService(intent);
		startService(intent);
		fd = openDriver();
		startBluetooth();	
	}

	private void setBroadcastManager() { 
		LocalBroadcastManager.getInstance(this).registerReceiver(
				new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						int recv = intent.getIntExtra(DeviceManager.TOGGLE_KEY, -1);
						
						switch (recv) {
						case 1: //go
							sendData("g");
							writeDriver(fd, 1, 1);
							break;
						case 3: // left
							sendData("l");
							writeDriver(fd, 3, 1);
							break;
						case 5: // right
							sendData("r");
							writeDriver(fd, 4, 1);
							break;
						case 7: // back
							sendData("b");
							writeDriver(fd, 2, 1);
							break;
						case 4: // stop
							sendData("s");
							writeDriver(fd,5,2);
							break;
						}
					}
				}, 
				new IntentFilter(DeviceManager.ACTION_DEVICE_SERVICE));
	}

	private void setTextViews() {
		mEditReceive = (EditText) findViewById(R.id.receiveString);
		mEditSend = (EditText) findViewById(R.id.sendString);		
	}

	private void setButtons() {
		mButtonSend = (Button)findViewById(R.id.sendButton);
		mButtonG = (Button)findViewById(R.id.button_g);
		mButtonB = (Button)findViewById(R.id.button_b);
		mButtonL = (Button)findViewById(R.id.button_l);
		mButtonR = (Button)findViewById(R.id.button_r);
		mButtonS = (Button)findViewById(R.id.button_s);

		mButtonSend.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				sendData(mEditSend.getText().toString());
				mEditSend.setText("");
				Log.d("TAG", "SENT G");
			}
		});
		mButtonG.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendData("g");
			}
		});
		mButtonB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendData("b");
			}
		});
		mButtonL.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendData("l");
			}
		});
		mButtonR.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendData("r");
			}
		});
		mButtonS.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendData("s");
			}
		});
	}

	BluetoothDevice getDeviceFromBondedList(String name) {
		BluetoothDevice selectedDevice = null;
		for(BluetoothDevice deivce : mDevices) {
			if(name.equals(deivce.getName())) {
				selectedDevice = deivce;
				break;
			}
		}
		return selectedDevice;
	}

\	void sendData(String msg) {
		msg += mStrDelimiter;
		try{
			mOutputStream.write(msg.getBytes());
		}catch(Exception e) {
			Toast.makeText(getApplicationContext(), "데이터 전송중 오류가 발생", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	void connectToSelectedDevice(String selectedDeviceName) {
		mRemoteDevie = getDeviceFromBondedList(selectedDeviceName);
		UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

		try {
			mSocket = mRemoteDevie.createRfcommSocketToServiceRecord(uuid);
			mSocket.connect();
			mOutputStream = mSocket.getOutputStream();
			mInputStream = mSocket.getInputStream();
			beginListenForData();

		}catch(Exception e) {
			Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	void beginListenForData() {
		final Handler handler = new Handler();

		readBufferPosition = 0;
		readBuffer = new byte[1024];

		mWorkerThread = new Thread(new Runnable() 
		{
			@Override
			public void run() {
				while(!Thread.currentThread().isInterrupted()) {
					try {
						int byteAvailable = mInputStream.available();
						if(byteAvailable > 0) { 
							byte[] packetBytes = new byte[byteAvailable];
							mInputStream.read(packetBytes);
							for(int i=0; i<byteAvailable; i++) {
								byte b = packetBytes[i];
								Log.d("Input","b " + b);
								if(b == mCharDelimiter) {
									byte[] encodedBytes = new byte[readBufferPosition]; 
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);

									final String data = new String(encodedBytes, "US-ASCII");
									readBufferPosition = 0;

									handler.post(new Runnable(){
										@Override
										public void run() {
											mEditReceive.setText(mEditReceive.getText().toString() + data+ mStrDelimiter);
										}

									});
								}
								else {
									readBuffer[readBufferPosition++] = b;
								}
							}
						}

					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), "데이터 수신 중 오류가 발생 했습니다.", Toast.LENGTH_LONG).show();
						finish();       
					}
				}
			}

		});

	}

	void selectDevice() {
		mDevices = mBluetoothAdapter.getBondedDevices();
		mPariedDeviceCount = mDevices.size();

		if(mPariedDeviceCount == 0 ) {
			Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
			finish();
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("블루투스 장치 선택");

		List<String> listItems = new ArrayList<String>();
		for(BluetoothDevice device : mDevices) {
			listItems.add(device.getName());
		}
		listItems.add("취소");

		final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

		listItems.toArray(new CharSequence[listItems.size()]);

		builder.setItems(items, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item) {
				if(item == mPariedDeviceCount) {
					Toast.makeText(getApplicationContext(), "연결할 장치를 선택하지 않았습니다.", Toast.LENGTH_LONG).show();
					finish();
				}
				else { 
					connectToSelectedDevice(items[item].toString());
				}
			}

		});

		builder.setCancelable(false); 
		AlertDialog alert = builder.create();
		alert.show();
	}


	void startBluetooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if(mBluetoothAdapter == null ) {
			Toast.makeText(getApplicationContext(), "기기가 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
			finish();
		} else {       
			if(!mBluetoothAdapter.isEnabled()) {
				Toast.makeText(getApplicationContext(), "현재 블루투스가 비활성 상태입니다.", Toast.LENGTH_LONG).show();
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); 
				
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			} else {
				selectDevice();
			}
		}
	}

	@Override
	protected void onDestroy() {
		try{
			mWorkerThread.interrupt(); 
			mInputStream.close();
			mSocket.close();
		} catch (Exception e){}
		Intent intent = new Intent(getApplicationContext(), DeviceManager.class);
		stopService(intent);
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch(requestCode) {
		case REQUEST_ENABLE_BT:
			if(resultCode == RESULT_OK) {
				selectDevice();
			}
			else if(resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), 
						        "블루투수를 사용할 수 없어 프로그램을 종료합니다", Toast.LENGTH_LONG).show();
				finish();
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}    
}
