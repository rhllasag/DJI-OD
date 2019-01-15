package com.dji.videostreamdecodingsample.services;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.dji.videostreamdecodingsample.activities.MainActivity;
import com.dji.videostreamdecodingsample.main.Constants;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class Server implements  Runnable{
	MainActivity mActivityInstance;
	String message = null;
	private int socketServerPORT = Constants.IP_STREAMING;
	private Context mContext;
	private Handler mHandler;
	public Server(Context context, Handler handler) {
		super();
		mContext=context;
		mHandler = handler;
		mActivityInstance = (MainActivity) mContext;
	}

	public int getPort() {
		return socketServerPORT;
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(socketServerPORT);
			mActivityInstance.infoip.setText(this.getIpAddress()+":"+this.getPort());
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mActivityInstance.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							if(message!=null)
								mActivityInstance.infoip.setText(message);
						}
					});
				}
			});
			while (true) {

				Socket socket = serverSocket.accept();
				message = "#"+socket.toString()  + " from "
						+ socket.getInetAddress() + ":"
						+ socket.getPort() + "\n";
				new Thread(new SocketServerReplyThread(socket)).start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private class SocketServerReplyThread implements Runnable {

		private Socket hostThreadSocket;
		private OutputStream outputStream = null;

		SocketServerReplyThread(Socket socket) {
			hostThreadSocket = socket;

		}
		@Override
		public void run() {
			if(hostThreadSocket!=null){
				try {
					hostThreadSocket.setKeepAlive(true);
					outputStream = hostThreadSocket.getOutputStream();
					while (true){
						DataOutputStream dos = new DataOutputStream(outputStream);
						dos.write(intToByteArray(mActivityInstance.mFrames.size()));
						System.out.println("Delay Frame:"+mActivityInstance.timesTampNeeded);
						dos.flush();
						dos.write(mActivityInstance.mFrames.toByteArray());
						dos.flush();
						Thread.sleep(mActivityInstance.timesTampNeeded);
					}
                } catch (Exception e) {
					e.printStackTrace();
					try {
						if (outputStream!= null)
							outputStream.close();

					} catch (Exception e2) {
						e.printStackTrace();
					}
				}

				mActivityInstance.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if(message!=null)
							mActivityInstance.infoip.setText(message);
					}
				});
			}
			else
			{
				Log.d("SOCKET", "Socket is null");
			}
		}

	}

	private byte[] intToByteArray(int data) {
		byte result[] = new byte[4];
		result[3] = (byte) ((data & 0xFF000000) >> 24);
		result[2] = (byte) ((data & 0x00FF0000) >> 16);
		result[1] = (byte) ((data & 0x0000FF00) >> 8);
		result[0] = (byte) ((data & 0x000000FF) >> 0);
		return  result;
	}

	public String getIpAddress() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
					.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces
						.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface
						.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress
							.nextElement();

					if (inetAddress.isSiteLocalAddress()) {
						ip += "Server running at : "
								+ inetAddress.getHostAddress();
					}
				}
			}

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ip += "Something Wrong! " + e.toString() + "\n";
		}
		return ip;
	}
}
