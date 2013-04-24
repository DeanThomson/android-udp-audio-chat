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
	public static final int BROADCAST_PORT = 50001; // Socket on which packets are sent/received
	private static final int BROADCAST_INTERVAL = 10000; // Milliseconds
	private static final int BROADCAST_BUF_SIZE = 1024;
	private boolean BROADCAST = true;
	private boolean LISTEN = true;
	private HashMap<String, InetAddress> contacts;
	private InetAddress broadcastIP;
	
	public ContactManager(String name, InetAddress broadcastIP) {
		
		contacts = new HashMap<String, InetAddress>();
		this.broadcastIP = broadcastIP;
		listen();
		broadcastName(name, broadcastIP);
	}
	
	public HashMap<String, InetAddress> getContacts() {
		
		return contacts;
	}
	
	public void addContact(String name, InetAddress address) {
		// If the contact is not already known to us, add it
		if(!contacts.containsKey(name)) {
			
			Log.i(LOG_TAG, "Adding contact: " + name);
			contacts.put(name, address);
			Log.i(LOG_TAG, "#Contacts: " + contacts.size());
			return;
		}
		Log.i(LOG_TAG, "Contact already exists: " + name);
		return;
	}
	
	public void removeContact(String name) {
		// If the contact is known to us, remove it
		if(contacts.containsKey(name)) {
			
			Log.i(LOG_TAG, "Removing contact: " + name);
			contacts.remove(name);
			Log.i(LOG_TAG, "#Contacts: " + contacts.size());
			return;
		}
		Log.i(LOG_TAG, "Cannot remove contact. " + name + " does not exist.");
		return;
	}
	
	public void bye(final String name) {
		// Sends a Bye notification to other devices
		Thread byeThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					Log.i(LOG_TAG, "Attempting to broadcast BYE notification!");
					String notification = "BYE:"+name;
					byte[] message = notification.getBytes();
					DatagramSocket socket = new DatagramSocket();
					socket.setBroadcast(true);
					DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
					socket.send(packet);
					Log.i(LOG_TAG, "Broadcast BYE notification!");
					socket.disconnect();
					socket.close();
					return;
				}
				catch(SocketException e) {
					
					Log.e(LOG_TAG, "SocketException during BYE notification: " + e);
				}
				catch(IOException e) {
					
					Log.e(LOG_TAG, "IOException during BYE notification: " + e);
				}
			}
		});
		byeThread.start();
	}
	
	public void broadcastName(final String name, final InetAddress broadcastIP) {
		// Broadcasts the name of the device at a regular interval
		Log.i(LOG_TAG, "Broadcasting started!");
		Thread broadcastThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					
					String request = "ADD:"+name;
					byte[] message = request.getBytes();
					DatagramSocket socket = new DatagramSocket();
					socket.setBroadcast(true);
					DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
					while(BROADCAST) {
						
						socket.send(packet);
						Log.i(LOG_TAG, "Broadcast packet sent: " + packet.getAddress().toString());
						Thread.sleep(BROADCAST_INTERVAL);
					}
					Log.i(LOG_TAG, "Broadcaster ending!");
					socket.disconnect();
					socket.close();
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
		// Ends the broadcasting thread
		BROADCAST = false;
	}
	
	public void listen() {
		// Create the listener thread
		Log.i(LOG_TAG, "Listening started!");
		Thread listenThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
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
				socket.disconnect();
				socket.close();
				return;
			}
			
			public void listen(DatagramSocket socket, byte[] buffer) {
				
				try {
					//Listen in for new notifications
					Log.i(LOG_TAG, "Listening for a packet!");
					DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
					socket.setSoTimeout(15000);
					socket.receive(packet);
					String data = new String(buffer, 0, packet.getLength());
					Log.i(LOG_TAG, "Packet received: " + data);
					String action = data.substring(0, 4);
					if(action.equals("ADD:")) {
						// Add notification received. Attempt to add contact
						Log.i(LOG_TAG, "Listener received ADD request");
						addContact(data.substring(4, data.length()), packet.getAddress());
					}
					else if(action.equals("BYE:")) {
						// Bye notification received. Attempt to remove contact
						Log.i(LOG_TAG, "Listener received BYE request");
						removeContact(data.substring(4, data.length()));
					}
					else {
						// Invalid notification received
						Log.w(LOG_TAG, "Listener received invalid request: " + action);
					}
					
				}
				catch(SocketTimeoutException e) {
					
					Log.i(LOG_TAG, "No packet received!");
					if(LISTEN) {
					
						listen(socket, buffer);
					}
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
		// Stops the listener thread
		LISTEN = false;
	}
}
