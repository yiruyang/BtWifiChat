package com.example.admin.btwifichat.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.admin.btwifichat.activity.ControlActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by admin on 2017/3/22.
 */

public class WifiChatService {

    //debug
    private static final String TAG = "WifiChatService";
    // ui handle
    private Handler mHandler;
    //连接服务器的线程
    private ClientThread clientThread;
    //发送数据的线程
    private SendThread sendThread;
    //输出流
    private OutputStream outputStream;

    private Socket mSocket;

    public WifiChatService(Handler handler){
        mHandler=handler;
    }

    public void sendMsg(String msg){

        sendThread=null;
        sendThread=new SendThread(msg);
        sendThread.start();
    }

    public void start(String serverIp, int port){

        clientThread=null;
        clientThread=new ClientThread(serverIp,port);
        clientThread.start();
    }

    public void stop(){

        if (mSocket!=null){
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (clientThread!=null){
            clientThread=null;
        }

        if (sendThread!=null){
            sendThread=null;
        }
    }


    class SendThread extends Thread {

        private String msg;

        public SendThread(String string){
            msg=string;
        }

        @Override
        public void run() {
            try {

                    outputStream=mSocket.getOutputStream();
                    outputStream.write(msg.getBytes("utf-8"));
                    outputStream.flush();

            } catch (IOException e) {
                Log.e(TAG, "发送异常",e );

            }
        }
    }

    class ClientThread extends Thread {

        //要连接的服务器ip和端口号
        private String serverIp;
        private int mPort;

        public ClientThread(String ip, int port){
            serverIp=ip;
            mPort=port;
        }
        @Override
        public void run() {

            InetAddress inet = null;
            Bundle bundle = new Bundle();
            bundle.clear();

            //连接服务器
            try {

                inet = InetAddress.getByName(serverIp);

            } catch (UnknownHostException e) {
                Log.e(TAG, "host exception", e);
                connectFailed();
            }

            try {

                mSocket=new Socket();
                mSocket.connect(new InetSocketAddress(inet,mPort),1000*10);//超过10s则认为连接超时
                //如果连接成功，通知主线程
                boolean connected = mSocket.isConnected();
                if (connected){
                    mHandler.obtainMessage(ControlActivity.CONNECT_SUCCESS).sendToTarget();
                }

                //获取输入流
                InputStream is=mSocket.getInputStream();
                BufferedReader br=new BufferedReader(new InputStreamReader(is));

                //读取服务器发来的信息
                int len;
                byte[] buf=new byte[1024];
                StringBuffer buffer=new StringBuffer();

                while (true){
                    len=is.read(buf);

                    if (buffer.length()>0)buffer.delete(0,buffer.length()-1);
                    buffer.append(new String(buf,0,len));

                    if (buffer.toString().contains("\n")){
                        bundle.putString(ControlActivity.MSG,buffer.toString());
                        //notice UI thread refresh
                        Message message = mHandler.obtainMessage(ControlActivity.RECEIVE_MSG);
                        message.setData(bundle);
                        message.sendToTarget();
                    }
                }

            } catch (IOException e) {

                Log.e(TAG, "run: connect fail",e );
                connectFailed();
            }
        }
        
        private void connectFailed(){
            mHandler.obtainMessage(ControlActivity.CONNECT_FAILED).sendToTarget();
            return;
        }
    }
}
