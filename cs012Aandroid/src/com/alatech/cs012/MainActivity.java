package com.alatech.cs012;



import no.nordicsemi.android.error.GattError;

import com.alatech.cs012.UartService;

import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String TAG = "cs012";
    
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    
    //private static final int UpDataState_ENABLE_Stop = 0;
    //private static final int UpDataState_ENABLE_Start = 1;
	//private int mUpDataState = UpDataState_ENABLE_Stop;


	private Button btnConnectDisconnect;
	private Button btnBluetoothDfu;
	private TextView TVHeartRate;
	private ProgressBar mProgressBar;
	private TextView mTextPercentage;
	
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
 
	private String mFilePath;
	private Uri mFileStreamUri;
	
	/**
	 * 與  UART Service 通訊
	 */
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

           //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                Log.d(TAG, "UART_CONNECT_MSG");
            	 runOnUiThread(new Runnable() {
                     public void run() {
                             btnConnectDisconnect.setText("Disconnect");
                     }
            	 });
            }
           
          //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
            	Log.i("Chandler","ACTION_GATT_DISCONNECTED");
	           	 runOnUiThread(new Runnable() {
	                 public void run() {
	                         Log.d(TAG, "UART_DISCONNECT_MSG");
	                         btnConnectDisconnect.setText("Connect");
	                         mService.close();
	                 }
	             });
	           	 //myHandler.removeCallbacks(runTimerStop);
	           	DfuStart();
            }
          //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
             	mService.enableTXNotification();
            }
          //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
            	
            }
           //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
            	showMessage("Device doesn't support UART. Disconnecting");
            	mService.disconnect();
        		
            }
            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_HEART_RATE_DISCOVERED)){
            	mService.enableHeartRateNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_SERIVCES_HEART_RATE)){
            	final String stringValue = intent.getStringExtra(UartService.EXTRA_DATA);
            	btnBluetoothDfu.setEnabled(true);
              	 runOnUiThread(new Runnable() {
                     public void run() {
                    	 TVHeartRate.setText(stringValue);
                    	 //Log.i("Chandler",stringValue);	
                     }
                 });
            }
            
        }
    };
    
       
       private void DfuStart (){   
    	   mBtAdapter.disable(); 
    	   try {
    		   Thread.sleep(1000);
    		   } catch (InterruptedException e) {
    		   // TODO Auto-generated catch block
    		   e.printStackTrace();
    		   }
    	   mBtAdapter.enable();
    	   try {
    		   Thread.sleep(3000);
    		   } catch (InterruptedException e) {
    		   // TODO Auto-generated catch block
    		   e.printStackTrace();
    		   }
     	showProgressBar();
 		mFilePath = null;
 		mFileStreamUri = null;
 		//mFilePath = "/data/data/com.alatech.cs012/files/res/raw/cs012_v11.hex";
 		mFilePath = "/storage/emulated/0/電子書/Test/CS012_V13.hex";
 		final Intent service = new Intent(MainActivity.this, DfuService.class);
 		//service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, "E6:DC:5F:E2:BC:60");
 		//service.putExtra(DfuService.EXTRA_DEVICE_NAME, "CS012 HRM V11");
 		//service.putExtra(DfuService.EXTRA_FILE_PATH, mFilePath);
 		//service.putExtra(DfuService.EXTRA_FILE_URI, mFileStreamUri);
 		
 		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, mDevice.getAddress());
 		service.putExtra(DfuService.EXTRA_DEVICE_NAME, mDevice.getName());
 		service.putExtra(DfuService.EXTRA_FILE_PATH, mFilePath);
 		service.putExtra(DfuService.EXTRA_FILE_URI, mFileStreamUri);
 		//Log.i("Chandler",mDevice.getAddress());
 		//Log.i("Chandler",mDevice.getName());
 		//Log.i("Chandler",mFilePath);
 		startService(service); 	 
       }
       
	/**
	 * 與  Dfu Service 通訊
	 */
	private final BroadcastReceiver mDfuUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			// DFU is in progress or an error occurred 
			final String action = intent.getAction();

			if (DfuService.BROADCAST_PROGRESS.equals(action)) {
				Log.i("Chandler","BROADCAST_PROGRESS");
				final int progress = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
				updateProgressBar(progress, false);
				Log.i("Chandler","Test2");
			} else if (DfuService.BROADCAST_ERROR.equals(action)) {
				Log.i("Chandler","BROADCAST_ERROR");
				final int error = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
				updateProgressBar(error, true);

				// We have to wait a bit before canceling notification. This is called before DfuService creates the last notification.
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						// if this activity is still open and upload process was completed, cancel the notification
						final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
						manager.cancel(DfuService.NOTIFICATION_ID);
					}
				}, 200);
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setGUI();
        service_init();

        //File workDir = this.getFilesDir();
        //Log.e("Chandler", "workdir "+ workDir.getAbsolutePath());
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	private void setGUI() {
		btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
		btnBluetoothDfu = (Button) findViewById(R.id.btn_updata);
		TVHeartRate = (TextView) findViewById(R.id.heart_value);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar_file);
		mTextPercentage = (TextView) findViewById(R.id.textviewProgress);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
        		mService = ((UartService.LocalBinder) rawBinder).getService();
        		Log.d(TAG, "onServiceConnected mService= " + mService);
        		if (!mService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
        }

        public void onServiceDisconnected(ComponentName classname) {
        		mService = null;
        }
    };
	
	//============================================================
	// 建立跟UART service 廣播
	//============================================================
    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(UartService.ACTION_SERIVCES_HEART_RATE);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_HEART_RATE_DISCOVERED);
        return intentFilter;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            showBLEDialog();
        }
		// We are using LocalBroadcastReceiver instead of normal BroadcastReceiver for optimization purposes
		final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
		broadcastManager.registerReceiver(mDfuUpdateReceiver, makeDfuUpdateIntentFilter());
    }
    
	@Override
	protected void onPause() {
		super.onPause();

		final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
		broadcastManager.unregisterReceiver(mDfuUpdateReceiver);
	}
    
    @Override
    public void onDestroy() {
    	 super.onDestroy();
        Log.d(TAG, "onDestroy()");
        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        } 
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
    }	 

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_SELECT_DEVICE:
        	//When the DeviceListActivity return, with the selected device address
        	if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                mService.connect(deviceAddress);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            Log.e(TAG, "wrong request code");
            break;
        }
    }
    
	private static IntentFilter makeDfuUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(DfuService.BROADCAST_PROGRESS);
		intentFilter.addAction(DfuService.BROADCAST_ERROR);
		intentFilter.addAction(DfuService.BROADCAST_LOG);
		return intentFilter;
	}
    
	private void updateProgressBar(final int progress, final boolean error) {
		switch (progress) {
		case DfuService.PROGRESS_CONNECTING:
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_connecting);
			break;
		case DfuService.PROGRESS_STARTING:
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_starting);
			break;
		case DfuService.PROGRESS_VALIDATING:
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_validating);
			break;
		case DfuService.PROGRESS_DISCONNECTING:
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_disconnecting);
			break;
		case DfuService.PROGRESS_COMPLETED:
			mTextPercentage.setText(R.string.dfu_status_completed);
			// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					showFileTransferSuccessMessage();
					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}
			}, 200);
			break;
		default:
			mProgressBar.setIndeterminate(false);
			if (error) {
				showErrorMessage(progress);
			} else {
				mProgressBar.setProgress(progress);
				mTextPercentage.setText(getString(R.string.progress, progress));
			}
			break;
		}
	}	
	
	private void showFileTransferSuccessMessage() {
		clearUI();
		showMessage("Application has been transfered successfully.");
	}	
	
	private void showErrorMessage(final int code) {
		clearUI();
		showMessage("Upload failed: " + GattError.parse(code) + " (" + code + ")");
	}
	
	private void clearUI() {
		mProgressBar.setVisibility(View.INVISIBLE);
		mTextPercentage.setVisibility(View.INVISIBLE);
	}	
	
	/**
	 * Handler Disconnect & Connect button
	 */
	public void onSelectClicked(final View view) {
        if (!mBtAdapter.isEnabled()) {
            showBLEDialog();
        } else {
        	if (btnConnectDisconnect.getText().equals("Connect")){
        		//Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
        		Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
    			startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
			} else {
				//Disconnect button pressed
				if (mDevice!=null)
				{
					mService.disconnect();
				}
			}
        }
	}
	
	/**
	 *  發送更新指令給裝置
	 */
	public void onUpDataClicked(final View view) {
    	if(mDevice != null) {
            if (mBtAdapter.isEnabled()) {
            	byte[] value;
				int res;
				value = new byte[4];
				res = 0xFF;
				value[0] = (byte) (res & 0xff);
				value[1] = (byte) ((res >> 8) & 0xff);
				value[2] = (byte) ((res >> 16) & 0xff);
				value[3] = (byte) (res >>> 24);
            	mService.writeRXCharacteristic(value);
            	Log.i("Chandler","UpData");	
            	mProgressBar.setVisibility(View.VISIBLE);
            	mProgressBar.setIndeterminate(true);
            }
    	}
	}
    
	private void showProgressBar() {
		mProgressBar.setVisibility(View.VISIBLE);
		mTextPercentage.setVisibility(View.VISIBLE);
	}
	
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    
	private void showBLEDialog() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}
    
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
