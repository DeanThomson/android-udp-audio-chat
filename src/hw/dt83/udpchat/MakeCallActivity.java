package hw.dt83.udpchat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MakeCallActivity extends Activity {

	private static final String LOG_TAG = "MakeCall";
	private static final int BROADCAST_PORT = 50002;
	private static final int BUF_SIZE = 1024;
	private String displayName;
	private String contactName;
	private String contactIp;
	private boolean LISTEN = true;
	private boolean IN_CALL = false;
	private AudioCall call;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_make_call);
		
		Log.i(LOG_TAG, "MakeCallActivity started!");
		
		Intent intent = getIntent();
		displayName = intent.getStringExtra(MainActivity.EXTRA_DISPLAYNAME);
		contactName = intent.getStringExtra(MainActivity.EXTRA_CONTACT);
		contactIp = intent.getStringExtra(MainActivity.EXTRA_IP);
		
		TextView textView = (TextView) findViewById(R.id.textViewCalling);
		textView.setText("Calling: " + contactName);
		
		startListener();
		makeCall();
		
		Button endButton = (Button) findViewById(R.id.buttonEndCall);
		endButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Button to end the call has been pressed
				endCall();
			}
		});
	}
	
	private void makeCall() {
		// Send a request to start a call
		sendMessage("CAL:"+displayName, 50003);
	}
	
	private void endCall() {
		// Ends the chat sessions
		stopListener();
		if(IN_CALL) {
			
			call.endCall();
		}
		sendMessage("END:", BROADCAST_PORT);
		finish();
	}
	
	private void startListener() {
		// Create listener thread
		LISTEN = true;
		Thread listenThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					
					Log.i(LOG_TAG, "Listener started!");
					DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
					socket.setSoTimeout(15000);
					byte[] buffer = new byte[BUF_SIZE];
					DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
					while(LISTEN) {
						
						try {
							
							Log.i(LOG_TAG, "Listening for packets");
							socket.receive(packet);
							String data = new String(buffer, 0, packet.getLength());
							Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
							String action = data.substring(0, 4);
							if(action.equals("ACC:")) {
								// Accept notification received. Start call
								call = new AudioCall(packet.getAddress());
								call.startCall();
								IN_CALL = true;
							}
							else if(action.equals("REJ:")) {
								// Reject notification received. End call
								endCall();
							}
							else if(action.equals("END:")) {
								// End call notification received. End call
								endCall();
							}
							else {
								// Invalid notification received
								Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
							}
						}
						catch(SocketTimeoutException e) {
							if(!IN_CALL) {
								
								Log.i(LOG_TAG, "No reply from contact. Ending call");
								endCall();
								return;
							}
						}
						catch(IOException e) {
							
						}
					}
					Log.i(LOG_TAG, "Listener ending");
					socket.disconnect();
					socket.close();
					return;
				}
				catch(SocketException e) {
					
					Log.e(LOG_TAG, "SocketException in Listener");
					endCall();
				}
			}
		});
		listenThread.start();
	}
	
	private void stopListener() {
		// Ends the listener thread
		LISTEN = false;
	}
	
	private void sendMessage(final String message, final int port) {
		// Creates a thread used for sending notifications
		Thread replyThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					
					InetAddress address = InetAddress.getByName(contactIp);
					byte[] data = message.getBytes();
					DatagramSocket socket = new DatagramSocket();
					DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
					socket.send(packet);
					Log.i(LOG_TAG, "Sent message( " + message + " ) to " + contactIp);
					socket.disconnect();
					socket.close();
				}
				catch(UnknownHostException e) {
					
					Log.e(LOG_TAG, "Failure. UnknownHostException in sendMessage: " + contactIp);
				}
				catch(SocketException e) {
					
					Log.e(LOG_TAG, "Failure. SocketException in sendMessage: " + e);
				}
				catch(IOException e) {
					
					Log.e(LOG_TAG, "Failure. IOException in sendMessage: " + e);
				}
			}
		});
		replyThread.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.make_call, menu);
		return true;
	}

}
