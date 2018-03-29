package backup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ControlChannelListener implements Runnable
{
	private MulticastSocket socket;
	private String recovery_adr;
	private int recovery_port;
	private ThreadPoolExecutor control_pool;
	ConcurrentHashMap<String,Integer> records_backup;
	ConcurrentHashMap<String,Integer> records_store;
	ConcurrentHashMap<String,Integer> records_restore;
	private String server_id;
	final static int MAX_PACKET_SIZE=64096;
	
	public ControlChannelListener(String serverid,ConcurrentHashMap<String,Integer> recbac,ConcurrentHashMap<String,Integer> recsto, ConcurrentHashMap<String, Integer> recs_restore, String adr, int port, String recoveryadr, int recoveryport) throws IOException 
	{	
		socket = new MulticastSocket(port);
		InetAddress control_adr = InetAddress.getByName(adr);
		socket.joinGroup(control_adr);
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		control_pool = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, queue);
		records_backup=recbac;
		records_store=recsto;
		records_restore=recs_restore;
		recovery_adr=recoveryadr;
		recovery_port=recoveryport;
		server_id=serverid;
	}
	
	public void run()
	{
		while(true)
		{
			byte[] buf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				control_pool.execute(new ControlChannelPacketHandler(packet,server_id,records_backup,records_store,records_restore,recovery_adr,recovery_port,socket));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initiateDelete(String filename) throws IOException, NoSuchAlgorithmException, InterruptedException
	{
		control_pool.execute(new SendDelete(filename));
		
	}
	
	public class SendDelete implements Runnable
	{
		String filename;
		public SendDelete(String filen) {
			filename=filen;
		}
		@Override
		public void run() {
			try {
				int nr_tries = 0;
				String fileID = Utils.createFileID(filename);
				byte[] delete = CreateMessages.createHeader("DELETE", "1.0", server_id, fileID, 0,0);
				InetAddress control_addr = InetAddress.getByName("239.0.0.0");
				DatagramPacket packet = new DatagramPacket(delete,0,delete.length,control_addr,8888);
				while(nr_tries<3) {
					socket.send(packet);
					Thread.sleep(300);
					nr_tries++;
				}
				System.out.println(fileID);
				Utils.deleteFile(fileID,server_id,records_backup,records_store);
				System.out.println("Delete terminated!");
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
}

