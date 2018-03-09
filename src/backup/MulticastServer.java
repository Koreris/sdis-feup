package backup;
import java.net.*;
import java.rmi.RemoteException;
import java.io.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MulticastServer implements RMI_backup_service
{
	private String id;
	private String protocol;
	private String access_point;
	private String control_address;
	private String control_port;
	private ControlChannelListener control_thread;
	private DataChannelListener data_thread;
	protected enum ChannelState{SEND_PUTCHUNK,NEUTRAL,BACKUP_WAIT};
	
	public MulticastServer(String ident, String proto, String access_point) {
		
	}
	
	public MulticastServer() {
		// TODO Auto-generated constructor stub
	}

	private static class ControlChannelListener implements Runnable
	{
		private MulticastSocket socket;
		private InetAddress	control_adr;
		private int port;
		private ThreadPoolExecutor control_pool;

		public ControlChannelListener() throws IOException 
		{
			socket = new MulticastSocket(8888);
			InetAddress mcast_addr = InetAddress.getByName("239.0.0.0");
			socket.joinGroup(mcast_addr);
			control_pool = new ThreadPoolExecutor(100, 500, 10, TimeUnit.SECONDS, null);
		}

		public void run()
		{
			while(true)
			{
					
			}
		}
	}

	private static class DataChannelListener implements Runnable
	{
		private MulticastSocket socket;
		private InetAddress data_adr;
		private int port;
		private ChannelState data_state;
		private int rep_degree;
		private String file_to_backup;
		private ThreadPoolExecutor data_pool;
		
		public DataChannelListener() throws IOException 
		{
			socket = new MulticastSocket(7777);	
			InetAddress mcast_addr = InetAddress.getByName("239.0.0.1");
			socket.joinGroup(mcast_addr);
			data_state=ChannelState.NEUTRAL;
			data_pool = new ThreadPoolExecutor(100, 500, 10, TimeUnit.SECONDS, null);
		}

		public void run()
		{
			while(true)
			{
				
	
			}
		}
		
		public void initiate_backup(String filename,int rep_deg) {
			data_state=ChannelState.SEND_PUTCHUNK;
			rep_degree=rep_deg;
			file_to_backup=filename;
		}
	}
	
	private void startup() {
		new Thread(control_thread).start();
		new Thread(data_thread).start();
	}
	
	public static void main(String[] args) throws IOException 
	{
		MulticastServer serv = new MulticastServer();
		serv.control_thread=new ControlChannelListener();
		serv.data_thread=new DataChannelListener();
		serv.startup();
	}

	@Override
	public String backup(String filename, int replication_degree) throws RemoteException {
		this.data_thread.initiate_backup(filename,replication_degree);
		return null;
	}

}
