package hw.dt83.udpchat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	static final String LOG_TAG = "UDPchat";
	static final int AUDIO_PORT = 50000;
	private AudioCall audioCall;
	private boolean inCall = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final Button btnStartCall = (Button) findViewById(R.id.startCall);
		final Button btnStop = (Button) findViewById(R.id.stop);
		btnStop.setEnabled(false);
		displayLocalIp();
		
		btnStartCall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "btnStartCall pressed");
				
				if(!inCall) {
					InetAddress address = getHostAddress();
					audioCall = new AudioCall(address, AUDIO_PORT);
					audioCall.startCall();
					inCall = true;
					btnStartCall.setEnabled(false);
					btnStop.setEnabled(true);
				}
			}
		});
		
		btnStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "btnStop pressed");
				
				if(inCall) {
					try {
						audioCall.endCall();
						inCall = false;
						btnStop.setEnabled(false);
						btnStartCall.setEnabled(true);
					}
					catch (Exception e){
						Log.e(LOG_TAG, "ERROR WITH END CALL: " + e);
					}
				}
			}
		});
	}
	
	public InetAddress getHostAddress() {
		try {
			EditText ip_edit = (EditText) findViewById(R.id.ipAddress);
			InetAddress address = InetAddress.getByName(ip_edit.getText().toString());
			Log.i(LOG_TAG, "Requested IP: "+ address.toString());
			return address;
		} 
		catch (UnknownHostException e) {
			Log.e(LOG_TAG, "Unknown host exception: " + e);
			return null;
		}
	}
	
	public void displayLocalIp() {
		TextView ip = (TextView) findViewById(R.id.thisIP);
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String address = intToIp(ipAddress);
		ip.setText("Your IP: " + address);
	}
	
	public String intToIp(int i) {
		return (i & 0xFF) + "." +
				((i >> 8) & 0xFF) + "." +
				((i >> 16) & 0xFF) + "." +
				((i >> 24) & 0xFF);
	}
}
