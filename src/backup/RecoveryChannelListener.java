package backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.FileSystemView;

class RecoveryChannelListener implements Runnable
{
	private MulticastSocket socket;
	private ThreadPoolExecutor recovery_pool;
	private ScheduledThreadPoolExecutor scheduling_pool;
	private String server_id; 
	private String control_adr;
	private int control_port;
	private InetAddress recovery_adr;
	private int recovery_port;
	ConcurrentHashMap<String,Integer> records_backup;
	ConcurrentHashMap<String,Integer> records_store;
	ConcurrentHashMap<String,Integer> records_restore;
	final static int MAX_PACKET_SIZE=64096;

	public RecoveryChannelListener(String serverID,ConcurrentHashMap<String,Integer> recbac,ConcurrentHashMap<String,Integer> recsto,ConcurrentHashMap<String,Integer> restore_recs, String adr, int port, String controladr, Integer controlport) throws IOException 
	{
		recovery_port=port;
		socket = new MulticastSocket(recovery_port);	
		control_port=controlport;
		control_adr=controladr;
		recovery_adr = InetAddress.getByName(adr);
		socket.joinGroup(recovery_adr);
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		recovery_pool = new ThreadPoolExecutor(5, 20, 10, TimeUnit.SECONDS, queue);
		scheduling_pool = new ScheduledThreadPoolExecutor(5);
		server_id=serverID;
		records_backup=recbac;
		records_store=recsto;
		records_restore = restore_recs;
	}

	public void run()
	{
		while(true)
		{	
			try {
				byte[] buf = new byte[MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				recovery_pool.execute(new RecoveryChannelPacketHandler(packet,server_id,records_restore,scheduling_pool));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} 
		}
	}
	
	public void initiateRestore(String filename) throws NoSuchAlgorithmException, IOException  
	{
		recovery_pool.execute(new RestoreService(filename));

	}
	
	
	public class RestoreService implements Runnable {

		private String file_to_restore;
		private int file_size;
		private int total_chunks;
		private String fileID;
		Boolean restore_done=false;
		ConcurrentHashMap<Integer,Integer> chunks_check;
		FileInputStream fis = null;
		BufferedInputStream bis = null;


		public RestoreService(String filename) throws NoSuchAlgorithmException, IOException {
			chunks_check = new ConcurrentHashMap<Integer,Integer>();
			file_to_restore=filename;
			fileID = Utils.createFileID(file_to_restore);
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File restore_dir = new File(home.getAbsolutePath()+"/sdis/files/"+server_id+"/restored/"+fileID);
			if (restore_dir.exists())
			{
				String[]entries = restore_dir.list();
				for(String s: entries)
				{
				    File currentFile = new File(restore_dir.getPath(),s);
				    currentFile.delete();
				}
			}
			analyzeFile();
			for(int i=0;i<total_chunks;i++) 
			{
				scheduling_pool.schedule(new GetchunkSender(i), 50*i, TimeUnit.MILLISECONDS);

			}
			records_restore.put(fileID, 1);
		}

		@Override
		public void run() {
			while(!restore_done) {
				try {
					checkReceivedAllChunks();
					if(restore_done)
						return;
					Thread.sleep(1000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public void checkReceivedAllChunks() {
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File restore_dir = new File(home.getAbsolutePath()+"/sdis/files/"+server_id+"/restored/"+fileID);
			if (restore_dir.exists())
			{
				String[]entries = restore_dir.list();
				if(entries.length==total_chunks) {
					mergeChunksToFile();
					restore_done=true;
					System.out.println("Restore is done!");
				}
			}
			
		}
		
		public void mergeChunksToFile(){
			records_restore.remove(fileID);
			FileOutputStream out;
			String[] file = file_to_restore.split("\\\\");
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
		
			File restored_file = new File(home.getAbsolutePath()+"/sdis/files/"+server_id+"/restored/"+fileID+File.separator+file[file.length-1]);
			try {
				if (!restored_file.exists()) {
				    restored_file.createNewFile();
				}
				out = new FileOutputStream(restored_file);
				File restore_dir = new File(home.getAbsolutePath()+"/sdis/files/"+server_id+"/restored/"+fileID);		
				for(int i=0;i<total_chunks;i++)
				{
				    File currentFile = new File(restore_dir.getPath(),i+"");
				    System.out.println("MERGING "+i);
					out.write(Files.readAllBytes(currentFile.toPath()));
				    currentFile.delete();
				}
				out.close();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			
		}
		
		public void sendGetchunk(byte[] getchunk) throws IOException{
			InetAddress adr = InetAddress.getByName(control_adr);
			DatagramPacket packet = new DatagramPacket(getchunk, 0, getchunk.length,adr,control_port);
			socket.send(packet);
		}

		public void analyzeFile() 
		{
			// send file
			//System.out.println("Analyzing file!");
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File my_file = new File (home.getAbsolutePath()+file_to_restore);
			file_size=(int)my_file.length();

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
			//System.out.println("Total Chunks to recover "+total_chunks);
		}

		public class GetchunkSender implements Runnable{
			int chunkno;
			public GetchunkSender(int i) {
				chunkno=i;
			}
			@Override
			public void run() {
				byte[] getchunk;
				try 
				{
					getchunk = CreateMessages.createHeader("GETCHUNK", "1.0", server_id, fileID, chunkno,0);
					sendGetchunk(getchunk);	
					//System.out.println("Sent getchunk "+chunkno);
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}

		}

	}

}
