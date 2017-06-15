package com.example.mytest.irremoter_controller.activity;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mytest.irremoter_controller.R;
import com.example.mytest.irremoter_controller.ble.BLEDevice;
import com.example.mytest.irremoter_controller.ble.BleManager;
import com.example.mytest.irremoter_controller.utils.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;


public class MainAty extends Activity implements View.OnClickListener {

    private ImageButton imgBtn_add;
    private ImageButton imgBtn_reduce;
    private ImageButton imgBtn_switch;
    private ImageButton imgBtn_wind_scan;
    private ImageButton imgBtn_speed_wind;
    private ImageButton imgBtn_mode;
    private TextView tv_temperature;
    private TextView tv_wind_scan;
    private TextView tv_speed_wind;
    private TextView tv_mode;
    private TextView tv_temperature_sign;
    private ListView lv_dev;
    private boolean app_enabled = false;
    private boolean switch_state_on = false;
    private boolean switch_state_scan = false;
    private int temp_value = 0;
    private int max_temp_value = 30;
    private int min_temp_value = 16;
    private int mode_value = 0;
    private int speed_value = 0;
    private int conn_dev_successful_num = 0;
    private String my_dev_name = "FireIRRemoter";
    private boolean conn_successful_flag = false;
    private boolean conn_failed_flag = false ;
    private boolean InitFirst = true ;

    private ArrayList<String> preDatas = new ArrayList<String>();// 存放分段发送的消息
    private ArrayList<BluetoothGattService> services = new ArrayList<BluetoothGattService>();
    public static final String SEND_COMPLETED = "SEND_COMPLETED";
    private Iterator<String> dataIterator;// 消息的迭代器

    protected static String uuidQppService = "0000ff92-0000-1000-8000-00805f9b34fb";// 读写服务
    protected static String uuidQppCharWrite = "00009600-0000-1000-8000-00805f9b34fb"; // 写特征
    protected static String uuidQppCharRead = "00009601-0000-1000-8000-00805f9b34fb";// 读特征

    protected static String uuidATService = "0000cc03-0000-1000-8000-00805f9b34fb";// AT命令
    protected static String uuidATCharWrite = "0000ec00-0000-1000-8000-00805f9b34fb";// 写特征
    protected static String uuidATCharRead = "0000eb00-0000-1000-8000-00805f9b34fb"; // 读特征

    private static final int HANDLER_ON_CONNECTION_CHANGED = 0;
    private static final int HANDLER_ON_CHARACTERISTIC_READ = 1;
    private static final int HANDLER_ON_CHARACTERISTIC_WRITE = 2;
    private static final int HANDLER_ON_CHARACTERISTIC_CHANGED = 3;
    private static final int HANDLER_ON_SERVICE_DISCOVERED = 4;


    @Override
    protected  void onDestroy()
    {
        super.onDestroy();
        BleManager.getInstance().clearBLEDevice();
        BleManager.getInstance().disconnect();
    }

    @Override
    protected  void onStop()
    {
        super.onStop();
        BleManager.getInstance().clearBLEDevice();
        BleManager.getInstance().disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_main);

        InitWidgets();
        SetClickListenerForWidgets();
        SetWidgetsEnabled(app_enabled);


