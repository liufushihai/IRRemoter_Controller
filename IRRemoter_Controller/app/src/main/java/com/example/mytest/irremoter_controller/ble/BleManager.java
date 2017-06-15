package com.example.mytest.irremoter_controller.ble;

import java.util.UUID;

//import com.tchip.ble.bleasy.R;
import com.example.mytest.irremoter_controller.R;
import com.example.mytest.irremoter_controller.ble.BLEDevice.BLEDeviceCallback;
//import com.tchip.ble.bleasy.utils.LogUtil;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
/**
 * * 通过该类对BLEDevice进行控制，实现连接蓝牙设备并通信。
 * Created by ShiHai on 2016/8/11 10:42.
 */
@SuppressLint("NewApi")
public class BleManager implements BLEDeviceCallback {
	private static final boolean DEBUG = true;
	/**设备断开连接*/
	public static final int STATE_DISCONNECTED = 0;
	/**设备正在连接*/
	public static final int STATE_CONNECTING = 1;
	/**设备已连接*/
	public static final int STATE_CONNECTED = 2;

	private BLEDevice mBLEDevice;
	private BluetoothDevice mDevice;
	/**信号强度*/
	private int rssi=0;

	private ArrayAdapter<DeviceItem> mLeDeviceListAdapter;
	private Object mLeDeviceListAdapterLock = new Object();

	public ArrayAdapter<DeviceItem> getmLeDeviceListAdapter() {
		return mLeDeviceListAdapter;
	}

	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter;

	private Handler mHandler;

	/**
	 * @brief 本类内部调用的实例
	 */
	private static BleManager mBleManager = new BleManager();

	/**
	 * @brief 得到对象的实例
	 * @return BLEDevice
	 */
	public static BleManager getInstance() {
		// LogUtil.d(BleManager.this, "getInstance() start ");`
		if (mBleManager == null) {
			mBleManager = new BleManager();
		}
		// LogUtil.d(BleManager.this, "getInstance() end ");
		return mBleManager;
	}

	public void init(Context context) {
		mContext = context;
		mHandler = new Handler();
		mLeDeviceListAdapter = new ArrayAdapter<DeviceItem>(context,
				R.layout.device_list_item, R.id.device_name);
		initBluetooth();
	}

	public boolean isBluetoothAdapterOk() {

		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
			return true;
		} else {
			return false;
		}
	}

	/**搜索蓝牙设备状态*/
	private boolean mScanning = false;
	private Object mScanLock = new Object();

	private void initBluetooth() {
		final BluetoothManager bluetoothManager = (BluetoothManager) mContext
				.getSystemService(Context.BLUETOOTH_SERVICE);
		if (bluetoothManager == null) {
			Toast.makeText(mContext, "设备不支持蓝牙！", Toast.LENGTH_LONG).show();
			return;
		}
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.enable()) {
				Toast.makeText(mContext, "蓝牙打开失败！！", Toast.LENGTH_LONG).show();
			}
		}
	}
