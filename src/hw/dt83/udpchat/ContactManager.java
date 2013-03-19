package hw.dt83.udpchat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import android.util.Log;

public class ContactManager {

	private static final String LOG_TAG = "ContactManager";
	private static final int BROADCAST_PORT = 50001;
	private static final int BROADCAST_INTERVAL = 10000;
	private static final int BROADCAST_BUF_SIZE = 1024;
	private boolean BROADCAST = true;
	private boolean LISTEN = true;
	private HashMap<String, InetAddress> contacts;
	
	public ContactManager(String name, InetAddress broadcastIP) {
		
		contacts = new HashMap<String, InetAddress>();
		listen();
		broadcastName(name, broadcastIP);
	}
	
	public HashMap<String, InetAddress> getContacts() {
		
		return contacts;
	}
	
	public void addContact(String name, InetAddress address) {
		
		Log.i(LOG_TAG, "Attempting to add contact: " + name);
		if(!contacts.containsKey(name)) {
			
			Log.i(LOG_TAG, "Adding contact");
			contacts.put(name, address);
			Log.i(LOG_TAG, "#Contacts: " + contacts.size());
			return;
		}
		Log.i(LOG_TAG, "Contact already exists: " + name);
		return;
	}
	
	public void broadcastName(final String name, final InetAddress broadcastIP) {
		
		Log.i(LOG_TAG, "Broadcasting started!");
		Thread broadcastThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					Log.i(LOG_TAG, "Broadcasting thread created!");
					byte[] message = name.getBytes();
					DatagramSocket socket = new DatagramSocket();
					socket.setBroadcast(true);
					DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
					
					while(BROADCAST) {
						
						socket.send(packet);
						Log.i(LOG_TAG, "Broadcast packet sent: " + packet.getAddress().toString());
						Thread.sleep(BROADCAST_INTERVAL);
					}
					Log.i(LOG_TAG, "Broadcaster ending!");
					return;
				}
				catch(SocketException e) {
					
					Log.e(LOG_TAG, "SocketExceltion in broadcast: " + e);
					Log.i(LOG_TAG, "Broadcaster ending!");
					return;
				}
				catch(IOException e) {
					
					Log.e(LOG_TAG, "IOException in broadcast: " + e);
					Log.i(LOG_TAG, "Broadcaster ending!");
					return;
				}
				catch(InterruptedException e) {
					
					Log.e(LOG_TAG, "InterruptedException in broadcast: " + e);
					Log.i(LOG_TAG, "Broadcaster ending!");
					return;
				}
			}
		});
		
		broadcastThread.start();
	}
	
	public void stopBroadcasting() {
		
		BROADCAST = false;
	}
	
	public void listen() {
		
		Log.i(LOG_TAG, "Listening started!");
		Thread listenThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				Log.i(LOG_TAG, "Listening thread started!");
				DatagramSocket socket;
				try {
					
					socket = new DatagramSocket(BROADCAST_PORT);
				} 
				catch (SocketException e) {
					
					Log.e(LOG_TAG, "SocketExcepion in listener: " + e);
					return;
				}
				byte[] buffer = new byte[BROADCAST_BUF_SIZE];
					
				while(LISTEN) {
					
					listen(socket, buffer);
				}
				Log.i(LOG_TAG, "Listener ending!");
				return;
			}
			
			public void listen(DatagramSocket socket, byte[] buffer) {
				
				try {
					
					Log.i(LOG_TAG, "Listening for a packet!");
					DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
					socket.setSoTimeout(15000);
					socket.receive(packet);
					String data = new String(buffer, 0, packet.getLength());
					Log.i(LOG_TAG, "Packet received from " + data);
					addContact(data, packet.getAddress());
				}
				catch(SocketTimeoutException e) {
					
					Log.i(LOG_TAG, "No packet received!");
					if(LISTEN)
						listen(socket, buffer);
					return;
				}
				catch(SocketException e) {
					
					Log.e(LOG_TAG, "SocketException in listen: " + e);
					Log.i(LOG_TAG, "Listener ending!");
					return;
				}
				catch(IOException e) {
					
					Log.e(LOG_TAG, "IOException in listen: " + e);
					Log.i(LOG_TAG, "Listener ending!");
					return;
				}
			}
		});
		
		listenThread.start();
	}
	
	public void stopListening() {
		
		LISTEN = false;
	}
}
