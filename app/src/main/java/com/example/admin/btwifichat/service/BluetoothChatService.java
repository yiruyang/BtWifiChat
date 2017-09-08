package com.example.admin.btwifichat.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.admin.btwifichat.R;
import com.example.admin.btwifichat.activity.ControlActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by admin on 2017/3/31.
 */

public class BluetoothChatService {

    //debug
    private static final String TAG = "BluetoothChatService";
    
    //uuid
    private static final UUID MY_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private Handler mHandler;
    private Context mContext;
    private int mState;

    //thread
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    
    public BluetoothChatService(Context context,Handler handler){

        mContext=context;
        mHandler=handler;
        mState=STATE_NONE;
    }

    /**
     * stop all thread
     * */
    public synchronized void stop(){

        if (mConnectThread!=null){mConnectThread.cancel();mConnectThread=null;}

        if (mConnectedThread!=null){mConnectedThread.cancel();mConnectedThread=null;}
    }

    /**
     * 发送数据
     * */
    public void write(byte[] out){
        //创建临时对象
        ConnectedThread r;

        synchronized (BluetoothChatService.this) {
            r=mConnectedThread;
        }
        r.write(out);
    }

    public synchronized void connect(BluetoothDevice device){

        Log.i(TAG, "connect: ");
        //停止当前正在连接设备的线程
        if (mConnectThread!=null){
            mConnectThread.cancel();
            mConnectedThread=null;
        }

        //停止当前正在通信的线程
        if (mConnectedThread!=null){mConnectedThread.cancel();mConnectedThread=null;}

        //开启连接蓝牙设备的线程
        mConnectThread=new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothDevice device,BluetoothSocket socket){

        //停止当前在连接设备的线程
        if (mConnectThread!=null){mConnectThread.cancel();mConnectThread=null;}
        //停止当前正在通信的线程
        if (mConnectedThread!=null){mConnectedThread.cancel();mConnectedThread=null;}

        //开启通信线程
        mConnectedThread=new ConnectedThread(socket);
        mConnectedThread.start();

        //将正在通信设备的名称发给主线程
        Message message = mHandler.obtainMessage(ControlActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(ControlActivity.DEVICE_NAME,device.getName());
        message.setData(bundle);
        mHandler.sendMessage(message);

        setState(STATE_CONNECTED);
    }

    //将设备的状态告诉主线程
    private void setState(int state) {
        mState=state;
        mHandler.obtainMessage(ControlActivity.MESSAGE_STATE_CHANGE,state,-1).sendToTarget();
    }

    public int getState(){
        return mState;
    }

    class ConnectThread extends Thread{

        private BluetoothDevice mDevice;
        private BluetoothSocket mBtSocket;

        public ConnectThread(BluetoothDevice device){
            
            mDevice=device;
            BluetoothSocket tmp=null;

            try {
                tmp=mDevice.createRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "ConnectThread exception", e);
            }
            mBtSocket=tmp;
        }

        @Override
        public void run() {

            Log.i(TAG, "run: begin connect");
            try {
                //connect()方法会阻塞线程，所以在子线程执行
                mBtSocket.connect();
                mHandler.obtainMessage(ControlActivity.MESSAGE_STATE_CHANGE,STATE_CONNECTING,-1).sendToTarget();

            } catch (IOException e) {
                Log.e(TAG, "connect failed",e );
                try {
                    mBtSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "socket close exception",e1 );
                }
                connectionFailed();
                return;
            }
            if (mBtSocket.isConnected()) {
                Log.i(TAG, "run: is connected");
                mHandler.obtainMessage(ControlActivity.MESSAGE_STATE_CHANGE,STATE_CONNECTED,-1).sendToTarget();
            }
            //重置线程
            synchronized (BluetoothChatService.class){
                mConnectThread=null;
            }

            connected(mDevice,mBtSocket);

        }

        public void cancel(){
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ConnectedThread extends Thread{

        private BluetoothSocket mSocket;
        private InputStream mis;
        private OutputStream mos;

        public ConnectedThread(BluetoothSocket socket){

            Log.i(TAG, "ConnectedThread: ");
            mSocket=socket;
            InputStream tmpIn = null;
            OutputStream tmpOs = null;

            try {
                tmpIn=mSocket.getInputStream();
                tmpOs=mSocket.getOutputStream();

            } catch (IOException e) {
                Log.e(TAG, "获取输入输出流失败",e );
            }
            mis=tmpIn;
            mos=tmpOs;

        }

        @Override
        public void run() {

            Log.i(TAG, "run: connected");
            byte[] buf=new byte[1024];
            int len;

            while (true){

                try {
                    len=mis.read(buf);
                    mHandler.obtainMessage(ControlActivity.MESSAGE_READ,len,-1,buf);

                } catch (IOException e) {

                    Log.e(TAG, "读取数据异常",e );
                    connectionFailed();

                    BluetoothChatService.this.stop();
                }
            }
        }
        /**
         * 发送数据
         * */
        public void write(byte[] buffer){

            try {
                mos.write(buffer);
                //通知主线程刷新UI
                mHandler.obtainMessage(ControlActivity.MESSAGE_WRITE,-1,-1,buffer).sendToTarget();
            } catch (IOException e) {

                Log.e(TAG, "Exception during writing",e);
            }
        }

        public void cancel(){

            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close of connected socket failed",e);
            }
        }
    }

    public void connectionFailed(){

        Message message = mHandler.obtainMessage(ControlActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ControlActivity.TOAST,mContext.getResources().getString(R.string.connect_failed));
        message.setData(bundle);
        message.sendToTarget();
    }

    /**
     * 连接断开了并通知主线程
     * */
    public void connectionLost(){

        Message message = mHandler.obtainMessage(ControlActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ControlActivity.TOAST,mContext.getResources().getString(R.string.btDevice_connect_failed));
        message.setData(bundle);
        message.sendToTarget();
    }
}