/**
 * 搜索蓝牙设备
 * @param enable true：开始搜索设备；false：停止搜索设备
 */
	public void scanLeDevice(final boolean enable) {
		synchronized (mScanLock) {
			if (enable) {
				if (!mScanning) {
					mScanning = true;
					if (isBluetoothAdapterOk()) {
						mBluetoothAdapter.startLeScan(lsCallbackForBind);
					}
//					LogUtil.d(BleManager.this, "startLeScan");
				}
			} else {
				if (mScanning) {
					mScanning = false;
					if (mBluetoothAdapter != null) {
						mBluetoothAdapter.stopLeScan(lsCallbackForBind);
					}
//					LogUtil.d(BleManager.this, "stopLeScan");
				}
			}
		}
	}

	private class MyLeScanCallback implements LeScanCallback {
		private String mScanAddress;

		public void setScanAddress(String addr) {
			mScanAddress = addr;
		}

		public String getScanAddress() {
			return mScanAddress;
		}

//		@Override
//		public void onLeScan(BluetoothDevice device, final int rssi,
//				byte[] scanRecord) {
//			final BluetoothDevice device_tmp = device;
//			mHandler.post(new Runnable() {
//
//				@Override
//				public void run() {
//					// TODO Auto-generated method stub
//					synchronized (mLeDeviceListAdapterLock) {
//						final DeviceItem item = new DeviceItem(device_tmp, rssi);
////						LogUtil.d(BleManager.this,
////								" gg scan device " + item.getDeviceName() + " "
////										+ item.getDeviceAddress() + " rssi"
////										+ rssi);
//						if (item.getDeviceAddress() != null) {
//							int length = mLeDeviceListAdapter.getCount();
//							int i = 0;
//							boolean exist = false;
//							for (i = 0; i < length; i++) {
//								if (mLeDeviceListAdapter.getItem(i)
//										.equals(item)) {
//									exist = true;
//									break;
//								}
//							}
//							if (exist == false && device_tmp.getName() != null
//									&& !device_tmp.getName().equals("")) {
//								mLeDeviceListAdapter.add(item);
//
//							}
//
//						} else {
////							LogUtil.d(BleManager.this,
////									"scan device getDeviceAddress == null!");
//						}
//					}
//				}
//			});
//
//		}
//


		/*******************************************************my code start***********************************************************/
		@Override
		public void onLeScan(BluetoothDevice device, final int rssi, byte[] scanRecord)
		{
			final BluetoothDevice device_tmp = device;
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					synchronized (mLeDeviceListAdapterLock) {
						final DeviceItem item = new DeviceItem(device_tmp, rssi);
//						LogUtil.d(BleManager.this,
//								" gg scan device " + item.getDeviceName() + " "
//										+ item.getDeviceAddress() + " rssi"
//										+ rssi);
						if (item.getDeviceAddress() != null) {
							int length = mLeDeviceListAdapter.getCount();
							Log.e("Length","Length is wrong"+length);
							int i = 0;
							boolean exist = false;
							for (i = 0; i < length; i++) {
								if (mLeDeviceListAdapter.getItem(i)
										.equals(item)) {
									exist = true;
									break;
								}
							}
							if (exist == false && device_tmp.getName() != null
									&& !device_tmp.getName().equals("")) {
								mLeDeviceListAdapter.add(item);

							}

						} else {
//							LogUtil.d(BleManager.this,
//									"scan device getDeviceAddress == null!");
						}
					}
				}
			});
		}

		/*******************************************************my code end*************************************************************/

	}

	private MyLeScanCallback lsCallbackForBind = new MyLeScanCallback();

	/**
	 * 连接设备前调用，创建设备对象并注册透传需要的服务的uuid和回调函数
	 * @param uuid 透传需要的服务的uuid的字符串
	 * @param callback 监听蓝牙设备的回调函数
	 */
	public void prepareBLEDevice(String uuid, BLEDeviceCallback callback) {
		if (mDevice == null) {
//			LogUtil.w(BleManager.this, "Device not found.  Unable to connect.");
			return;
		}
		mBLEDevice = new BLEDevice(mDevice, mContext, uuid);
		mBLEDevice.setBLEDeviceCallback(callback);

	}

	/**
	 * 返回连接设备对象
	 * @return
	 */
	public BLEDevice getmBLEDevice() {
		return mBLEDevice;
	}

	/**
	 * 清除当前连接设备
	 */
	public void clearBLEDevice() {
		mBLEDevice = null;
		mDevice = null;
	}
/**
 * 连接设备
 * @return
 */
	public boolean connect() {
		if (mDevice == null || mBLEDevice == null) {
//			LogUtil.w(BleManager.this, "No BLE Device!.");
			return false;
		}
		// disconnect();
		return mBLEDevice.connect();
	}
/**
 * 断开设备
 */
	public void disconnect() {
		if (mBLEDevice == null) {
			return;
		}
		if (DEBUG)
//			LogUtil.d(BleManager.this, "disconnect mBluetoothGatt.");
		mBLEDevice.disconnect();
		mBLEDevice.close();
	}
