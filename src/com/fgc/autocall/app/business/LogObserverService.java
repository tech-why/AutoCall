package com.fgc.autocall.app.business;
import java.io.DataInputStream;  
import java.io.IOException;  
import com.fgc.autocall.ui.ActivityMain;
import android.app.Service;  
import android.content.Intent;  
import android.os.Bundle;  
import android.os.IBinder;  
import android.util.Log;  
public class LogObserverService extends Service implements Runnable{  
    private String TAG = "LogObserverService";  
    private boolean isObserverLog = false;  
    private StringBuffer logContent = null;  
    private Bundle mBundle = null;  
    private Intent mIntent = null;  
    @Override  
    public IBinder onBind(Intent intent) {  
        return null;  
    }  
  
    @Override  
    public void onCreate() {  
        super.onCreate();  
        Log.i(TAG,"onCreate");  
        mIntent = new Intent();  
        mBundle = new Bundle();  
        logContent = new StringBuffer();  
        startLogObserver();  
    }  
  
    /** 
     * 开启检测日志 
     */  
    public void startLogObserver() {  
        Log.i(TAG,"startObserverLog");  
        isObserverLog = true;  
        Thread mTherad = new Thread(this);  
        mTherad.start();  
    }  
  
    /** 
     * 关闭检测日志 
     */  
    public void stopLogObserver() {  
        isObserverLog = false;  
    }  
  
    @Override  
    public void onDestroy() {  
        super.onDestroy();  
        stopLogObserver();  
    }  
  
    /** 
     * 发送log内容 
     * @param logContent 
     */  
    private void sendLogContent(String logContent){  
        mBundle.putString("log",logContent);  
        mIntent.putExtras(mBundle);  
        mIntent.setAction(ActivityMain.LOG_ACTION);  
        sendBroadcast(mIntent);  
    }  
      
      
    @Override  
    public void run() {  
        Process pro = null;  
        try {  
            Runtime.getRuntime().exec("logcat -c").waitFor();  
              
            pro = Runtime.getRuntime().exec("logcat -b radio");  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
  
        DataInputStream dis = new DataInputStream(pro.getInputStream());  
        String line = null;  
        while (isObserverLog) {  
            try {  
                while ((line = dis.readLine()) != null) {  
                    String temp = logContent.toString();  
                    logContent.delete(0, logContent.length());  
                    logContent.append(line);  
                    logContent.append("\n");  
                    logContent.append(temp);  
                    //发送log内容  
                    sendLogContent(logContent.toString());  
                    Thread.yield();  
                }  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
    }  

}  
