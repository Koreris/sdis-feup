package backup;

import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MulticastServer
{
	private String id;
	private String protocol;
	private String access_point;
	private String control_address;
	private String control_port;
	protected ControlChannelListener control_thread;
	protected DataChannelListener data_thread;
	protected enum ChannelState{SEND_PUTCHUNK,NEUTRAL,BACKUP_WAIT};
	
	public MulticastServer(String ident, String proto, String access_point) {
		
	}
	
	public MulticastServer() {
		// TODO Auto-generated constructor stub
	}

	protected static class ControlChannelListener implements Runnable
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
			LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
			control_pool = new ThreadPoolExecutor(100, 500, 10, TimeUnit.SECONDS, queue);
		}

		public void run()
		{
			while(true)
			{
					
			}
		}
	}

	protected static class DataChannelListener implements Runnable
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
			LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
			data_pool = new ThreadPoolExecutor(100, 500, 10, TimeUnit.SECONDS, queue);
		}

		public void run()
		{
			while(true)
			{
				switch(data_state) {
				case SEND_PUTCHUNK:
					System.out.println("SENDING PUTCHUNK");
					break;
				case NEUTRAL:
					break;
				case BACKUP_WAIT:
					break;
				default:
					break;
				
				}
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
	
	public static void main(String[] args) throws IOException, AlreadyBoundException 
	{
		MulticastServer serv = new MulticastServer();
		serv.control_thread=new ControlChannelListener();
		serv.data_thread=new DataChannelListener();
		serv.startup();
		ServerRemoteObject obj = new ServerRemoteObject(serv);
		RMIBackup stub = (RMIBackup) UnicastRemoteObject.exportObject(obj, 0);
		
		// Bind the remote object's stub in the registry
        Registry registry = LocateRegistry.getRegistry();
        registry.bind("RMIBackup", stub);
	}

}
