package com.fgc.autocall.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.simonvt.menudrawer.MenuDrawer;

import com.android.internal.telephony.ITelephony;
import com.fgc.autocall.R;
import com.fgc.autocall.Tools.StringTools;
import com.fgc.autocall.Tools.Tools;
import com.fgc.autocall.app.business.LogObserverService;
import com.fgc.autocall.app.business.MessageSender;
import com.fgc.autocall.app.business.MicroPhone;
import com.fgc.autocall.app.component.ContactParser;
import com.fgc.autocall.app.component.FileLoader;
import com.fgc.autocall.app.component.StorageManagerFgc;
import com.fgc.autocall.app.component.FileLoader.LoadObserver;
import com.fgc.autocall.constant.Constans;
import com.fgc.autocall.data.ContactPerson;
import com.fgc.autocall.data.ContactPersonWrapper;
import com.fgc.autocall.ui.OneByOneWork.OnWorkingObserver;
import com.fgc.autocall.ui.component.ButtonTwoState;
import com.fgc.autocall.ui.component.SideMenu;
import com.fgc.autocall.ui.component.ButtonTwoState.OnTwoStateSwitchListener;
import com.fgc.autocall.ui.component.SideMenu.OnMenuItemClickObserver;

import android.app.Instrumentation.ActivityResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.gsm.*;

public class ActivityMain extends BaseActivity {
	private SimBroadcastReciver mSimBroadcastReciver=null;
	private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";  
	  public static String LOG_ACTION = "com.fgc.log.LOG_ACTION"; 
	  private Intent logObserverIntent = null;  
	    private LogBroadcastReceiver mLogBroadcastReceiver = null;  
	private static final String LOG_TAG = "ActivityMain";
	
	private RelativeLayout mLayoutTitleBar;
	private ButtonTwoState mButtonStartPause;
		
	private SideMenu mSideMenu;
	
	private LinearLayout mLayoutWraning;
	private LinearLayout mLayoutWraningOk;
	private TextView mTextOk;
	
	private List<ContactPersonWrapper> mContactPersonWrappers = new ArrayList<ContactPersonWrapper>();
	
	private ContactsListViewWrapper mContactListViewWrapper;
	
	private OneByOneWork mOneByOneWork;
	
	private TextView mTextContactState;
	private TextView mTextSimState;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		
		// keep screen always on
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		mSideMenu = new SideMenu(this);
		mSideMenu.getMenuDrawer().setContentView(R.layout.a_main);
		mSideMenu.setOnMenuItemClickObserver(mOnMenuItemClickObserver);
		initView();
		
