package com.example.admin.btwifichat.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.btwifichat.R;
import com.example.admin.btwifichat.widget.TitleLayout;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    //debug
    private static final String TAG = "DeviceListActivity";
    //adapter
    private ArrayAdapter<String> pairedDeviceAdapter;
    private ArrayAdapter<String> newDeviceAdapter;

    private ListView pairedList,newList;
    private BluetoothAdapter mBtAdapter;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private ProgressDialog mProgressDialog;
    private TitleLayout titleBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_list);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null)actionBar.hide();

        //Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        initView();
        initBtn();
        initBroadcastReceiver();
    }

    /**
     * 初始化两个listview，并绑定adapter
     * 一个显示已经配对过的设备，一个显示没有配对的设备
     * */
    private void initView() {

        titleBar = ((TitleLayout) findViewById(R.id.title_bar));
        titleBar.setTitle(getResources().getString(R.string.device_list));

        pairedDeviceAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        newDeviceAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);

        pairedList = ((ListView) findViewById(R.id.paired_devices_list));
        pairedList.setAdapter(pairedDeviceAdapter);
        pairedList.setOnItemClickListener(mDeviceClickListener);

        newList = ((ListView) findViewById(R.id.new_devices_list));
        newList.setAdapter(newDeviceAdapter);
        newList.setOnItemClickListener(mDeviceClickListener);

        mBtAdapter=BluetoothAdapter.getDefaultAdapter();
        //获取已经配对的设备列表
        Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();

        findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);

        if (bondedDevices.size()>0){
            for (BluetoothDevice device : bondedDevices) {
                pairedDeviceAdapter.add(device.getName()+"\n"+device.getAddress());
            }
        }
    }

    private void initBtn() {
        findViewById(R.id.scan_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
            }
        });
    }

    private void initBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver,filter);
    }

    private AdapterView.OnItemClickListener mDeviceClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            //cancel discovery because it's costly and we're about to connect
            if (mBtAdapter.isDiscovering())mBtAdapter.cancelDiscovery();

            //Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            String address=info.substring(info.length()-17);

            Intent intent=new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS,address);

            setResult(Activity.RESULT_OK,intent);
            finish();
        }
    };

    /**
     * 通过蓝牙适配器查找设备
     * */
    private void doDiscovery(){

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getResources().getString(R.string.title));
        mProgressDialog.setMessage(getResources().getString(R.string.scanning));
        mProgressDialog.setCancelable(false);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        //如果已经在扫描就停止
        if (mBtAdapter.isDiscovering())mBtAdapter.cancelDiscovery();

        //重新开始扫描
        mBtAdapter.startDiscovery();
    }

    /**
     * 广播监听扫描的状态
     * */
    private BroadcastReceiver mReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            //开始扫描
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){

                Log.i(TAG, "onReceive: started");
                mProgressDialog.show();

            }
            //发现设备
            else if (action.equals(BluetoothDevice.ACTION_FOUND)){

                Log.i(TAG, "onReceive: found");
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //跳过已经配对过的设备，因为已经在列表里了
                if (device.getBondState()!=BluetoothDevice.BOND_BONDED){
                    newDeviceAdapter.add(device.getName()+"\n"+device.getAddress());
                }
            }
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){

                Log.i(TAG, "onReceive: finished");
                mProgressDialog.dismiss();
                if (newDeviceAdapter.getCount()==0){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.none_found),Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.scan_over),Toast.LENGTH_SHORT).show();

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);

        if (mBtAdapter!=null)mBtAdapter.cancelDiscovery();
    }
}
