package com.example.mytest.irremoter_controller.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

//import com.tchip.ble.bleasy.utils.LogUtil;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;


/**
 * 蓝牙实体类
 * Created by ShiHai on 2016/8/11 10:42.
 */
public class BLEDevice {
	public UUID SERVER_UUID = UUID
			.fromString("0000fee9-0000-1000-8000-00805F9B34FB");

	private static final boolean DEBUG = true;
	public static final int STATE_DISCONNECTED = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;

	private Context mContext;
	private BluetoothDevice mDevice;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	private Object mGattLock = new Object();

	/**
	 * 构造函数
	 * @param bd 蓝牙设备
	 * @param context 上下文
	 * @param uuid 提供蓝牙透传读写服务的uuid的字符串
	 */
	public BLEDevice(BluetoothDevice bd, Context context, String uuid) {
		mDevice = bd;
		mContext = context;
		SERVER_UUID = UUID.fromString(uuid);
	}

/*******************************************************/
	/**
	 * 官方函数原型
	 * @return
     */
//	public BluetoothGatt getBluetoothGatt() {
//		return mBluetoothGatt;
//	}


	/***修改*****/
	public BluetoothGatt getBluetoothGatt() {
		if(mBluetoothGatt == null)
			return null;
		return mBluetoothGatt;
	}
/***************************************************************/
	@Override
	public boolean equals(Object o) {
		if (o == null || mDevice == null)
			return false;
		if (o instanceof BLEDevice) {
			BLEDevice bd = (BLEDevice) o;
			return mDevice.getAddress().equals(bd.mDevice.getAddress());
		} else {
			return false;
		}
	}

	/**
	 * 获取连接状态
	 * @return 	 STATE_DISCONNECTED = 0;
		STATE_CONNECTING = 1;
		pSTATE_CONNECTED = 2;
	 */
	public int getConnectionState() {
		return mConnectionState;
	}

	/**
	 * 获取设备名
	 * @return
	 */
	public String getDeviceName() {
		return mDevice.getName();
	}

	/**
	 * 获取设备mac地址
	 * @return
	 */
	public String getDeviceAddress() {
		return mDevice.getAddress();
	}

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			stopConnectTimer();
			if (newState == BluetoothProfile.STATE_CONNECTED) {
//				LogUtil.d(BLEDevice.this, "device " + getDeviceName()
//						+ " connected ! Stop mConnectTimer");
				startDiscoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//				LogUtil.d(BLEDevice.this, "device " + getDeviceName()
//						+ " disconnected ! close mBluetoothGatt");
				close();
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			// TODO Auto-generated method stub
			super.onDescriptorRead(gatt, descriptor, status);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			// TODO Auto-generated method stub
			super.onDescriptorWrite(gatt, descriptor, status);
//			LogUtil.d(BLEDevice.this, "status:" + status);

		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			// TODO Auto-generated method stub
			super.onReliableWriteCompleted(gatt, status);
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			stopDiscoverTimer();
			if (status == BluetoothGatt.GATT_SUCCESS) {
//				LogUtil.d(BLEDevice.this,
//						"onServicesDiscovered success ! Stop mDiscoverTimer");
				synchronized (mGattLock) {
					try {
						mBTGattServiceList = mBluetoothGatt.getServices();
						initBTGattCharacteristicList();
					} catch (Exception ex) {
//						LogUtil.d(BLEDevice.this,
//								"getServices mBluetoothGatt dead .");
						// close();
						return;
					}
				}
				if (checkServices()) {
					doConnectionChange(STATE_CONNECTED);
					doServicesDiscovered();
				} else {
//					LogUtil.w(BLEDevice.this,
//							"check services error , disconnect and close mBluetoothGatt");
					disconnect();
					close();
				}

			} else {
//				LogUtil.w(BLEDevice.this, "onServicesDiscovered received: "
//						+ status + " , disconnect for wrong");
				disconnect();
				close();
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
//				LogUtil.d(BLEDevice.this, "onCharacteristicRead "
//						+ characteristic.getUuid() + " received: "
//						+ characteristic.getValue());

				doCharacteristicRead(characteristic.getUuid(),
						characteristic.getStringValue(0));
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
//			LogUtil.d(BLEDevice.this, "onCharacteristicChanged");
			doCharacteristicChanged(characteristic.getUuid(),
					characteristic.getStringValue(0));
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//			LogUtil.d(BLEDevice.this, "onReadRemoteRssi rssi=" + rssi
//					+ ",status=" + status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// doRssiChange(rssi);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			doCharacteristicWriteState(characteristic.getUuid(), status);
//			LogUtil.d(BLEDevice.this, "onCharacteristicWrite!");
			mCommandHandler.post(new MySendCmdThread());
		}

	};
/**
 * 连接设备
 * @return
 */
	public boolean connect() {
		synchronized (mGattLock) {
			if (BleManager.getInstance() == null) {
//				LogUtil.w(BLEDevice.this, "BLEService not started !");
				return false;
			}

			final BluetoothDevice device = mDevice;
			if (device == null) {
//				LogUtil.w(BLEDevice.this,
//						"Device not found.  Unable to connect.");
				return false;
			}
			// We want to directly connect to the device, so we are setting the
			// autoConnect
			// parameter to false.
			mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
//			LogUtil.d(BLEDevice.this, "Trying to create a new connection.");
			doConnectionChange(STATE_CONNECTING);
			startConnectConnectTimer();
			return true;
		}
	}

