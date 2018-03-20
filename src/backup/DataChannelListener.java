package backup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class DataChannelListener implements Runnable
{
	private MulticastSocket socket;
	private InetAddress data_adr;
	private int port;
	private int rep_degree;
	private String file_to_backup;
	private int file_size;
	private int final_chunk_size;
	private int total_chunks;
	private int sent_chunks;
	private ThreadPoolExecutor data_pool;
	private String server_id; 
	final static int MAX_PACKET_SIZE=64096;
	
	public DataChannelListener(String serverID) throws IOException 
	{
		socket = new MulticastSocket(7777);	
		data_adr = InetAddress.getByName("239.0.0.1");
		socket.joinGroup(data_adr);
		LinkedBlockingQueue<Runnable> queue= new LinkedBlockingQueue<Runnable>();
		data_pool = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, queue);
		server_id=serverID;
	}

	public void run()
	{
		while(true)
		{
			byte[] buf = new byte[MAX_PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet);
				data_pool.execute(new DataChannelPacketHandler(packet));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void initiateBackup(String filename,int rep_deg)  
	{
		rep_degree=rep_deg;
		file_to_backup=filename;
		analyzeFile();
		byte[] data = null;
		//send putchunk
		byte[] header=CreateMessages.createHeader("PUTCHUNK", "1.0", server_id, "fileID", sent_chunks, rep_deg);
		try {
			data=makeChunk();
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
		sendMessage(combined);
	}
	
	public void sendMessage(byte[] msg)
	{
		System.out.println("Packet length: "+msg.length);
		DatagramPacket packet = new DatagramPacket(msg, 0, msg.length,data_adr,7777);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
	public byte[] makeChunk() throws IOException {
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
	
	public void updateBackupState(boolean next) {
		if(next)
			sent_chunks++;
	}
	
	private class DataChannelPacketHandler implements Runnable 
	{
		DatagramPacket data;
		public DataChannelPacketHandler(DatagramPacket packet) 
		{
			data=packet;
		}

		@Override
		public void run() 
		{
			String packetString = new String(data.getData(),0,data.getLength());
			String[] lines = packetString.split(System.getProperty("line.separator"));
			String header = lines[0];
			String[] headerComponents = header.split(" ");
			
			
			switch(headerComponents[0]) {
				case "PUTCHUNK":
					handlePutchunk(headerComponents,lines[2]);
					break;
				default:
					break;
			}
		
			System.out.print(lines[2].length());
		}

		private void handlePutchunk(String[] headerComponents, String filedata) {
			System.out.println(headerComponents[1]); //version
			System.out.println(headerComponents[2]); //sender
			System.out.println(headerComponents[3]); // filepath-> supposed to be fileID
			if(headerComponents[2].equals(server_id))
				return;
			System.out.println("Header component: "+headerComponents[2]+" server id: "+server_id);
			// guardar ficheiro no diretorio headerComponents[3]/headerComponents[4]
			
			FileOutputStream out;
			try {
				File newfile = new File("/C:/sdis/files/peer2/"+headerComponents[3]+File.separator+headerComponents[4]);
				newfile.getParentFile().mkdirs(); // correct!
				if (!newfile.exists()) {
				    newfile.createNewFile();
				} 
				out = new FileOutputStream(newfile);
				out.write(filedata.getBytes());
				out.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
}
