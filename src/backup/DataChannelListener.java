package backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.filechooser.FileSystemView;


class DataChannelListener implements Runnable
{
	private MulticastSocket socket;
	private MulticastServer main_server;
	private ThreadPoolExecutor data_pool;
	ConcurrentHashMap <String,Integer> records_reclaim;
	ScheduledThreadPoolExecutor cleanup_records;
	final static int MAX_PACKET_SIZE=64096;
	
	public DataChannelListener(MulticastServer multicastServer) throws UnknownHostException, IOException {
		main_server=multicastServer;
		socket = new MulticastSocket(main_server.data_port);	
		socket.joinGroup(InetAddress.getByName(main_server.data_address));
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		data_pool = new ThreadPoolExecutor(5, 20, 10, TimeUnit.SECONDS, queue);
		records_reclaim = new ConcurrentHashMap<String,Integer>();
		cleanup_records = new ScheduledThreadPoolExecutor(1);
	}

	public void run()
	{
		while(true)
		{
			byte[] buf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				data_pool.execute(new DataChannelPacketHandler(packet,main_server,socket,records_reclaim));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initiateBackup(String filename,int rep_deg)  
	{
		data_pool.execute(new BackupService(filename,rep_deg));
	}
	
	
	public void initiateReclaim(Integer space) 
	{
		data_pool.execute(new ReclaimService(space));
	}
	
	public class ReclaimService implements Runnable{
		
		int space_to_occupy;
		public ReclaimService(Integer s) 
		{
			space_to_occupy=s*1000;
		}

		@Override
		public void run() 
		{
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File peer_directory = new File(home.getAbsolutePath()+"/sdis/files/"+main_server.id);
			File restore_directory = new File(home.getAbsolutePath()+"/sdis/files/"+main_server.id+"/restore");
			
			if(peer_directory.exists()) 
			{
				long storage_occupied=Utils.checkDirectorySize(peer_directory);
			    
				if(restore_directory.exists()) 
				{
					storage_occupied-=Utils.checkDirectorySize(restore_directory);
				}
				
				if(storage_occupied<=space_to_occupy)
					return;
				else
				{
					long accumulator = 0;
					//key=fileID:chunkNo:size:desiredRepDegree value=perceivedRepDegree
					for (String key : main_server.records_store.keySet()) 
					{
						String[] keyComponents = key.split(":");
						if(keyComponents.length==2) {
							main_server.records_store.remove(key);
							continue;
						}
						int file_size = Integer.parseInt(keyComponents[2]);
						accumulator=accumulator+file_size;
						File to_delete= new File(peer_directory+"/"+keyComponents[0]+File.separator+keyComponents[1]);
						to_delete.delete();
						main_server.records_store.remove(key);
						records_reclaim.put(keyComponents[0],-1);
						cleanup_records.schedule(()->records_reclaim.remove(keyComponents[0]), 5, TimeUnit.SECONDS);
						try {
							byte[] removed = CreateMessages.createHeader("REMOVED","1.0", main_server.id, keyComponents[0], Integer.parseInt(keyComponents[1]), 0);
							DatagramPacket packet = new DatagramPacket(removed, 0, removed.length,InetAddress.getByName(main_server.control_address),main_server.control_port);
							socket.send(packet);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(storage_occupied-accumulator<=space_to_occupy)
							break;
					}
				}
			} 
			else return;
		}
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
				if(main_server.records_backup.containsKey(file_to_backup+":"+fileID)) {
					//System.out.println("Already backed up this file!");
					//return;
				}
				main_server.records_backup.put(file_to_backup+":"+fileID, rep_degree);
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
			byte[] header=CreateMessages.createHeader("PUTCHUNK", main_server.protocol_version, main_server.id, fileID, sent_chunks, rep_degree);
			
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
				main_server.records_backup.put(fileID+":"+sent_chunks, 0);
				while(nr_tries<5) {
					
					System.out.println(Thread.currentThread().getName()+" - Sending chunk :"+sent_chunks+" Try: "+(nr_tries+1));
					DatagramPacket packet = new DatagramPacket(chunk, 0, chunk.length,InetAddress.getByName(main_server.data_address),main_server.data_port);
					socket.send(packet);
					Thread.sleep(1000);
					Integer perceived_replication_degree=main_server.records_backup.get(fileID+":"+sent_chunks);
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
