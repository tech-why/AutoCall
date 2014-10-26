package com.fgc.autocall.app.business;

import java.io.BufferedReader;
import java.io.DataInputStream;  
import java.io.IOException;  
import java.io.InputStreamReader;
  

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
        Log.i("tags","发送广播内容 ");
        sendBroadcast(mIntent);  
        
    }  
      
	/*@Override  
    public void run() {  
        Process pro = null; 
        
     try {  
            Runtime.getRuntime().exec("logcat -c").waitFor();  
              
            pro = Runtime.getRuntime().exec("logcat -b radio");  
            Log.i("tags","正在jjjjjjxxxx");
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
 
        DataInputStream dis = new DataInputStream(pro.getInputStream());  
        try {
			Log.i("tags",dis.readLine());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        	BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));
        	try {
				Log.i("tags",reader.readLine());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        
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
                    Log.i("tags","正在发送log");
                    Thread.yield();  
                }  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
    }  */
    @Override
    public void run() {
        Process logcatProcess = null;
        BufferedReader bufferedReader = null;
        try {
                /** 获取系统logcat日志信息 */
                
                //相当于在命令行运行  logcat -s dalvikm ,  -s表示过滤， 第三个参数表示过滤的条件。如果没有第三个参数，数组长度2，肯定也是可以的。下面有logcat的使      用方法
                String[] running=new String[]{ "logcat","-s","dalvikvm" };
                logcatProcess = Runtime.getRuntime().exec(running);
                                
                bufferedReader = new BufferedReader(new InputStreamReader(
                                logcatProcess.getInputStream()));

                String line;
                //筛选需要的字串
                String strFilter="Could not find method";
                
                while ((line = bufferedReader.readLine()) != null) {
                        //读出每行log信息
                        System.out.println(line);
                        if (line.indexOf(strFilter) >= 0) {
                                /** 检测到strFilter的log日志语句，进行你需要的处理 */
                        Log.i("tags","执行到这");
                                break;
                                }
                }

        } catch (Exception e) {

                e.printStackTrace();
        }
}
}  