	private static final long CONNECT_TIMEOUT = 30 * 1000;
	private Timer mConnectTimer;

/**
 * 开始连接超时计时器
 */
	private void startConnectConnectTimer() {
		stopConnectTimer();
		mConnectTimer = new Timer();
		mConnectTimer.schedule(new TimerTask() {

			@Override
			public void run() {
//				LogUtil.e(BLEDevice.this,
//						"connect time out , close mBluetoothGatt");
				close();
			}
		}, CONNECT_TIMEOUT);
	}
	/**
	 * 停止连接超时计时器
	 */
	private void stopConnectTimer() {
		if (mConnectTimer != null) {
			mConnectTimer.cancel();
		}
		mConnectTimer = null;
	}
/**
 * 断开连接
 */
	public void disconnect() {
		synchronized (mGattLock) {
			if (mBluetoothGatt == null) {
				return;
			}
			mBluetoothGatt.disconnect();
			clearBTGattCharacteristicList();
		}
	}
/**
 * 关闭BluetoothGatt
 */
	public void close() {
		synchronized (mGattLock) {
//			LogUtil.d(BLEDevice.this, "close and delete mBluetoothGatt.");
			if (mBluetoothGatt != null) {
				mBluetoothGatt.close();
				mBluetoothGatt = null;
			}
			doConnectionChange(STATE_DISCONNECTED);
			clearBTGattCharacteristicList();
		}
	}
/**
 * 获取所有蓝牙的服务
 * @return
 */
	public List<BluetoothGattService> getBluetoothGattServices() {
		return mBTGattServiceList;
	}
/**
 * 开始搜索服务超时计时器
 */
	private void startDiscoverTimer() {
		stopDiscoverTimer();
		mDiscoverTimer = new Timer();
		mDiscoverTimer.schedule(new TimerTask() {

			@Override
			public void run() {
//				LogUtil.d(BLEDevice.this,
//						"discoverServices time out , disconnect and close mBluetoothGatt");
				disconnect();
				close();
			}
		}, DISCOVER_SERVICES_TIMEOUT);
	}
	/**
	 * 停止搜索服务超时计时器
	 */
	private void stopDiscoverTimer() {
		if (mDiscoverTimer != null) {
			mDiscoverTimer.cancel();
			mDiscoverTimer = null;
		}
	}

