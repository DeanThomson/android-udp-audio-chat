package hw.dt83.udpchat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

	static final String LOG_TAG = "UDPchat";
	private ContactManager contactManager;
	private HashMap<String, InetAddress> contacts;
	private String displayName;
	private boolean STARTED = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Log.i(LOG_TAG, "App started");
		
		final Button btnStart = (Button) findViewById(R.id.buttonStart);
		btnStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Log.i(LOG_TAG, "Start button pressed");
				STARTED = true;
				EditText displayNameText = (EditText) findViewById(R.id.editTextDisplayName);
				displayName = displayNameText.getText().toString();
				
				displayNameText.setEnabled(false);
				btnStart.setEnabled(false);
				
				TextView text = (TextView) findViewById(R.id.textViewSelectContact);
				text.setVisibility(View.VISIBLE);
				
				Button updateButton = (Button) findViewById(R.id.buttonUpdate);
				updateButton.setVisibility(View.VISIBLE);
				
				Button callButton = (Button) findViewById(R.id.buttonCall);
				callButton.setVisibility(View.VISIBLE);
				
				ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
				scrollView.setVisibility(View.VISIBLE);
				
				contactManager = new ContactManager(displayName, getBroadcastIp());				
			}
		});
		
		final Button btnUpdate = (Button) findViewById(R.id.buttonUpdate);
		btnUpdate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				updateContactList();
			}
		});
	}
	
	private void updateContactList() {
		
		contacts = contactManager.getContacts();
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.contactList);
		radioGroup.removeAllViews();
		
		for(String name : contacts.keySet()) {
			
			RadioButton radioButton = new RadioButton(getBaseContext());
			radioButton.setText(name);
			radioGroup.addView(radioButton);
		}
	}
	
	private InetAddress getBroadcastIp() {
		
		try {
			
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			String addressString = toBroadcastIp(ipAddress);
			InetAddress broadcastAddress = InetAddress.getByName(addressString);
			return broadcastAddress;
		}
		catch(UnknownHostException e) {
			
			Log.e(LOG_TAG, "UnknownHostException in getBroadcastIP: " + e);
			return null;
		}
		
	}
	
	private String toBroadcastIp(int ip) {
		return (ip & 0xFF) + "." +
				((ip >> 8) & 0xFF) + "." +
				((ip >> 16) & 0xFF) + "." +
				"255";
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(STARTED) {
			
			contactManager.stopBroadcasting();
			contactManager.stopListening();
		}
		Log.i(LOG_TAG, "App paused!");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(STARTED) {
			
			contactManager.stopBroadcasting();
			contactManager.stopListening();
		}
		Log.i(LOG_TAG, "App stopped");
	}
}
