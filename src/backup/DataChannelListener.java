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
class DataChannelListener implements Runnable
{
	private MulticastSocket socket;

	private String control_adr;
	private int control_port;
	private InetAddress data_adr;
	private int data_port;
	private ThreadPoolExecutor data_pool;
	private String server_id; 
	private Integer storage_capacity;
	ConcurrentHashMap<String,Integer> records_backup;
	ConcurrentHashMap<String,Integer> records_store;

	final static int MAX_PACKET_SIZE=64096;
	
	public DataChannelListener(String serverID,ConcurrentHashMap<String,Integer> recbac,ConcurrentHashMap<String,Integer> recsto, String adr, int dataport, Integer storage, String controladr, int controlport) throws IOException 
	{
		data_port=dataport;
		socket = new MulticastSocket(data_port);	
		data_adr = InetAddress.getByName(adr);
		socket.joinGroup(data_adr);
		control_port=controlport;
		control_adr=controladr;
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		data_pool = new ThreadPoolExecutor(5, 20, 10, TimeUnit.SECONDS, queue);
		server_id=serverID;
		storage_capacity=storage;
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
				data_pool.execute(new DataChannelPacketHandler(packet,server_id,records_backup,records_store,socket,control_adr,control_port));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	
	
	public void initiateBackup(String filename,int rep_deg)  
	{
		data_pool.execute(new BackupService(filename,rep_deg));
	}
	
	public class BackupService implements Runnable {
		private int file_size;
		private int final_chunk_size;
		private int total_chunks;
		private int sent_chunks;
		private int rep_degree;
		private String file_to_backup;
		private String fileID;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		
		public BackupService(String filename,int deg) {
			rep_degree=deg;
			file_to_backup=filename;
		}
		
		@Override
		public void run() {
			try {
				fileID = Utils.createFileID(file_to_backup);
				if(records_backup.containsKey(file_to_backup+":"+fileID)) {
					System.out.println("Already backed up this file!");
					return;
				}
				records_backup.put(file_to_backup+":"+fileID, rep_degree);
				analyzeFile();
				createPutchunk();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
		}
		
		public void createPutchunk() throws NoSuchAlgorithmException, IOException 
		{
			
			if(sent_chunks==total_chunks) 
			{
				System.out.println("Backup over: Sent "+sent_chunks+"/"+total_chunks);
				fis.close();
				bis.close();
				fis = null;
				bis = null;
				return;
			}
			
			byte[] data = null;
			//send putchunk
			byte[] header=CreateMessages.createHeader("PUTCHUNK", "1.0", server_id, fileID, sent_chunks, rep_degree);
			
			try 
			{
				data=getFileChunk();
			}
			catch(Exception e) 
			{
				e.printStackTrace();
				return;
			}
			
			byte[] combined = new byte[header.length + data.length];

			for (int i = 0; i < combined.length; ++i)
			{
			    combined[i] = i < header.length ? header[i] : data[i - header.length];
			}
			
			sendChunk(combined);
		}
		
		public void sendChunk(byte[] chunk){
			int nr_tries=0;
			try {
				records_backup.put(fileID+":"+sent_chunks, 0);
				while(nr_tries<5) {
					DatagramPacket packet = new DatagramPacket(chunk, 0, chunk.length,data_adr,data_port);
					socket.send(packet);
					Thread.sleep(1000);
					Integer perceived_replication_degree=records_backup.get(fileID+":"+sent_chunks);
					if(perceived_replication_degree>=rep_degree) 
					{
						sent_chunks++;
						createPutchunk();
						return;
					}
					nr_tries++;
				}
				sent_chunks++;
				createPutchunk();
				return;
				
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		
		public void analyzeFile() 
		{
	          // send file
			  System.out.println("Analyzing file!");
			  sent_chunks=0;
			  File home = FileSystemView.getFileSystemView().getHomeDirectory();
			  File my_file = new File (home.getAbsolutePath()+file_to_backup);
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
	  			final_chunk_size=file_size;
	  			return;
	  		  }
	          if((file_size % 64000)==0) 
	          {
	        	  total_chunks+=1;
	        	  final_chunk_size=0;
	        	  return;
	          }
	    
	          final_chunk_size=file_size-(total_chunks-1)*64000;
		}
		
		public byte[] getFileChunk() throws IOException 
		{
	        
			byte[] file_data;
			
			if(sent_chunks<total_chunks-1) 
			{
				if(file_size<64000) 
				{
					file_data= new byte [file_size];
					bis.read(file_data,0,file_size); 
				}
				else
				{
					file_data= new byte [64000];
					bis.read(file_data,0,64000); 
				}
			}
			else 
			{
				file_data= new byte [final_chunk_size];
				bis.read(file_data,0,final_chunk_size); 
			}
			
	        return file_data;
		}

	
	}
	
}