	private List<BluetoothGattService> mBTGattServiceList = null;
	private List<BluetoothGattCharacteristic> mBTGattNtfCharacteristicList = new ArrayList<BluetoothGattCharacteristic>();
	private List<BluetoothGattCharacteristic> mBTGattWriteCharacteristicList = new ArrayList<BluetoothGattCharacteristic>();
	private int mBTGattNtfCharacteristicListIndex = 0;
	private int mBTGattWriteCharacteristicListIndex = 0;
	private Object mBTGattNtfCharacteristicListLock = new Object();
	private Object mBTGattWriteCharacteristicListLock = new Object();
/**
 * 蓝牙初始化 
 */
	private void initBTGattCharacteristicList() {
		if (mBluetoothGatt == null || mBTGattServiceList == null)
			return;
		BluetoothGattService bgs = mBluetoothGatt.getService(SERVER_UUID);
		List<BluetoothGattCharacteristic> mBTGattCharacteristicList = bgs
				.getCharacteristics();
		for (BluetoothGattCharacteristic characteristic : mBTGattCharacteristicList) {
			int properties = characteristic.getProperties();
			if (((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
				synchronized (mBTGattNtfCharacteristicListLock) {
					mBTGattNtfCharacteristicList.add(characteristic);
				}
//				LogUtil.d(BLEDevice.this, "add notify chara:"
//						+ characteristic.getUuid().toString());

			} else if (((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE)
					|| ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)
					|| ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
				synchronized (mBTGattWriteCharacteristicListLock) {
					mBTGattWriteCharacteristicList.add(characteristic);
				}
//				LogUtil.d(BLEDevice.this, "add write chara:"
//						+ characteristic.getUuid().toString());
			}
		}
		mBTGattNtfCharacteristicListIndex = 0;
		mBTGattWriteCharacteristicListIndex = 0;
		/*设置notify不能太频繁，否则容易失败*/
		mCommandHandler.postDelayed(mSetBTGattNtfCharacteristicRunnable, 500);
	}

	/* BLE的操作不能太频繁或者不间断连续操作，否则可能失败，而且无从知晓！ */
	private Runnable mSetBTGattNtfCharacteristicRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			synchronized (mBTGattNtfCharacteristicListLock) {
				if (mBTGattNtfCharacteristicList.size() == 0) {
					mBTGattNtfCharacteristicListIndex = 0;
					return;
				}
				BluetoothGattCharacteristic characteristic = mBTGattNtfCharacteristicList
						.get(mBTGattNtfCharacteristicListIndex);
				setCharacteristicNotification(characteristic, true);//TODO 9.15 将值设为false，原为true。
				mBTGattNtfCharacteristicListIndex++;
				if (mBTGattNtfCharacteristicListIndex >= mBTGattNtfCharacteristicList
						.size()) {
					mBTGattNtfCharacteristicListIndex = 0;
				} else {
					mCommandHandler.postDelayed(
							mSetBTGattNtfCharacteristicRunnable, 500);
				}
			}
		}
	};
/**
 * 清空特征值队列
 */
	private void clearBTGattCharacteristicList() {
		synchronized (mBTGattNtfCharacteristicListLock) {
			mBTGattNtfCharacteristicList.clear();
		}
		synchronized (mBTGattWriteCharacteristicListLock) {
			mBTGattWriteCharacteristicList.clear();
		}
		mBTGattNtfCharacteristicListIndex = 0;
		mBTGattWriteCharacteristicListIndex = 0;
	}
/**
 * 开始搜索服务
 */
	private void startDiscoverServices() {

		if (mBTGattServiceList != null) {
			mBTGattServiceList.clear();
		}
		mBTGattServiceList = null;

		stopDiscoverTimer();
		boolean b = false;
		synchronized (mGattLock) {
			try {
				if (mBluetoothGatt == null)
					return;
				b = mBluetoothGatt.discoverServices();
			} catch (Exception ex) {
//				LogUtil.d(BLEDevice.this,
//						"discoverServices mBluetoothGatt dead .");
				// close();
				return;
			}
		}

		if (!b) {
//			LogUtil.d(BLEDevice.this,
//					"discoverServices error disconnect and close mBluetoothGatt");
			disconnect();
			close();
		} else {
//			LogUtil.d(BLEDevice.this, "discoverServices start mDiscoverTimer");
			startDiscoverTimer();
		}
	}