		mOneByOneWork = new OneByOneWork(this, getMainLooper());
		mOneByOneWork.setOnWorkingObserver(mOnWorkingObserver);
		registerLogBroadcastReceiver();  
		registerSmsBroadcastReceiver();
		
	}

	private void initView()
	{
		mLayoutTitleBar = (RelativeLayout)findViewById(R.id.title_bar);
		Button btnLeft = (Button)mLayoutTitleBar.findViewById(R.id.title_btn_left);
		btnLeft.setBackgroundResource(R.drawable.selector_btn_menu);
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)btnLeft.getLayoutParams();
		lp.leftMargin = Tools.dip2px(this, 8);
		btnLeft.setLayoutParams(lp);
		btnLeft.setOnClickListener(mOnClickListener);
		mButtonStartPause = (ButtonTwoState)mLayoutTitleBar.findViewById(R.id.title_btn_right);
		mButtonStartPause.setTwoStateDrawble(R.drawable.selector_btn_start, R.drawable.selector_btn_pause);
		mButtonStartPause.setOnTwoStateSwitchListener(mOnStatePauseSwitchListener);
		
		mLayoutWraning = (LinearLayout)findViewById(R.id.layout_wraning);
		mLayoutWraningOk = (LinearLayout)findViewById(R.id.layout_warning_ok);
		mLayoutWraningOk.setOnClickListener(mOnClickListener);
		mTextOk = (TextView)findViewById(R.id.text_ok);
		
		TextView notice = (TextView)findViewById(R.id.notice1);
		ListView listView = (ListView)findViewById(R.id.list1);
		mContactListViewWrapper = new ContactsListViewWrapper(this, notice, listView);
		
		mTextContactState = (TextView)findViewById(R.id.contact_state);
		mTextSimState = (TextView)findViewById(R.id.sim_state);
		
		mSideMenu.init();
		loadContactsData();
	}
	
	private void loadContactsData()
	{
		List<String> allPaths = new StorageManagerFgc(this).getMountedStoragePaths();
		int existContactFileIndex = -1;
		for (int i=0; i<allPaths.size(); i++)
		{
			if (Tools.isExistOneFileInOneDir( Constans.FileName.FILE_NAME, allPaths.get(i)))
			{
				existContactFileIndex = i;
				break;
			}
		}
		if (existContactFileIndex == -1)
		{
			Log.w(LOG_TAG, "not exist this file !");
			mLayoutWraning.setVisibility(View.VISIBLE);
			return;
		}
		String filePath = allPaths.get(existContactFileIndex) + "/" + Constans.FileName.FILE_NAME;
		Log.i(LOG_TAG, "file path: " + filePath);
		FileLoader fileLoader = new FileLoader(filePath);
		fileLoader.load(new LoadObserver() {
			
			@Override
			public void onloadOver(boolean isSuccess, List<String> contentList) {
				// TODO Auto-generated method stub
				if (!isSuccess)
				{
					Log.i(LOG_TAG, "can not get contacts file");
					mLayoutWraning.setVisibility(View.VISIBLE);
				}
				else
				{
					Log.i(LOG_TAG, "got contacts file");
					mLayoutWraning.setVisibility(View.GONE);
					ContactParser parser = new ContactParser(contentList);
					List<ContactPerson> persons = new ArrayList<ContactPerson>();
					parser.parse(persons);
					mContactPersonWrappers.clear();
					for (ContactPerson person : persons)
					{
						mContactPersonWrappers.add(new ContactPersonWrapper(person));
						Log.i(LOG_TAG, "person: " + person.toString());
					}
					mContactListViewWrapper.add(mContactPersonWrappers);
					mOneByOneWork.resetContacts(mContactPersonWrappers);
				}
			}
		});
	}

	private OnTwoStateSwitchListener mOnStatePauseSwitchListener = new OnTwoStateSwitchListener()
	{

		@Override
		public void onSwitch(boolean isPositive, int btnId) {
			// TODO Auto-generated method stub
			Log.i(LOG_TAG, "is start: " + isPositive);
			if (isPositive)
			{
				// pause
				mOneByOneWork.pauseWork();
			}
			else
			{
				  startLogObserverService();
				// start
				int internal = mApp.getConfigManager().getCallInternal();
				mOneByOneWork.setCallInternal(internal);
				mOneByOneWork.startWork(mApp.getConfigManager().isCall(), mApp.getConfigManager().isSendMessage());
				
			}
		}
		
	};
	
	private OnWorkingObserver mOnWorkingObserver = new OnWorkingObserver()
	{
		@Override
		public void onDoWork(int index, int workType)
		{
			Log.i(LOG_TAG, "working index : " + index + "  working type: " + workType);
			mContactListViewWrapper.setWorkingState(index, workType);
		}

		@Override
		public void onOver(int overIndex, int totalCount) {
			// TODO Auto-generated method stub
			String noticeOver = "已完成     " + "( " + (overIndex+1) + "/" + totalCount + " )";
 			Log.i(LOG_TAG, "over : " + noticeOver);
 			mTextContactState.setText(noticeOver);
		}
	};

	
	private OnClickListener mOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Log.i(LOG_TAG, "onClick");
			
			switch (v.getId())
			{
			case R.id.title_btn_left:
				Log.i(LOG_TAG, "click title_btn_left");
				mSideMenu.open();
				break;
			case R.id.layout_warning_ok:
				Log.i(LOG_TAG, "click layout_warning_ok");
				String detecting = ActivityMain.this.getResources().getString(R.string.warning_detecting);
				if (detecting.equals(mTextOk.getText()))
				{
					Log.w(LOG_TAG, "is detecting !");
					return;
				}
				mTextOk.setText(detecting);
				loadContactsData();
				mCheckFileHandler.sendEmptyMessageDelayed(0, 4000);
				break;
			}
		}
	};
	
	
	private Handler mCheckFileHandler = new Handler()
	{
		@Override
		public void handleMessage(android.os.Message msg)
		{
			mTextOk.setText(ActivityMain.this.getResources().getString(R.string.warning_ok));
		}
	};

    private OnMenuItemClickObserver mOnMenuItemClickObserver = new OnMenuItemClickObserver()
    {

		@Override
		public void onClickMenuItem(int position) {
			// TODO Auto-generated method stub
			
			mSideMenu.close();
			
            switch(position)
            {
	            case SideMenu.MENU_INDEX_SETTING:{
	            	Log.i(LOG_TAG, "MENU_INDEX_SETTING");
	            	Intent intent = new Intent(ActivityMain.this, ActivitySettings.class);
	            	startActivity(intent);
	            	break;
	            }
	            case SideMenu.MENU_INDEX_EXPORT_CALL_RECORD:
	            {
	            	Log.i(LOG_TAG, "MENU_INDEX_EXPORT_CALL_RECORD");
	            	Intent intent = new Intent(ActivityMain.this, ActivityExportRecord.class);
	            	startActivity(intent);
	            	break;
	            }
	            case SideMenu.MENU_INDEX_CHECK_SIM_CARD_INFO:
	            {
	            	Log.i(LOG_TAG, "MENU_INDEX_CHECK_SIM_CARD_INFO");
	            	Intent intent = new Intent(ActivityMain.this, ActivitySimCard.class);
	            	startActivity(intent);
	            	break;
	            }
	            case SideMenu.MENU_INDEX_ABOUT_US:{
	            	Log.i(LOG_TAG, "MENU_INDEX_ABOUT_US");
	            	Intent intent = new Intent(ActivityMain.this, ActivityAboutUs.class);
	            	startActivity(intent);
	            	break;
	            }
            }
		}
    };
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(LOG_TAG, "onNewIntent");
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		Log.i(LOG_TAG, "onStart");
	}
	
	@Override
	protected void onRestart()
	{
		super.onRestart();
		Log.i(LOG_TAG, "onRestart");
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(LOG_TAG, "onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(LOG_TAG, "onPause");
	}

	@Override
	protected void onStop() {
		Log.i(LOG_TAG, "onStop");
		super.onStop();
		
	}

	@Override
	protected void onDestroy() {
		Log.i(LOG_TAG, "onDestroy");
		mOneByOneWork.stopWork();
		
		super.onDestroy();
		if(logObserverIntent!=null){
			 stopService(logObserverIntent);  
			}
			 unregisterReceiver(mLogBroadcastReceiver);  
		
	}

	@Override
	protected boolean ifFinishAppByBackKeyPress(){
		return true;
	}
	
	//广播接收
	private void startLogObserverService() {  
        logObserverIntent = new Intent(this, LogObserverService.class);  
        startService(logObserverIntent);  
    }  
	 /** 
     * 注册log广播接收者 
     */  
    private void registerLogBroadcastReceiver(){  
        mLogBroadcastReceiver = new LogBroadcastReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(LOG_ACTION);  
        registerReceiver(mLogBroadcastReceiver, filter);  
        Log.i("tags","注册成功");
    }  		
	
    private class LogBroadcastReceiver extends BroadcastReceiver{  
        private String action = null;  
        private Bundle mBundle = null;  
        public void onReceive(Context context, Intent intent) {  
            action = intent.getAction();  
            if(LOG_ACTION.equals(action)){  
                mBundle = intent.getExtras();  
                //监听出现问题的电话
           String problemlog=mBundle.getString("log"); 
         Log.i("tags", problemlog);
      
            }  
        }  
    } 
    
    
    private void registerSmsBroadcastReceiver(){  
    	mSimBroadcastReciver= new SimBroadcastReciver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(ACTION_SIM_STATE_CHANGED);  
        registerReceiver(mSimBroadcastReciver, filter);  
        Log.i("tags","注册成功");
    }  		
    private class SimBroadcastReciver extends BroadcastReceiver{
    	
        private final static int SIM_VALID = 0;  
        private final static int SIM_INVALID = 1;  
        private int simState = SIM_INVALID;  
          
        public int getSimState() {  
            return simState;  
        }  
      
        @Override  
        public void onReceive(Context context, Intent intent) {  
            Log.i("sms","发生改变了sms1"); 
            if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {  
                TelephonyManager tm = (TelephonyManager)context.getSystemService(context.TELEPHONY_SERVICE);   
                int state = tm.getSimState();  
                switch (state) {  
                case TelephonyManager.SIM_STATE_READY :  
                    simState = SIM_VALID;  
                    Log.i("sms","发生改变了sms");
                    String imsi=tm.getSubscriberId();
                    Log.i("sms","imsi"+imsi);
                    break;  
                case TelephonyManager.SIM_STATE_UNKNOWN :  
                case TelephonyManager.SIM_STATE_ABSENT :  
                case TelephonyManager.SIM_STATE_PIN_REQUIRED :  
                case TelephonyManager.SIM_STATE_PUK_REQUIRED :  
                case TelephonyManager.SIM_STATE_NETWORK_LOCKED :  
                default:  
                    simState = SIM_INVALID;  
                    break;  
                }  
             
            }  
        }  
    }
    
    
	
}
