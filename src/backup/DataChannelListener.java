package backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//TODO -> contar o numero de stores para quem fez o backup e para quem faz store dos chunks
//TODO -> actualizar no control channel -> formato de records para quem faz backup "fileId=chunkId | perceivedRepDegree"
//TODO -> actualizar no control channel -> formato de records para quem faz store "fileID=chunkID chunkSize | perceivedRepDegree"
//TODO -> parametrizar os servidores com os ips e ports dos canais
class DataChannelListener implements Runnable
{
	private MulticastSocket socket;
	private InetAddress data_adr;
	private int port=7777;
	private int rep_degree;
	private String file_to_backup;
	private int file_size;
	private int final_chunk_size;
	private int total_chunks;
	private int sent_chunks;
	private ThreadPoolExecutor data_pool;
	private String server_id; 
	private ConcurrentHashMap<String,Integer> records;
	final static int MAX_PACKET_SIZE=64096;
	
	public DataChannelListener(String serverID,ConcurrentHashMap<String,Integer> rec) throws IOException 
	{
		socket = new MulticastSocket(port);	
		data_adr = InetAddress.getByName("239.0.0.1");
		socket.joinGroup(data_adr);
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		data_pool = new ThreadPoolExecutor(5, 20, 10, TimeUnit.SECONDS, queue);
		server_id=serverID;
		records=rec;
	}

	public void run()
	{
		while(true)
		{
			byte[] buf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				data_pool.execute(new DataChannelPacketHandler(packet,server_id,records,socket));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initiateBackup(String filename,int rep_deg) throws NoSuchAlgorithmException, IOException  
	{
		rep_degree=rep_deg;
		file_to_backup=filename;
		records.put(file_to_backup+" fileID", rep_degree);
		analyzeFile();
		createPutchunk();
	}
	
	public void createPutchunk() throws NoSuchAlgorithmException, IOException {
		
		if(sent_chunks==total_chunks) {
			System.out.println("Backup over: Sent "+sent_chunks+"/"+total_chunks);
			return;
		}
		
		byte[] data = null;
		//send putchunk
		byte[] header=CreateMessages.createHeader("PUTCHUNK", "1.0", server_id, file_to_backup, sent_chunks, rep_degree);
		try {
			data=getFileChunk();
		}
		catch(Exception e) {
			e.printStackTrace();
			return;
		}
		byte[] combined = new byte[header.length + data.length];

		for (int i = 0; i < combined.length; ++i)
		{
		    combined[i] = i < header.length ? header[i] : data[i - header.length];
		}
		data_pool.execute(new SendChunk(combined));
		
	}
	
	private class SendChunk implements Runnable{
		byte[] chunk;
		int nr_tries=0;
		public SendChunk(byte[] chunkdata) 
		{
			chunk=chunkdata;
		}
		@Override
		public void run() {
			try {
				while(nr_tries<5) {
					System.out.println("Sending Packet length: "+chunk.length);
					DatagramPacket packet = new DatagramPacket(chunk, 0, chunk.length,data_adr,7777);
					socket.send(packet);
					records.put("fileID="+sent_chunks, 0);
					
					Thread.sleep(1000);
					int perceived_replication_degree=records.get("fileID="+sent_chunks);
					if(perceived_replication_degree>=rep_degree) {
						sent_chunks++;
						createPutchunk();
						return;
					}
					nr_tries++;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void analyzeFile() {
          // send file
          File my_file = new File (file_to_backup);
          file_size=(int)my_file.length();
          
          total_chunks=file_size/64000;
          
          final_chunk_size=file_size-(total_chunks-1)*64000;
          if(total_chunks==0) {
        	  total_chunks++;
        	  final_chunk_size=0;
          }
          if((file_size % 64000)==0) {
        	  total_chunks+=1;
        	  final_chunk_size=0;
          }	 
	}
	
	public byte[] getFileChunk() throws IOException {
        FileInputStream fis = null;
		BufferedInputStream bis = null;
		byte[] file_data;
		File my_file = new File (file_to_backup);
		fis = new FileInputStream(my_file);
	    bis = new BufferedInputStream(fis);
		if(sent_chunks<total_chunks) {
			if(file_size<64000) {
				file_data= new byte [file_size];
				 bis.read(file_data,sent_chunks*64000,file_size); 
			}
			else{
				file_data= new byte [64000];
				bis.read(file_data,sent_chunks*64000,64000); 
			}
	        bis.close();
	        fis.close();
		}
		else {
			file_data= new byte [final_chunk_size];
			bis.read(file_data,sent_chunks*64000,final_chunk_size); 
	        bis.close();
	        fis.close();
		}
        return file_data;
	}
	
	
	
}
