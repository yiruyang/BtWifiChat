package com.example.admin.btwifichat.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import com.example.admin.btwifichat.R;
import com.example.admin.btwifichat.adater.MRecycleAdapter;
import com.example.admin.btwifichat.bean.ItemEntity;
import com.example.admin.btwifichat.service.BluetoothChatService;
import com.example.admin.btwifichat.service.WifiChatService;
import com.example.admin.btwifichat.util.TipTool;
import com.example.admin.btwifichat.widget.NavController;
import com.example.admin.btwifichat.widget.SpaceItemDecoration;
import com.example.admin.btwifichat.widget.SwitchButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.admin.btwifichat.util.TipTool.showToast;

public class ControlActivity extends AppCompatActivity {

    //debug
    private static final String TAG = "ControlActivity";

    //member fields
    private BluetoothChatService mChatService;
    private ProgressDialog btDialog;
    private String mConnectedDeviceName;
    private WifiChatService mWifiService;

    //view
    private RecyclerView mRecyclerView;
    private NavController controlView;
    private SwitchButton bluetoothSb,wifiSb;
    private SeekBar speedBar;
    private AlertDialog.Builder builder;

    //adapter
    private MRecycleAdapter mRecycleAdapter;
    private BluetoothAdapter mBTAdapter;

    //list
    private List<ItemEntity> list;

    // bluetooth intent request code;
    private static final int REQUEST_CONNECT_DEVICE=6;
    private static final int REQUEST_ENABLE_BT = 7;

    //wifi intent request code
    private static final int REQUEST_NETWORK=10;


    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    //bluetooth key
    public static final String TOAST="toast";
    public static final String DEVICE_NAME = "device_name";

    //wifi state
    public static final int SOCKET_CLOSED=20;
    public static final int RECEIVE_MSG=21;
    public static final int CONNECT_FAILED=22;
    public static final int CONNECT_SUCCESS=23;

    //wifi key
    public static final String MSG="msg";
    public static final String NETWORK_NAME="network_name";

   //是否已连上wifi
    private boolean isConnect;

    private boolean isWifi;
    private boolean isBluetooth;

