package backup;

import java.io.IOException;
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

	public ControlChannelListener(String serverid,ConcurrentHashMap<String,Integer> rec) throws IOException 
	{	
		socket = new MulticastSocket(8888);
		InetAddress mcast_addr = InetAddress.getByName("239.0.0.0");
		socket.joinGroup(mcast_addr);
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		control_pool = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, queue);
		records=rec;
		server_id=serverid;
	}

	public void run()
	{
		while(true)
		{
				
		}
	}
}

