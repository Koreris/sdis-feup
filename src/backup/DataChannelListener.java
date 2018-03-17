package backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class DataChannelListener implements Runnable
{
	private MulticastSocket socket;
	private InetAddress data_adr;
	private int port;
	private int rep_degree;
	private String file_to_backup;
	private ThreadPoolExecutor data_pool;
	private String server_id; 
	final static int MAX_PACKET_SIZE=64096;
	
	public DataChannelListener(String serverID) throws IOException 
	{
		socket = new MulticastSocket(7777);	
		InetAddress mcast_addr = InetAddress.getByName("239.0.0.1");
		socket.joinGroup(mcast_addr);
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		data_pool = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, queue);
		server_id=serverID;
	}

	public void run()
	{
		while(true)
		{
			byte[] buf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				data_pool.execute(new DataChannelPacketHandler(packet));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initiateBackup(String filename,int rep_deg) 
	{
		rep_degree=rep_deg;
		file_to_backup=filename;
		//send putchunk
		byte[] header=CreateMessages.createHeader("PUTCHUNK", "1.0", server_id, filename, 0, rep_deg);
	}
	
	public void sendMessage(byte[] msg)
	{
		DatagramPacket packet = new DatagramPacket(msg, msg.length,954);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendFile(String FILE_TO_SEND) throws IOException {
		 FileInputStream fis = null;
		    BufferedInputStream bis = null;
		   

          // send file
          File myFile = new File (FILE_TO_SEND);
          byte [] mybytearray  = new byte [(int)myFile.length()];
          fis = new FileInputStream(myFile);
          bis = new BufferedInputStream(fis);
          bis.read(mybytearray,0,mybytearray.length);
          
          bis.close();
	}
	
	private class DataChannelPacketHandler implements Runnable 
	{
		DatagramPacket data;
		public DataChannelPacketHandler(DatagramPacket packet) 
		{
			data=packet;
		}

		@Override
		public void run() 
		{
			String packetString = new String(data.getData(),0,data.getLength());
			String[] lines = packetString.split(System.getProperty("line.separator"));
			String header = lines[0];
			System.out.println(lines.length);
		}
		
	}
}