    //保存wifi ip和端口号的key
    private static final String IP_KEY="ip_key";
    public static final String PORT_KEY="port_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        Log.i(TAG, "onCreate: sin=="+ Math.asin(2/2.828427)*180/Math.PI);
        initView();
        loadData();
        initService();
        initBlueTooth();

    }

    private void initView() {
        //方向控制按钮
        controlView = ((NavController) findViewById(R.id.control_view));
        controlView.setOnNavAndSpeedListener(new NavController.OnNavAndSpeedListener() {
            @Override
            public void onNavAndSpeed(float nav, float speed) {

                Log.i(TAG, "onNavAndSpeed: nav=="+nav+"   speed=="+speed);
                sendMessage(String.valueOf(nav)+"\n");
            }
        });

        mRecyclerView = ((RecyclerView) findViewById(R.id.custom_setting));
        mRecyclerView.setLayoutManager(new GridLayoutManager(this,4));
        //设置recyclerView item 间距
        int space = getResources().getDimensionPixelOffset(R.dimen.recycler_view_space);
        mRecyclerView.addItemDecoration(new SpaceItemDecoration(space));

        mRecycleAdapter=new MRecycleAdapter(this,this);
        mRecyclerView.setAdapter(mRecycleAdapter);

        speedBar = ((SeekBar) findViewById(R.id.speed_seekBar));
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                sendMessage(String.valueOf(progress));
            }
        });

        //蓝牙开关
        bluetoothSb = (SwitchButton) findViewById(R.id.bluetooth_switch);
        bluetoothSb.setOnChangeListener(new SwitchButton.OnChangeListener() {
            @Override
            public void onChange(SwitchButton sb, boolean state) {

                Log.i(TAG, "onChange: bluetooth state=="+state);
                Intent serverIntent;
                if (state){

                    if (!mBTAdapter.isEnabled()){
                        serverIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(serverIntent,REQUEST_ENABLE_BT);
                    }
                }else {
                    if (mBTAdapter.isEnabled()){
                        mBTAdapter.disable();
                    }
                    mChatService.stop();
                }
            }
        });
        //wifi开关
        wifiSb= (SwitchButton) findViewById(R.id.wifi_switch);
        wifiSb.setOnChangeListener(new SwitchButton.OnChangeListener() {
            @Override
            public void onChange(SwitchButton sb, boolean state) {

                Log.i(TAG, "onChange: wifi state=="+state);
                WifiManager wifiManager= (WifiManager) getSystemService(Context.WIFI_SERVICE);;
                if (state){
                    wifiManager.setWifiEnabled(true);
                }else {
                    wifiManager.setWifiEnabled(false);
                    mWifiService.stop();
                }
            }
        });
    }

    private void loadData() {

        list=new ArrayList<>();
        for (int i=0;i<12;i++){
            list.add(new ItemEntity());
        }
        mRecycleAdapter.addData(list);
    }

    private void initService() {

        mChatService=new BluetoothChatService(getApplicationContext(),mHandler);
        mWifiService=new WifiChatService(mHandler);

    }

    /**
     * 获取蓝牙适配器
     * */
    private void initBlueTooth() {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBTAdapter==null){
            showToast(getApplicationContext(),"该设备不支持蓝牙功能");
            finish();
            return;
        }
    }

    /**
     * 跳转到找寻蓝牙设备的activity
     * */
    private void findBluetoothDevices() {

        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent,REQUEST_CONNECT_DEVICE);
    }

    private void findNetWork(){
        Intent intent = new Intent(this, NetworkListActivity.class);
        startActivityForResult(intent,REQUEST_NETWORK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            //当蓝牙设备列表请求被连接时
            case REQUEST_CONNECT_DEVICE:
                if (resultCode==RESULT_OK){
                    Log.i(TAG, "onActivityResult:bluetooth ");
                    connectDevice(data);
                }
                break;
            //wifi列表被点击时
            case REQUEST_NETWORK:
                if (resultCode==RESULT_OK){
                    Log.i(TAG, "onActivityResult: wifi");
                }
                break;
        }
    }

    private void connectDevice(Intent data) {
        //获取设备的mac地址
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //获取远程蓝牙设备
        BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

        //连接设备
        mChatService.connect(device);

        btDialog = new ProgressDialog(ControlActivity.this);
        btDialog.setMessage(getResources().getString(R.string.connecting));

    }

    /**
     * 发送消息且判断是通过wifi还是蓝牙
     * */
    public void sendMessage(String msg){

        if (msg!=null){
            //通过蓝牙发送
            if (isBluetooth){
                if (mChatService.getState()!=BluetoothChatService.STATE_CONNECTED){
                    showToast(getApplicationContext(),getResources().getString(R.string.not_connected_device));
                    return;
                }

                if (msg.length()>0){
                    byte[] buf=msg.getBytes();
                    mChatService.write(buf);
                }else {

                    showToast(getApplicationContext(),getResources().getString(R.string.message_not_null));
                }
            }
            Log.i(TAG, "sendMessage: iswifi=="+isWifi);
            //通过wifi发送
            if (isWifi){
                connect(msg);
            }
            //没有选择wifi或蓝牙通信时，提示用户
            if (!isBluetooth&&!isWifi){
                TipTool.showToast(getApplicationContext(),getResources().getString(R.string.no_connect_device));
            }

        }else {

            TipTool.showToast(getApplicationContext(),getResources().getString(R.string.message_not_null));
        }
    }

    private HashMap<String,String> set=new HashMap<>();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void connect(String msg){

        Log.i(TAG, "connect: isConnect=="+isConnect);
        if (!isConnect){

            View view = LayoutInflater.from(this).inflate(R.layout.dialog_connect, null);
            final EditText ipView = (EditText) view.findViewById(R.id.dialog_ip);
            final EditText portView = ((EditText) view.findViewById(R.id.dialog_port));

            if (set.size()>0){
                ipView.setText(set.get(IP_KEY));
                portView.setText(set.get(PORT_KEY));
            }

            final AlertDialog.Builder connectDialog=new AlertDialog.Builder(this);
            connectDialog.setView(view);

            connectDialog.setTitle(getResources().getString(R.string.connect));
            connectDialog.setPositiveButton(getResources().getString(R.string.connect), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    String ip=ipView.getText().toString().trim();
                    String port=portView.getText().toString().trim();
                    set.put(IP_KEY,ip);
                    set.put(PORT_KEY,port);

                    mWifiService.start(ip, Integer.parseInt(port));
                }
            });

            connectDialog.setNegativeButton(getResources().getString(R.string.dialog_negative),null);
            connectDialog.create().show();

        }else {
            mWifiService.sendMsg(msg);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        switch (itemId) {

            case R.id.discover_bt_device:

                if (mBTAdapter.isEnabled()){

                    //停止wifi通信
                    mWifiService.stop();
                    isWifi=false;
                    isBluetooth=true;

                    findBluetoothDevices();
                }else {
                    showToast(getApplicationContext(),getResources().getString(R.string.open_bt_first));
                }

                break;
            case R.id.discover_wifi:

                @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                if (wifiManager==null){
                    showToast(getApplicationContext(),getResources().getString(R.string.unsupport_wifi));
                }

                if (wifiManager.isWifiEnabled()){

                    //停止蓝牙通信
                    mChatService.stop();
                    isBluetooth=false;
                    isWifi=true;

                    findNetWork();
                }else {
                    showToast(getApplicationContext(),getResources().getString(R.string.open_wifi_first));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {

                //bluetooth
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        //正在连接
                        case BluetoothChatService.STATE_CONNECTING:
                            Log.i(TAG, "handleMessage: is connecting");
                            btDialog.show();
                            break;

                        //已连接
                        case BluetoothChatService.STATE_CONNECTED:
                            Log.i(TAG, "handleMessage: lian jie cheng gong");
                            btDialog.dismiss();
                            break;
                    }
                    break;

                case MESSAGE_READ:
                    byte[] readBuf= (byte[]) msg.obj;
                    if (readBuf==null)return;
                    TipTool.showToast(getApplicationContext(),readBuf.toString()+getResources().getString(R.string.receive_message));
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf= (byte[]) msg.obj;

                    break;

                //连接上了设备
                case MESSAGE_DEVICE_NAME:
                    isBluetooth=true;
                    mConnectedDeviceName=msg.getData().getString(DEVICE_NAME);
                    //提示用户连接成功
                    showToast(getApplicationContext(),mConnectedDeviceName+
                            getResources().getString(R.string.connect_successful));
                    break;

                case MESSAGE_TOAST:
                    showToast(getApplicationContext(),
                            msg.getData().getString(TOAST));
                    btDialog.dismiss();
                    break;

                // wifi
                case RECEIVE_MSG:
                    TipTool.showToast(getApplicationContext(),getResources().getString(R.string.receive_successful));
                    Bundle data=msg.getData();
                    String result = data.getString(MSG, null);
                    if (result!=null){
                        TipTool.showToast(getApplicationContext(),result+getResources().getString(R.string.receive_message_from_wifi));
                    }
                    break;

                case CONNECT_SUCCESS:
                    isConnect=true;
                    TipTool.showToast(getApplicationContext(),getResources().getString(R.string.connect_successful));
                    break;

                case CONNECT_FAILED:
                case SOCKET_CLOSED:
                    isConnect=false;
                    TipTool.showToast(getApplicationContext(),getResources().getString(R.string.connect_exception));
                    break;
            }
            }
        };
}