/**
 * 向设备写入数据(仅蓝牙透传服务）
 * @param value
 * @return
 */
	public boolean write(String value) {
		if (mBLEDevice == null) {
//			LogUtil.e(BleManager.this, "No BLE Device!");
			return false;
		}

		return mBLEDevice.write(value);
	}
/**
 * 清除设备列表
 */
	public void clearData() {
		disconnect();
		clearBLEDevice();
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				synchronized (mLeDeviceListAdapterLock) {
					mLeDeviceListAdapter.clear();
					mLeDeviceListAdapter.notifyDataSetChanged();
				}
			}
		});
	}
/**
 * 获取设备连接状态
 * @return
 */
	public int getConnectionState() {
		if (mBLEDevice == null) {
			return BLEDevice.STATE_DISCONNECTED;
		}
		return mBLEDevice.getConnectionState();
	}
/**
 * 获取信号强度
 * @return
 */
	public int getRssi() {
		if (mDevice == null) {
			return 0;
		}
		return rssi;
	}
/**
 * 获取设备名称
 * @return
 */
	public String getDeviceName() {
		if (mDevice == null)
			return "No Device";
		return mDevice.getName();
	}
/**
 * 获取设备mac地址
 * @return
 */
	public String getDeviceAddress() {
		if (mDevice == null)
			return "No Address";
		return mDevice.getAddress();
	}

	public static final String BLE_ACTION_CONNECTION_CHANGE = "com.tchip.tchipblehelper.action_CONNECTION_CHANGE";
	public static final String BLE_ACTION_CHARACTERISTIC_CHANGE = "com.tchip.tchipblehelper.action_CHARACTERISTIC_CHANGE";
	public static final String BLE_ACTION_CHARACTERISTIC_READ = "com.tchip.tchipblehelper.action_CHARACTERISTIC_READ";
	public static final String BLE_ACTION_CHARACTERISTIC_WRITE_STATE = "com.tchip.tchipblehelper.action_CHARACTERISTIC_WRITE_STATE";
	public static final String BLE_ACTION_SERVICES_DISCOVERED = "com.tchip.tchipblehelper.action_SERVICES_DISCOVERED";

	@Override
	public void onBLEDeviceConnectionChange(BLEDevice dev, int laststate,
			int newstate) {
		
		// TODO Auto-generated method stub
		Intent intent = new Intent(BLE_ACTION_CONNECTION_CHANGE);
			intent.putExtra("laststate", laststate);
		intent.putExtra("newstate", newstate);
		mContext.sendBroadcast(intent);
	}

	@Override
	public void onCharacteristicRead(BLEDevice dev, UUID uuid, String data) {
	
		// TODO Auto-generated method stub
		Intent intent = new Intent(BLE_ACTION_CHARACTERISTIC_READ);
		
		intent.putExtra("uuid", uuid.toString());
		intent.putExtra("data", data);
		mContext.sendBroadcast(intent);
	}

	@Override
	public void onCharacteristicChanged(BLEDevice dev, UUID uuid, String data) {
		// TODO Auto-generated method stub
	
		Intent intent = new Intent(BLE_ACTION_CHARACTERISTIC_CHANGE);
		
		intent.putExtra("uuid", uuid.toString());
		intent.putExtra("data", data);
		mContext.sendBroadcast(intent);
	}

	@Override
	public void onServicesDiscovered(BLEDevice dev) {
		// TODO Auto-generated method stub


	}

	@Override
	public void onCharacteristicWriteState(BLEDevice dev, UUID uuid, int state) {

		// TODO Auto-generated method stub
	
		Intent intent = new Intent(BLE_ACTION_CHARACTERISTIC_WRITE_STATE);
	
		intent.putExtra("uuid", uuid.toString());
		intent.putExtra("state", state);
		mContext.sendBroadcast(intent);
	}
/**
 * 获取搜索到的设备数量
 * @return
 */
	public int getDeviceCount() {
		return mLeDeviceListAdapter.getCount();
	}
/**
 * 设置当前连接设备
 * @param index
 */
	public void setDevice(int index) {
		mDevice = mLeDeviceListAdapter.getItem(index).getDevice();
		rssi = mLeDeviceListAdapter.getItem(index).getRssi();
	}

}
