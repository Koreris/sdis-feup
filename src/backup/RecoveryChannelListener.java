package backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.FileSystemView;

//TODO -> parametrizar os servidores com os ips e ports dos canais
class RecoveryChannelListener implements Runnable
{
	private MulticastSocket socket;
	private ThreadPoolExecutor recovery_pool;
	private String server_id; 
	private String control_adr;
	private int control_port;
	private InetAddress recovery_adr;
	private int recovery_port;
	ConcurrentHashMap<String,Integer> records_backup;
	ConcurrentHashMap<String,Integer> records_store;
	final static int MAX_PACKET_SIZE=64096;
	
	public RecoveryChannelListener(String serverID,ConcurrentHashMap<String,Integer> recbac,ConcurrentHashMap<String,Integer> recsto, String adr, int port, String controladr, Integer controlport) throws IOException 
	{
		recovery_port=port;
		socket = new MulticastSocket(recovery_port);	
		control_port=controlport;
		control_adr=controladr;
		recovery_adr = InetAddress.getByName(adr);
		socket.joinGroup(recovery_adr);
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		recovery_pool = new ThreadPoolExecutor(5, 20, 10, TimeUnit.SECONDS, queue);
		server_id=serverID;
		records_backup=recbac;
		records_store=recsto;
	}

	public void run()
	{
		while(true)
		{
			byte[] buf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				recovery_pool.execute(new RecoveryChannelPacketHandler(packet,server_id,records_backup,records_store,socket));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	
	
	public void initiateRestore(String filename) throws NoSuchAlgorithmException, IOException  
	{
		
		
	}
	
	public class RestoreService implements Runnable {
		
		private String file_to_restore;
		private int file_size;
		private int total_chunks;
		private int received_chunks;
		private String fileID;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		
		public RestoreService(String filename) {
			file_to_restore=filename;
		}
	
		@Override
		public void run() {
			try {
				fileID = Utils.createFileID(file_to_restore);
				if(records_backup.containsKey(file_to_restore+":"+fileID)) {
					return;
				}
				analyzeFile();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		

	
		public void createGetchunk() throws NoSuchAlgorithmException, IOException 
		{
			
			if(received_chunks==total_chunks) 
			{
				System.out.println("Restore over: Sent "+received_chunks+"/"+total_chunks);
				fis.close();
				bis.close();
				fis = null;
				bis = null;
				return;
			}
			
			//send putchunk
			byte[] header=CreateMessages.createHeader("GETCHUNK", "1.0", server_id, fileID, received_chunks,0);
			
			recovery_pool.execute(new SendGetchunk(header));
			
		}
		public class SendGetchunk implements Runnable{
			public SendGetchunk(byte[] getchunk) 
			{
			}
			@Override
			public void run() {
			}			
			
		}
		
		public void analyzeFile() 
		{
	          // send file
			  System.out.println("Analyzing file!");
			  received_chunks=0;
			  File home = FileSystemView.getFileSystemView().getHomeDirectory();
			  File my_file = new File (home.getAbsolutePath()+file_to_restore);
	          file_size=(int)my_file.length();
	          try 
	          {
					fis = new FileInputStream(my_file);
					bis = new BufferedInputStream(fis);
			  } catch (FileNotFoundException e) 
	          {
					// TODO Auto-generated catch block
					e.printStackTrace();
			  }
	          
	          total_chunks=file_size/64000;
	          total_chunks++;
	          if(file_size<64000) 
	          {
	  			total_chunks=1;
	  			return;
	  		  }
	          if((file_size % 64000)==0) 
	          {
	        	  total_chunks+=1;
	        	  return;
	          }
	    
		}
		
	}

}
