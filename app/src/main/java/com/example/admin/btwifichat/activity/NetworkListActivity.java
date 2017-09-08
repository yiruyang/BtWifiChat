package com.example.admin.btwifichat.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.admin.btwifichat.R;
import com.example.admin.btwifichat.util.TipTool;
import com.example.admin.btwifichat.widget.TitleLayout;

import java.util.List;

public class NetworkListActivity extends AppCompatActivity {

    //debug
    private static final String TAG = "NetworkListActivity";

    private ListView networkList;
    private ListView newNetworkList;

    private List<ScanResult> scanResults;

    private ArrayAdapter<String> networkAdapter;
    private ArrayAdapter<String> newNetworkAdapter;
    private WifiManager wifiManager;
    private String password;

    //action
    public static final String RSSI_CHANGED="android.NET.wifi.RSSI_CHANGED";
    private List<WifiConfiguration> configuredNetworks;
    private TitleLayout networkTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_work_list);

        //隐藏actionbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null)actionBar.hide();

        initWifiManager();
        initView();
        initReceiver();

    }

    private void initReceiver() {
        IntentFilter filter=new IntentFilter();
        filter.addAction(RSSI_CHANGED);
    }

    private void initWifiManager() {
        wifiManager= (WifiManager) getSystemService(WIFI_SERVICE);
    }

    private void initView() {

        networkTitle = ((TitleLayout) findViewById(R.id.network_title_layout));
        networkTitle.setTitle(getResources().getString(R.string.network_title));

        networkList = ((ListView) findViewById(R.id.paired_network_list));
        newNetworkList = ((ListView) findViewById(R.id.new_network_list));

        networkAdapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        newNetworkAdapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);

        //已连接的wifi列表
        networkList.setAdapter(networkAdapter);
        //没有配置过的wifi列表
        newNetworkList.setAdapter(newNetworkAdapter);

        getConfiguration();
        newNetworkList.setOnItemClickListener(mNetworkClickListener);

        findViewById(R.id.scan_network_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                scanResults = wifiManager.getScanResults();
                loadData();
            }
        });

    }

    private void loadData() {

        newNetworkAdapter.clear();
        if (scanResults.size()>0){

            findViewById(R.id.title_new_network).setVisibility(View.VISIBLE);
            for (ScanResult scanResult : scanResults) {
                newNetworkAdapter.add(scanResult.SSID+"\n"+scanResult.BSSID);
            }
        }else {
            Log.i(TAG, "loadData: scanResults size is 0");
        }
    }

    private AdapterView.OnItemClickListener mNetworkClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


            //此处应该是连上点击的wifi
            String info = ((TextView) view).getText().toString();
            String ssid = info.substring(0, info.indexOf("\n"));
            Log.i(TAG, "onItemClick: ssid=="+ssid);

            Intent intent=new Intent("android.net.wifi.PICK_WIFI_NETWORK");
            startActivity(intent);

            //跳转到系统的wifi界面
            Intent data=new Intent();
            intent.putExtra(ControlActivity.NETWORK_NAME,ssid);
            setResult(Activity.RESULT_OK,data);
            finish();

     /*       inputPassword();
            if (getPassword()!=null){
                int wifiId = addWifiConfig(scanResults,ssid,getPassword());
                connectWifi(wifiId);
            }else {
                Log.i(TAG, "onItemClick: password is null");
            }*/

            }
    };

    public void inputPassword(){

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pwd, null);
        final EditText pwdEt = (EditText) view.findViewById(R.id.dialog_pwd_et);

        final AlertDialog.Builder inputPwd=new AlertDialog.Builder(NetworkListActivity.this);

        inputPwd.setView(view);
        inputPwd.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String pwd=pwdEt.getText().toString().trim();
                setPassword(pwd);
                dialog.dismiss();
//                NetworkListActivity.this.finish();

            }
        });

        if (!isFinishing()){
            inputPwd.create().show();
        }
    }

    public void setPassword(String pwd){
        this.password=pwd;
    }

    public String getPassword(){
        return this.password;
    }


    public void getConfiguration(){

        configuredNetworks = wifiManager.getConfiguredNetworks();
        WifiInfo info = wifiManager.getConnectionInfo();

        if (info!=null){
            findViewById(R.id.title_paired_network).setVisibility(View.VISIBLE);
                networkAdapter.add(info.getSSID());
        }else {
            Log.i(TAG, "getConfiguration: 没有已配置过的wifi");
        }

    }

    /**
     * 如果需要连接的WIFI没有配置好，即没有保存密码。
     * 则为指定名称ssid的WIFI添加密码信息psw，添加成功后返回给其分配的networId，同于连接
     * */
    public int addWifiConfig(List<ScanResult> wifiList,String ssid,String pwd){

        int wifiId=-1;
        for (ScanResult scanResult : wifiList) {
            if (scanResult.SSID.equals(ssid)){
                WifiConfiguration wificong=new WifiConfiguration();
                wificong.SSID="\""+scanResult.SSID+"\"";// \"转义字符代表"
                wificong.preSharedKey="\""+pwd+"\"";//WPA-PSK密码
                wificong.hiddenSSID=false;
                wificong.status=WifiConfiguration.Status.ENABLED;
                //将配置好的特定WIFI密码信息添加,添加完成后默认是不激活状态，成功返回ID，否则为-1
                wifiId=wifiManager.addNetwork(wificong);
                if (wifiId!=-1)
                    return wifiId;
            }
        }
        return -1;
    }

    /**
     *通过networkId连接指定的wifi
     * */
    public boolean connectWifi(int wifiId){

        for (WifiConfiguration network : configuredNetworks) {
            if (network.networkId==wifiId){
                //激活该id建立连接
                while(!(wifiManager.enableNetwork(wifiId,true))){
                    //status:0--已经连接，1--不可连接，2--可以连接
                    Log.i(TAG, String.valueOf(configuredNetworks.get(wifiId).status));
                }
                return true;
            }
        }
        return false;
    }

    class mWifiReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            //wifi信号强度
            if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)){

            }
            //wifi是否已连接
            else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){

                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)){
                    TipTool.showToast(getApplicationContext(),getResources().getString(R.string.network_disconnected));
                }
                else if (info.getState().equals(NetworkInfo.State.CONNECTED)){

                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    //获取当前已连接wifi名称
                    TipTool.showToast(getApplicationContext(), wifiInfo.getSSID());
                }
            }
            //wifi是否打开
            else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){

                int wifiState=intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_DISABLED);

                if (wifiState==WifiManager.WIFI_STATE_DISABLED){
                    TipTool.showToast(getApplicationContext(),getResources().getString(R.string.wifi_disabled));
                }
                else if (wifiState==WifiManager.WIFI_STATE_ENABLED){
                    TipTool.showToast(getApplicationContext(),getResources().getString(R.string.wifi_enabled));
                }
            }
        }
    }


}
