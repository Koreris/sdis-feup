package backup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ControlChannelListener implements Runnable
{
	private MulticastSocket socket;
	private InetAddress	control_adr;
	private int port;
	private ThreadPoolExecutor control_pool;
	private ConcurrentHashMap<String,Integer> records;
	private String server_id;
	final static int MAX_PACKET_SIZE=64096;
	
	public ControlChannelListener(String serverid,ConcurrentHashMap<String,Integer> rec) throws IOException 
	{	
		socket = new MulticastSocket(8888);
		InetAddress mcast_addr = InetAddress.getByName("239.0.0.0");
		socket.joinGroup(mcast_addr);
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		control_pool = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, queue);
		records=rec;
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
				control_pool.execute(new ControlChannelPacketHandler(packet,server_id,records,socket));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

