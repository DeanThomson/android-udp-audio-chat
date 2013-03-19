package hw.dt83.udpchat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioCall {

	private static final String LOG_TAG = "UDPChat";
	private static final int SAMPLE_RATE = 8000;
	private static final int SAMPLE_INTERVAL = 20; // Milliseconds
	private static final int SAMPLE_SIZE = 2; // Bytes
	private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
	private InetAddress address; // Address to call
	private int port; // Port the packets are addressed to
	private boolean mic = false; // Enable mic?
	private boolean speakers = false; // Enable speakers?
	
	public AudioCall(InetAddress address, int port) {
		
		this.address = address;
		this.port = port;
	}
	
	public void startCall() {
		
		startMic();
		startSpeakers();
	}
	
	public void endCall() {
		
		muteMic();
		muteSpeakers();
	}
	
	public void muteMic() {
		
		mic = false;
	}
	
	public void muteSpeakers() {
		
		speakers = false;
	}
	
	public void startMic() {
		
		mic = true;
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				Log.i(LOG_TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());
				AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
						AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 
						AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);
				int bytes_read = 0;
				int bytes_sent = 0;
				byte[] buf = new byte[BUF_SIZE];
				try {
					
					Log.i(LOG_TAG, "Packet destination: " + address.toString());
					DatagramSocket socket = new DatagramSocket();
					audioRecorder.startRecording();
					while(mic) {
						
						bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
						DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, port);
						socket.send(packet);
						bytes_sent += bytes_read;
						Log.i(LOG_TAG, "Total bytes sent: " + bytes_sent);
						Thread.sleep(SAMPLE_INTERVAL, 0);
					}
					audioRecorder.stop();
					audioRecorder.release();
					socket.disconnect();
					socket.close();
					mic = false;
					return;
				}
				catch(InterruptedException e) {
					
					Log.e(LOG_TAG, "InterruptedException: " + e.toString());
					mic = false;
				}
				catch(SocketException e) {
					
					Log.e(LOG_TAG, "SocketException: " + e.toString());
					mic = false;
				}
				catch(UnknownHostException e) {
					
					Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
					mic = false;
				}
				catch(IOException e) {
					
					Log.e(LOG_TAG, "IOException: " + e.toString());
					mic = false;
				}
			}
		});
		thread.start();
	}
	
	public void startSpeakers() {
		
		if(!speakers) {
			
			speakers = true;
			Thread receiveThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					Log.i(LOG_TAG, "Receive thread started. Thread id: " + Thread.currentThread().getId());
					AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
							AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
					track.play();
					try {
						
						DatagramSocket socket = new DatagramSocket(port);
						byte[] buf = new byte[BUF_SIZE];
						while(speakers) {
							
							DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
							socket.receive(packet);
							Log.i(LOG_TAG, "Packet received: " + packet.getLength());
							track.write(packet.getData(), 0, BUF_SIZE);
						}
						socket.disconnect();
						socket.close();
						track.stop();
						track.flush();
						track.release();
						speakers = false;
						return;
					}
					catch(SocketException e) {
						
						Log.e(LOG_TAG, "SocketException: " + e.toString());
						speakers = false;
					}
					catch(IOException e) {
						
						Log.e(LOG_TAG, "IOException: " + e.toString());
						speakers = false;
					}
				}
			});
			receiveThread.start();
		}
	}
}