	private static final long DISCOVER_SERVICES_TIMEOUT = 30 * 1000;
	private Timer mDiscoverTimer;
/**
 * 验证搜索服务是否成功
 * @return 成功：true；失败：false
 */
	private boolean checkServices() {
		// simple check
		if (mBTGattServiceList == null)
			return false;
		return mBTGattServiceList.size() > 3;
	}
/**
 * 读取蓝牙服务下读特征的数据
 * @param gatservice_uuid 服务的uuid
 * @param char_uuid 读特征的uuid
 * @return 成功：true；失败：false
 */
	public boolean readCharacteristic(UUID gatservice_uuid, UUID char_uuid) {
		synchronized (mGattLock) {
			try {
				if (mBluetoothGatt == null || mBTGattServiceList == null)
					return false;
				BluetoothGattService bgs = mBluetoothGatt
						.getService(gatservice_uuid);
				if (bgs == null)
					return false;
				BluetoothGattCharacteristic bgc = bgs
						.getCharacteristic(char_uuid);

				if (bgc == null)
					return false;
				int properties = bgc.getProperties();
				if (((properties & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ)) {
					return mBluetoothGatt.readCharacteristic(bgc);
				} else {
//					LogUtil.d(BLEDevice.this, gatservice_uuid + "->"
//							+ char_uuid + " can not read !");
					return false;
				}
			} catch (Exception ex) {
//				LogUtil.d(BLEDevice.this,
//						"readCharacteristic mBluetoothGatt dead .");
				// close();
				return false;
			}
		}
	}
/**
 * 向蓝牙服务下写特征写入数据
 * @param gatservice_uuid 服务的uuid
 * @param char_uuid 写特征的uuid
 * @param value 写入的数据
 * @return 成功：true；失败：false
 */
	public boolean writeCharacteristic(UUID gatservice_uuid, UUID char_uuid,
			String value) {
		synchronized (mGattLock) {
			try {
				if (mBluetoothGatt == null || mBTGattServiceList == null)
					return false;
				BluetoothGattService bgs = mBluetoothGatt
						.getService(gatservice_uuid);
				if (bgs == null) {
//					LogUtil.d(BLEDevice.this, "bgs:" + bgs + "->"
//							+ " can not find ! write error");
					return false;
				}
				BluetoothGattCharacteristic bgc = bgs
						.getCharacteristic(char_uuid);
				if (bgc == null) {
//					LogUtil.d(BLEDevice.this, "bgc:" + bgc + "->"
//							+ " can not find ! write error");
					return false;
				}
				int properties = bgc.getProperties();
				if (((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE)
						|| ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)
						|| ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
					bgc.setValue(value);
					return mBluetoothGatt.writeCharacteristic(bgc);
				} else {
//					LogUtil.d(BLEDevice.this, gatservice_uuid + "->"
//							+ char_uuid + " can not write !");
					return false;
				}
			} catch (Exception ex) {
//				LogUtil.d(BLEDevice.this,
//						"writeCharacteristic mBluetoothGatt dead .");
				// close();
				return false;
			}
		}
	}
/**
 * 向蓝牙服务下写特征写入数据
 * @param bgc 蓝牙服务下的写特征对象
 * @param value 数据
 * @return 成功：true；失败：false
 */
	public boolean writeCharacteristic(BluetoothGattCharacteristic bgc,
			String value) {
		synchronized (mGattLock) {
			try {
				if (mBluetoothGatt == null || mBTGattServiceList == null)
					return false;
				if (bgc == null) {
//					LogUtil.d(BLEDevice.this, "bgc:" + bgc + "->"
//							+ " can not find ! write error");
					return false;
				}
				int properties = bgc.getProperties();
				if (((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE)
						|| ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)
						|| ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
					bgc.setValue(value);
					return mBluetoothGatt.writeCharacteristic(bgc);
				} else {
//					LogUtil.d(BLEDevice.this, bgc.getUuid()
//							+ " can not write !");
					return false;
				}
			} catch (Exception ex) {
//				LogUtil.d(BLEDevice.this,
//						"writeCharacteristic mBluetoothGatt dead .");
				return false;
			}
		}
	}
/**
 * 该类往mBTGattWriteCharacteristicList下的所有特征进行写操作
 * @author Think Pad
 *
 */
	class MySendCmdThread implements Runnable {
		boolean result = false;
		String subValue = null;

		@Override
		public void run() {
			synchronized (writeDataBufLock) {
				if (writeDataBuf == null)
					return;
				synchronized (mBTGattWriteCharacteristicListLock) {
					for (int i = 0; i < mBTGattWriteCharacteristicList.size(); i++) {
						if (writeDataBuf.length() - writeDataBufIndex <= WRITE_CH_SIZE_MAX) {
							subValue = writeDataBuf.substring(
									writeDataBufIndex, writeDataBuf.length());
							writeDataBufIndex = writeDataBuf.length();
						} else {
							subValue = writeDataBuf.substring(
									writeDataBufIndex, writeDataBufIndex
											+ WRITE_CH_SIZE_MAX);
							writeDataBufIndex += WRITE_CH_SIZE_MAX;
						}
						result = writeCharacteristic(
								mBTGattWriteCharacteristicList
										.get(mBTGattWriteCharacteristicListIndex),
								subValue);
						if (result == false) {
//							LogUtil.e(BLEDevice.this,
//									"writeCharacteristic failed!");
						}
						mBTGattWriteCharacteristicListIndex++;
						mBTGattWriteCharacteristicListIndex %= mBTGattWriteCharacteristicList
								.size();
						if (writeDataBufIndex >= writeDataBuf.length()) {
//							LogUtil.d(BLEDevice.this, "send complete:"
//									+ writeDataBuf);
							writeDataBuf = null;
							writeDataBufIndex = -1;
							break;
						}
					}
				}
			}
		}
	};

	public static final int WRITE_CH_SIZE_MAX = 20;
	private String writeDataBuf = null;
	private int writeDataBufIndex = -1;
	private Object writeDataBufLock = new Object();
/**
 * 对mBTGattWriteCharacteristicList下的所有特征写入数据
 * @param value 数据
 * @return
 */
	public boolean write(String value) {
		if (mBluetoothGatt == null || mBTGattServiceList == null)
			return false;
		synchronized (writeDataBufLock) {
			if (writeDataBuf != null) {
//				LogUtil.e(BLEDevice.this,
//						"writeDataBuf != null! lose write new data!");
				return false;
			}
			if (mBTGattWriteCharacteristicList.size() == 0) {
//				LogUtil.e(BLEDevice.this, "No BTGattWriteCharacteristic!");
				return false;
			}
			writeDataBuf = new String(value);
			writeDataBufIndex = 0;
			if (writeDataBuf == null)
				return false;
			mCommandHandler.post(new MySendCmdThread());
		}

		return true;
	}

	private static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
/**
 * 设置蓝牙服务下的notify特征的监听
 * @param gatservice_uuid 蓝牙服务的uuid
 * @param char_uuid	notify特征的uuid
 * @param enable true：进行监听；false：停止监听
 * @return true：操作成功；false：操作失败
 */
	public boolean setCharacteristicNotification(UUID gatservice_uuid,
			UUID char_uuid, boolean enable) {
		synchronized (mGattLock) {
			try {
				if (mBluetoothGatt == null || mBTGattServiceList == null)
					return false;
				BluetoothGattService bgs = mBluetoothGatt
						.getService(gatservice_uuid);
				if (bgs == null)
					return false;
				BluetoothGattCharacteristic bgc = bgs
						.getCharacteristic(char_uuid);
				if (bgc == null)
					return false;
				int properties = bgc.getProperties();
				if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
					if (mBluetoothGatt.setCharacteristicNotification(bgc,
							enable)) {
						BluetoothGattDescriptor descriptor = bgc
								.getDescriptor(UUID
										.fromString(CLIENT_CHARACTERISTIC_CONFIG));
						descriptor
								.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						return mBluetoothGatt.writeDescriptor(descriptor);
					} else {
						return false;
					}
				} else {
//					LogUtil.d(BLEDevice.this, gatservice_uuid + "->"
//							+ char_uuid + " can not notify !");
					return false;
				}
			} catch (Exception ex) {
//				LogUtil.d(BLEDevice.this,
//						"setCharacteristicNotification mBluetoothGatt dead .");
				// close();
				return false;
			}
		}
	}

