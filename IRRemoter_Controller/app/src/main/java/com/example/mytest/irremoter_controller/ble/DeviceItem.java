package com.example.mytest.irremoter_controller.ble;

import android.bluetooth.BluetoothDevice;

/**
 * 蓝牙设备构造体
 * Created by ShiHai on 2016/8/11 10:42.
 */
public class DeviceItem {

	private BluetoothDevice device ;
	private String address = "";
	private String name = "";
	private int rssi=0;


	public DeviceItem(BluetoothDevice device) {
		this.device = device;
		this.address = device.getAddress();
		this.name = device.getName();
		this.rssi=0;
	}
	public DeviceItem(BluetoothDevice device,int rssi) {
		this.device = device;
		this.address = device.getAddress();
		this.name = device.getName();
		this.rssi=rssi;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		DeviceItem that = (DeviceItem) o;

		if (!address.equals(that.address))
			return false;

		return true;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public String getDeviceAddress() {
		return address;
	}

	public String getDeviceName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public int getRssi() {
		return rssi;
	}
}
