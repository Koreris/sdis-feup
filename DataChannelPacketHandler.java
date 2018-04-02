package backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


import javax.swing.filechooser.FileSystemView;

public class DataChannelPacketHandler implements Runnable 
{
	DatagramPacket data;
	DatagramSocket socket;
	MulticastServer main_server;
	ConcurrentHashMap <String,Integer> records_reclaim;


	
	public DataChannelPacketHandler(DatagramPacket packet, MulticastServer mainserver, MulticastSocket sock, ConcurrentHashMap<String, Integer> records_reclaim2) {
		main_server=mainserver;
		data=packet;
		socket=sock;
		records_reclaim = records_reclaim2;
	}

	@Override
	public void run() 
	{
		
		String packetString = new String(data.getData(),0,data.getLength());
	
		String[] lines = packetString.split(System.getProperty("line.separator"));
		String header = lines[0];
	
		String[] headerComponents = header.split(" ");
		byte[] actual_data = new byte[data.getLength()];
		System.arraycopy(data.getData(), data.getOffset(), actual_data, 0, data.getLength());
		int filler_length = lines[0].length()+lines[1].length()+4;
	
		
		switch(headerComponents[0]) {
			case "PUTCHUNK":
				try {
					handlePutchunk(headerComponents,Arrays.copyOfRange(actual_data, filler_length, actual_data.length));
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
		}
	}

	private void handlePutchunk(String[] headerComponents, byte[] filedata) throws IOException, InterruptedException {
	
		if(headerComponents[2].equals(main_server.id))
			return;
		if(records_reclaim.containsKey(headerComponents[3]))
			return;
		
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		File peer_directory = new File(home.getAbsolutePath()+"/sdis/files/"+main_server.id);
		File restore_directory = new File(home.getAbsolutePath()+"/sdis/files/"+main_server.id+"/restore");
		
		if(peer_directory.exists()) {
			long storage_occupied=Utils.checkDirectorySize(peer_directory);
		    
			if(restore_directory.exists()) {
				storage_occupied-=Utils.checkDirectorySize(restore_directory);
			}
			if(storage_occupied+filedata.length>main_server.storage_capacity)
				return;
		}
		File chunk = new File(home.getAbsolutePath()+"/sdis/files/"+main_server.id+"/"+headerComponents[3]+File.separator+headerComponents[4]);
		
		if(chunk.exists()) {
			System.out.println("I already received this chunk before!");
			return;
		}
		
		
		System.out.println("Received chunkno:"+headerComponents[4]);

	
		Random delay_gen = new Random();
		int delay=delay_gen.nextInt(401);
		Thread.sleep(delay);
		
		// BACKUP ENHANCEMENT BEGIN;
		if(headerComponents[1].equals("2.0")) {
			if(main_server.volatile_store_records.get(headerComponents[3]+headerComponents[4]) != null && main_server.volatile_store_records.get(headerComponents[3]+headerComponents[4])>=Integer.parseInt(headerComponents[5])) {
				System.out.println("Backup Enhancement: NOT STORING BECAUSE REQUIRED REP_DEGREE IS: "+headerComponents[5] + " AND PERCEIVED IS " + main_server.volatile_store_records.get(headerComponents[3]+headerComponents[4]));
				return;
			}
			main_server.volatile_store_records.put(headerComponents[3]+headerComponents[4],1);
		}
		// BACKUP ENHANCEMENT END;
		
		System.out.println("Storing chunkno:"+headerComponents[4]);
		byte[] stored = CreateMessages.createHeader("STORED", headerComponents[1], main_server.id, headerComponents[3], Integer.parseInt(headerComponents[4]),0);
		InetAddress control_addr = InetAddress.getByName(main_server.control_address);
		DatagramPacket packet = new DatagramPacket(stored,0,stored.length,control_addr,main_server.control_port);
		socket.send(packet);
		main_server.records_store.put(headerComponents[3]+":"+headerComponents[4]+":"+filedata.length, 1);
		main_server.records_store.put(headerComponents[3]+":"+headerComponents[4],Integer.parseInt(headerComponents[5]));
		
		FileOutputStream out;
		try {
			File newfile = new File("/"+home.getAbsolutePath()+"/sdis/files/"+main_server.id+"/"+headerComponents[3]+File.separator+headerComponents[4]);
			newfile.getParentFile().mkdirs(); // correct!
			if (!newfile.exists()) {
			    newfile.createNewFile();
			} 
			out = new FileOutputStream(newfile);
			out.write(filedata);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