        BleManager.getInstance().init(MainAty.this);
        startScanAndBind();
        BleManager.getInstance().prepareBLEDevice(uuidQppService,BleManager.getInstance());
        lv_dev.setAdapter(BleManager.getInstance().getmLeDeviceListAdapter());
        lv_dev.setVisibility(View.INVISIBLE);
        new BleTasks().execute();
    }

    /**
     * 为控件实例化
     */
    private void InitWidgets()
    {
        imgBtn_add = (ImageButton) findViewById(R.id.imgBtn_add);
        imgBtn_mode = (ImageButton) findViewById(R.id.imgBtn_mode);
        imgBtn_reduce =(ImageButton) findViewById(R.id.imgBtn_reduce);
        imgBtn_switch = (ImageButton) findViewById(R.id.imgBtn_switch);
        imgBtn_speed_wind = (ImageButton) findViewById(R.id.imgBtn_speed_wind);
        imgBtn_wind_scan = (ImageButton) findViewById(R.id.imgBtn_wind_scan);
        tv_temperature = (TextView) findViewById(R.id.tv_temperature);
        tv_wind_scan = (TextView) findViewById(R.id.tv_wind_scan);
        tv_speed_wind = (TextView) findViewById(R.id.tv_speed_wind);
        tv_mode = (TextView) findViewById(R.id.tv_mode);
        tv_temperature_sign = (TextView) findViewById(R.id.tv_temperature_sign);
        lv_dev = (ListView) findViewById(R.id.lv_dev);
    }

    /**
     * 初始化界面数据
     */
    private void InitUiData()
    {
        if(InitFirst == true) {
            temp_value = 26;
            tv_temperature.setText(temp_value + "");
            tv_temperature_sign.setText(R.string.Temperature_sign);
            InitFirst = false;
        }
        else{
            tv_temperature.setText(temp_value + "");
            tv_temperature_sign.setText(R.string.Temperature_sign);
        }

    }

    /**
     * 清除界面数据
     */
    private void ClearUiData()
    {
        speed_value = 0;
        mode_value = 0;
        tv_wind_scan.setText(R.string.null_str);
        tv_speed_wind.setText(R.string.State_auto_wind);
        tv_mode.setText(R.string.Mode_refrigeration);
        tv_temperature.setText(temp_value+"");
        tv_temperature_sign.setText(R.string.Temperature_sign);
    }

    /**
     * 为控件注册监听事件
     */
    private void SetClickListenerForWidgets()
    {
        imgBtn_add.setOnClickListener(MainAty.this);
        imgBtn_reduce.setOnClickListener(MainAty.this);
        imgBtn_switch.setOnClickListener(MainAty.this);
        imgBtn_wind_scan.setOnClickListener(MainAty.this);
        imgBtn_speed_wind.setOnClickListener(MainAty.this);
        imgBtn_mode.setOnClickListener(MainAty.this);
    }


    /**
     * 开关/扫风按钮操作
     * @param imgbtn
     */
    private void SwitchProcess(ImageButton imgbtn)
    {
        if(imgbtn == imgBtn_switch)
        {
            if(switch_state_on == false){
                InitUiData();
                imgBtn_switch.setImageResource(R.drawable.img_switch_on);
                switch_state_on = true;
                app_enabled = true;
                SetWidgetsEnabled(app_enabled);
            }
            else if(switch_state_on == true){
                //ClearUiData();
                imgBtn_switch.setImageResource(R.drawable.img_switch_off);
                switch_state_on = false;
                app_enabled = false;
                if (switch_state_scan == true) {
                    //imgBtn_wind_scan.setImageResource(R.drawable.img_switch_off);
                    //switch_state_scan = false;
                }
                SetWidgetsEnabled(app_enabled);
            }
        }
        else if(imgbtn == imgBtn_wind_scan)
        {
            if(app_enabled == true) {
                if (switch_state_scan == false) {
                   imgBtn_wind_scan.setImageResource(R.drawable.img_switch_on);
                    switch_state_scan = true;
                } else if (switch_state_scan == true) {
                   imgBtn_wind_scan.setImageResource(R.drawable.img_switch_off);
                    switch_state_scan = false;
                }
            }
            else
            {
                Toast.makeText(MainAty.this,"app_enabled=false",Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 温度按钮操作
     * @param imgbtn
     */
    private void TemperatureProcess(ImageButton imgbtn)
    {
        if(imgbtn == imgBtn_add)
        {
            if(++temp_value>max_temp_value){
                temp_value = max_temp_value;
            }
        }
        else if(imgbtn == imgBtn_reduce)
        {
            if(--temp_value<min_temp_value){
            temp_value = min_temp_value;
            }
        }
        tv_temperature.setText(temp_value+"");
    }

    /**
     * 风速和模式按钮操作
     * @param imgbtn
     */
    private void WindSpeedAndModeProcess(ImageButton imgbtn)
    {
        if(imgbtn == imgBtn_speed_wind)
        {
            if(++speed_value>3)
            {
                speed_value = 0;
            }
        }
        else if (imgbtn == imgBtn_mode)
        {
            if(++mode_value>4)
            {
                mode_value = 0;
            }
        }
    }

    /**
     * 根据变量值进行对应信息的显示
     */
    private void InformationShow()
    {
        if(app_enabled == true){
            String s_v_str = null,m_v_str = null,w_s_str = null;
            switch (speed_value)
            {
                case 0: s_v_str = getString(R.string.State_auto_wind); break;
                case 1: s_v_str = getString(R.string.State_one_wind); break;
                case 2: s_v_str = getString(R.string.State_two_wind); break;
                case 3: s_v_str = getString(R.string.State_third_wind); break;
                default:break;
            }
            switch (mode_value)
            {
                case 0:m_v_str = getString(R.string.Mode_refrigeration); break;
                case 1:m_v_str = getString(R.string.Mode_humidification);break;
                case 2:m_v_str = getString(R.string.Mode_blowing);break;
                case 3:m_v_str = getString(R.string.Mode_heating);break;
                case 4:m_v_str = getString(R.string.Mode_auto);break;
                default:break;
            }
            if(switch_state_scan == false)
            {
                w_s_str = getString(R.string.null_str);
            }
            else if(switch_state_scan == true){
                w_s_str = getString(R.string.Wind_scan);
            }
            tv_wind_scan.setText(w_s_str);
            tv_mode.setText(m_v_str);
            tv_speed_wind.setText(s_v_str);
        }
    }

    /**
     * 根据app_enabled设置控件是否可用
     * @param enabled
     */
    private void SetWidgetsEnabled(boolean enabled)
    {
        if(enabled == true){
            imgBtn_wind_scan.setEnabled(true);
            imgBtn_speed_wind.setEnabled(true);
            imgBtn_mode.setEnabled(true);
            imgBtn_add.setEnabled(true);
            imgBtn_reduce.setEnabled(true);
        }
        else if(enabled == false)
        {
            imgBtn_wind_scan.setEnabled(false);
            imgBtn_speed_wind.setEnabled(false);
            imgBtn_mode.setEnabled(false);
            imgBtn_add.setEnabled(false);
            imgBtn_reduce.setEnabled(false);
        }
    }


    private void  BleConnectHandler()
    {
        BleManager.getInstance().prepareBLEDevice(uuidQppService,BleManager.getInstance());
        for(int i = 0 ;i < BleManager.getInstance().getmLeDeviceListAdapter().getCount();i++)
        {
            if(BleManager.getInstance().getmLeDeviceListAdapter().getItem(i).getDeviceName() == my_dev_name)
            {
                BleManager.getInstance().setDevice(i);
                BleManager.getInstance().connect();
                BleManager.getInstance().connect();
                BleManager.getInstance().connect();
                break;
            }
        }
    }

    /**
     * 按钮点击事件监听
     * @param view
     */
    @Override
    public void onClick(View view) {
        String str = getString(R.string.Command_on);
        switch (view.getId())
        {
            case R.id.imgBtn_add:
                str = getString(R.string.Command_add);
                TemperatureProcess(imgBtn_add);
                break;
            case R.id.imgBtn_reduce:
                str = getString(R.string.Command_reduce);
                TemperatureProcess(imgBtn_reduce);
                break;
            case R.id.imgBtn_switch:
                str = getString(R.string.Command_on);
                SwitchProcess(imgBtn_switch);
                break;
            case R.id.imgBtn_wind_scan:
                str = getString(R.string.Command_wind_scan);
                SwitchProcess(imgBtn_wind_scan);
                break;
            case R.id.imgBtn_speed_wind:
                str = getString(R.string.Command_wind_speed);
                WindSpeedAndModeProcess(imgBtn_speed_wind);
                break;
            case R.id.imgBtn_mode:

                str = getString(R.string.Command_mode);
                WindSpeedAndModeProcess(imgBtn_mode);
                break;
            default:break;
        }

        /******
         * 以下判断有些问题，可以再做修改，主要是解决了程序点击开关按钮会导致程序崩溃的问题
         * 判断有设备打开时才进行服务的扫描，以免出现NullPointerException异常
         */
        if(conn_successful_flag == true)
        {
            setNotification(uuidQppService, uuidQppCharWrite);
            searchServices();
        }
        else {
            if((conn_failed_flag == false)&&(conn_successful_flag == false)) {
                //Toast.makeText(MainAty.this, R.string.connected_dev_failed, Toast.LENGTH_SHORT).show();
                conn_failed_flag = true ;
            }
        }
        BleManager.getInstance().write(str);
        InformationShow();
    }

    /****************************************  start  **************************************************/

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String value = (String) msg.obj;
            Toast.makeText(MainAty.this, value,
                    Toast.LENGTH_SHORT).show();
        };
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_ON_CONNECTION_CHANGED:
                    int newState = msg.arg1;
                    if (newState == BLEDevice.STATE_DISCONNECTED) {
//                        Toast.makeText(MainAty.this,
//                                R.string.disconnected_hint, Toast.LENGTH_SHORT)
//                                .show();
//                        LogUtil.d(DeviceListActivity.this, "STATE_DISCONNECTED!");
//                        updateDevMsg();
                        // tv_connect_status.setText("Disconnected");
                    } else if (newState == BLEDevice.STATE_CONNECTED) {
//                        Toast.makeText(MainAty.this,
//                                R.string.connected_hint, Toast.LENGTH_SHORT).show();
                        if(++conn_dev_successful_num > 4) {
                            Toast.makeText(MainAty.this,
                                    R.string.connected_dev_successful, Toast.LENGTH_LONG).show();
                            conn_dev_successful_num = 0;
                        }
//                        updateDevMsg();
                        // tv_connect_status.setText("Connected");
                    }
                    break;
                case HANDLER_ON_CHARACTERISTIC_WRITE:
                    break;
                case HANDLER_ON_CHARACTERISTIC_CHANGED:

                    break;
                case HANDLER_ON_SERVICE_DISCOVERED:

                    break;
                default:
                    // LogUtil.e(DeviceListActivity.this, "Unknown message!");
                    break;
            }
        }
    };

    private boolean mScanAndBindStarted = false;
    public static final long SCAN_PERIOD = 4 * 1000;

    /**
     * 开始扫描设备
     */
    public void startScanAndBind() {
        if (BleManager.getInstance() == null || mScanAndBindStarted == true) {
            return;
        }
        mScanAndBindStarted = true;
        BleManager.getInstance().clearData();
        BleManager.getInstance().scanLeDevice(true);
        //Toast.makeText(this,BleManager.getInstance().getDeviceCount()+"",Toast.LENGTH_SHORT).show();
        registerReciver();
    }

    /**
     * 停止扫描设备
     */
    public void stopScanForBind() {
        if (BleManager.getInstance() == null)
            return;
        BleManager.getInstance().scanLeDevice(false);
        mScanAndBindStarted = false;
    }

    /**
     * 串口透传：发送数据
     */
    private void sendData() {

        if (BleManager.getInstance().getConnectionState() != BLEDevice.STATE_CONNECTED) {
            Toast.makeText(MainAty.this,
                    R.string.connecting_hint, Toast.LENGTH_SHORT).show();
            BleManager.getInstance().disconnect();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    BleManager.getInstance().connect();
                }
            }, 3000);

            return;
        }

       // String value = editText_data_input.getText().toString();
        String value = getString(R.string.Command_on);
        if (value == null || value.equals("")) {
            return;
        }

