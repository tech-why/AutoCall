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
     * ���������־ 
     */  
    public void startLogObserver() {  
        Log.i(TAG,"startObserverLog");  
        isObserverLog = true;  
        Thread mTherad = new Thread(this);  
        mTherad.start();  
    }  
  
    /** 
     * �رռ����־ 
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
     * ����log���� 
     * @param logContent 
     */  
    private void sendLogContent(String logContent){  
        mBundle.putString("log",logContent);  
        mIntent.putExtras(mBundle);  
        mIntent.setAction(ActivityMain.LOG_ACTION);  
        Log.i("tags","���͹㲥���� ");
        sendBroadcast(mIntent);  
        
    }  
      
	/*@Override  
    public void run() {  
        Process pro = null; 
        
     try {  
            Runtime.getRuntime().exec("logcat -c").waitFor();  
              
            pro = Runtime.getRuntime().exec("logcat -b radio");  
            Log.i("tags","����jjjjjjxxxx");
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
                    //����log����  
                    sendLogContent(logContent.toString()); 
                    Log.i("tags","���ڷ���log");
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
                /** ��ȡϵͳlogcat��־��Ϣ */
                
                //�൱��������������  logcat -s dalvikm ,  -s��ʾ���ˣ� ������������ʾ���˵����������û�е��������������鳤��2���϶�Ҳ�ǿ��Եġ�������logcat��ʹ      �÷���
                String[] running=new String[]{ "logcat","-s","dalvikvm" };
                logcatProcess = Runtime.getRuntime().exec(running);
                                
                bufferedReader = new BufferedReader(new InputStreamReader(
                                logcatProcess.getInputStream()));

                String line;
                //ɸѡ��Ҫ���ִ�
                String strFilter="Could not find method";
                
                while ((line = bufferedReader.readLine()) != null) {
                        //����ÿ��log��Ϣ
                        System.out.println(line);
                        if (line.indexOf(strFilter) >= 0) {
                                /** ��⵽strFilter��log��־��䣬��������Ҫ�Ĵ��� */
                        Log.i("tags","ִ�е���");
                                break;
                                }
                }

        } catch (Exception e) {

                e.printStackTrace();
        }
}
}  