	/**
	 * 设置蓝牙服务下的notify特征的监听
	 * @param bgc notify特征对象
	 * @param enable true：进行监听；false：停止监听
	 * @return  true：操作成功；false：操作失败
	 */
	public boolean setCharacteristicNotification(
			BluetoothGattCharacteristic bgc, boolean enable) {
		synchronized (mGattLock) {
			try {
				if (mBluetoothGatt == null || mBTGattServiceList == null
						|| bgc == null)
					return false;
				int properties = bgc.getProperties();
				if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
					if (mBluetoothGatt.setCharacteristicNotification(bgc,
							enable)) {
						BluetoothGattDescriptor descriptor = bgc
								.getDescriptor(UUID
										.fromString(CLIENT_CHARACTERISTIC_CONFIG));
						descriptor
								.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						return mBluetoothGatt.writeDescriptor(descriptor);
					} else {
						return false;
					}
				} else {
//					LogUtil.d(BLEDevice.this, bgc.getUuid()
//							+ " can not notify !");
					return false;
				}
			} catch (Exception ex) {
//				LogUtil.d(BLEDevice.this,
//						"setCharacteristicNotification mBluetoothGatt dead .");
				// close();
				return false;
			}
		}
	}

	private BLEDeviceCallback mCallback;

	/**
	 * 注册BLEDeviceCallback
	 * @param l
	 */
	public void setBLEDeviceCallback(BLEDeviceCallback l) {
		mCallback = l;
	}