//        if (getSwitchStatus(R.id.hex_send_switch)) {
//            value = value.toUpperCase();
//            value = value.replace(" ", "");
//            value = StringUtils.hexStr2Str(value);
//
//        }

//        // 发送新行
//        if (getSwitchStatus(R.id.send_unewline_switch)) {
//            value = value + "\r\n";
//        }

        System.out.println("send value str=" + value);
        if (BleManager.getInstance().write(value) == false) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    Toast.makeText(MainAty.this,
                            R.string.write_text_send_err, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        } else {

//            Message msg = handler.obtainMessage();
//            msg.obj = value;
//            handler.sendMessage(msg);

        }

    }

    /**
     * 发送数据
     */
    private Object sendDataLock = new Object();
    private final static int DATALENGTH = 20;//蓝牙ble一次传输20个字节长的数据

    private boolean sendData(String value) {
        // 判断连接状态
        synchronized (sendDataLock) {
            if (BleManager.getInstance().getConnectionState() != BLEDevice.STATE_CONNECTED) {
                Toast.makeText(MainAty.this,
                        R.string.connecting_hint, Toast.LENGTH_SHORT).show();
            }
            // 判断value是否为空
            if (value == null || value.equals(""))
                return false;

            // 判断value长度
            if ( value.length() > DATALENGTH) {
                preDatas = StringUtils.spilt(value, DATALENGTH);
                dataIterator = preDatas.iterator();
                sendBroadcast(new Intent(SEND_COMPLETED));
                return false;
            }
            //System.out.println("sendvalue=" + value);
            // 发送

            if (BleManager
                    .getInstance()
                    .getmBLEDevice()
                    .writeCharacteristic(UUID.fromString(uuidATService),
                            UUID.fromString(uuidATCharWrite), value) == false) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(MainAty.this,
                                R.string.write_text_send_err,
                                Toast.LENGTH_SHORT).show();

                    }
                });
                return false;
            } else {
                return true;
            }
        }

    }


    /**
     * 设置notify
     * @param serviceUuid 服务的uuid字符串
     * @param charUuid	特征值的uuid字符串
     */
    private void setNotification(String serviceUuid, String charUuid) {
        final UUID tsUuid = UUID.fromString(serviceUuid);
        final UUID tcUuid = UUID.fromString(charUuid);

        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                BleManager.getInstance().getmBLEDevice()
                        .setCharacteristicNotification(tsUuid, tcUuid, true);

            }
        }, 500);

    }

    // 广播
    public static final String BLE_ACTION_CONNECTION_CHANGE = "com.tchip.tchipblehelper.action_CONNECTION_CHANGE";
    public static final String BLE_ACTION_CHARACTERISTIC_CHANGE = "com.tchip.tchipblehelper.action_CHARACTERISTIC_CHANGE";
    public static final String BLE_ACTION_CHARACTERISTIC_READ = "com.tchip.tchipblehelper.action_CHARACTERISTIC_READ";
    public static final String BLE_ACTION_CHARACTERISTIC_WRITE_STATE = "com.tchip.tchipblehelper.action_CHARACTERISTIC_WRITE_STATE";
    public static final String BLE_ACTION_SERVICES_DISCOVERED = "com.tchip.tchipblehelper.action_SERVICES_DISCOVERED";
    public static final String BLE_FINISHU = "com.tchip.finishu";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UUID uuid;
            String data;
            switch (intent.getAction()) {
                case BLE_ACTION_CONNECTION_CHANGE:
                    int laststate = intent.getIntExtra("laststate", 0);
                    int newstate = intent.getIntExtra("newstate", 0);
                    onBLEDeviceConnectionChange(laststate, newstate);
                    break;
                case BLE_ACTION_CHARACTERISTIC_CHANGE:
                    uuid = UUID.fromString(intent.getStringExtra("uuid"));
                    data = intent.getStringExtra("data");
                    onCharacteristicChanged(uuid, data);
                   // Toast.makeText(MainAty.this,"BLE_ACTION_CHARACTERISTIC_CHANGE",Toast.LENGTH_SHORT).show();
                    break;
                case BLE_ACTION_CHARACTERISTIC_READ:
                    uuid = UUID.fromString(intent.getStringExtra("uuid"));
                    data = intent.getStringExtra("data");
                    onCharacteristicRead(uuid, data);
                   // Toast.makeText(MainAty.this,"BLE_ACTION_CHARACTERISTIC_READ",Toast.LENGTH_SHORT).show();
                    break;
                case BLE_ACTION_CHARACTERISTIC_WRITE_STATE:
                    uuid = UUID.fromString(intent.getStringExtra("uuid"));
                    int state = intent.getIntExtra("state", 0);
                    onCharacteristicWriteState(uuid, state);
                   // Toast.makeText(MainAty.this,"BLE_ACTION_CHARACTERISTIC_WRITE_STATE",Toast.LENGTH_SHORT).show();
                    break;
                case BLE_ACTION_SERVICES_DISCOVERED:
                    onServicesDiscovered();
                   // Toast.makeText(MainAty.this,"BLE_ACTION_SERVICES_DISCOVERED",Toast.LENGTH_SHORT).show();
                    break;
                case BLE_FINISHU:
                    unRegisterReciver();
                    finish();
                    break;
            }
        }
    };

    /**
     * 注册广播监听
     */
    public void registerReciver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLE_ACTION_CHARACTERISTIC_CHANGE);
        filter.addAction(BLE_ACTION_CHARACTERISTIC_READ);
        filter.addAction(BLE_ACTION_CHARACTERISTIC_WRITE_STATE);
        filter.addAction(BLE_ACTION_CONNECTION_CHANGE);
        filter.addAction(BLE_ACTION_SERVICES_DISCOVERED);
        filter.addAction(BLE_FINISHU);
        this.registerReceiver(broadcastReceiver, filter);

    }

    /**
     * 取消广播监听
     */
    public void unRegisterReciver() {
        this.unregisterReceiver(broadcastReceiver);
    }

    public void onBLEDeviceConnectionChange(int laststate, int newstate) {
        // TODO Auto-generated method stub

        if (mHandler == null)
            return;
        Message msg = mHandler.obtainMessage();
        msg.what = HANDLER_ON_CONNECTION_CHANGED;
        msg.arg1 = newstate;
        mHandler.sendMessage(msg);
    }

    public void onCharacteristicRead(UUID uuid, String data) {
        // TODO Auto-generated method stub
        System.out.println("Dlist onCharacteristicRead data=" + data);

    }

    public void onCharacteristicChanged(UUID uuid, String data) {
        // TODO Auto-generated method stub
        System.out.println("Dlist onCharacteristicChanged data=" + data);
    }

    public void onServicesDiscovered() {
        // TODO Auto-generated method stub
        System.out.println("Dlist onServicesDiscovered");

    }

    public void onCharacteristicWriteState(UUID uuid, int state) {
        // TODO Auto-generated method stub
        System.out.println("Dlist onCharacteristicWriteState ");

    }

    /**
     * 搜索蓝牙服务
     * @return
     */
    private ArrayList<BluetoothGattService> searchServices() {
        int i = 0;
        BluetoothGatt gatt = BleManager.getInstance().getmBLEDevice()
                .getBluetoothGatt();
        ArrayList<BluetoothGattService> services;
        if (gatt != null) {
            services = (ArrayList<BluetoothGattService>) gatt.getServices();
        } else {
            return null;
        }
        for (BluetoothGattService serivce : services) {
            // System.out.println("Service[" + i + "]"
            // + serivce.getUuid().toString());
            i++;
        }
        return services;
    }

    /****************************************  end  **************************************************/

    /**
     * 后台任务类
     */
    public class BleTasks extends AsyncTask<Void,Integer,Boolean>{

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try{
                while (true)
                {
                    for(int i = 0 ;i < BleManager.getInstance().getmLeDeviceListAdapter().getCount();i++)
                    {
                        if(BleManager.getInstance().getmLeDeviceListAdapter().getItem(i).getDeviceName() .matches(my_dev_name) == true)
                        {
                            for(int j = 0;j < 3 ;j++)
                            {
                                BleManager.getInstance().prepareBLEDevice(uuidQppService,BleManager.getInstance());
                                BleManager.getInstance().setDevice(i);
                                BleManager.getInstance().connect();
                                if(BleManager.getInstance().getConnectionState() == BleManager.STATE_CONNECTING){
                                    BleManager.getInstance().onBLEDeviceConnectionChange(BleManager.getInstance().getmBLEDevice(),BleManager.STATE_CONNECTING,BleManager.STATE_CONNECTED);
                                }
                            }
                            if(BleManager.getInstance().getConnectionState()==BleManager.STATE_CONNECTING) {
                                BleManager.getInstance().onBLEDeviceConnectionChange(BleManager.getInstance().getmBLEDevice(),BleManager.STATE_CONNECTING,BleManager.STATE_CONNECTED);
                                return true;
                            }
                            conn_successful_flag = true;
                        }
                    }

                }
            }catch (Exception e){
                conn_successful_flag = false;
                return false;
            }

        }

        @Override
        protected void onProgressUpdate(Integer... values){
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }
    }
}