/**
 * 蓝牙连接状态发生变化时调用
 * @param newstate 新的状态值
 */
	private void doConnectionChange(int newstate) {
		int oldstate = mConnectionState;
		mConnectionState = newstate;
		if (mCallback != null) {
			mCallback.onBLEDeviceConnectionChange(this, oldstate, newstate);
		}
	}
/**
 * 读取蓝牙读特征时调用
 * @param uuid 读特征的UUID
 * @param data 数据
 */
	private void doCharacteristicRead(UUID uuid, String data) {
		if (mCallback != null) {
			mCallback.onCharacteristicRead(this, uuid, data);
		}
	}
/**
 * 向写特征写入数据时调用
 * @param uuid 写特征uuid
 * @param state 写入结果
 */
	private void doCharacteristicWriteState(UUID uuid, int state) {
		if (mCallback != null) {
			mCallback.onCharacteristicWriteState(this, uuid, state);
		}
	}
/**
 * notify特征响应消息时调用
 * @param uuid notify特征的UUID
 * @param data 数据
 */
	private void doCharacteristicChanged(UUID uuid, String data) {
		if (mCallback != null) {
			mCallback.onCharacteristicChanged(this, uuid, data);
		}
	}
/**
 * 发现蓝牙服务时调用
 */
	private void doServicesDiscovered() {
		if (mCallback != null) {
			mCallback.onServicesDiscovered(this);
		}
	}

	public interface BLEDeviceCallback {
		public void onBLEDeviceConnectionChange(BLEDevice dev, int laststate,
				int newstate);

		public void onCharacteristicRead(BLEDevice dev, UUID uuid, String data);

		public void onCharacteristicChanged(BLEDevice dev, UUID uuid,
				String data);

		public void onServicesDiscovered(BLEDevice dev);

		public void onCharacteristicWriteState(BLEDevice dev, UUID uuid,
				int state);
	}

	public interface OnRssiChangeListener {
		public void onRssiChange(BLEDevice dev, int rssi);
	}

	private OnRssiChangeListener mOnRssiChangeListener;

	public void setOnRssiChangeListener(OnRssiChangeListener l) {
		mOnRssiChangeListener = l;
	}

	static HandlerThread mCommandHandlerThread = new HandlerThread(
			"Tchip Handler Thread");
	static {
		mCommandHandlerThread.start();
	}
	static Handler mCommandHandler = new Handler(
			mCommandHandlerThread.getLooper());
}
