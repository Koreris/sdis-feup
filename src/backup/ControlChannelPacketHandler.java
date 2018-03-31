package backup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;

import javax.swing.filechooser.FileSystemView;

public class ControlChannelPacketHandler implements Runnable{
	DatagramPacket data;
	DatagramSocket socket;
	MulticastServer main_server;

	public ControlChannelPacketHandler(DatagramPacket packet, MulticastServer mainserver, MulticastSocket sock) {
		main_server=mainserver;
		socket=sock;
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
			case "STORED":
				handleStored(headerComponents);
				break;
			case "DELETE":
				handleDelete(headerComponents);
				break;
			case "GETCHUNK":
				handleGetchunk(headerComponents);
				break;
			default:
				break;
		}
	}
	
	private void handleGetchunk(String[] headerComponents) {
		if(headerComponents[2].equals(main_server.id))
			return;
	
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		Path chunkpath = Paths.get(home.getAbsolutePath()+"/sdis/files/"+main_server.id+"/"+headerComponents[3]+File.separator+headerComponents[4]);
		
		if(Files.exists(chunkpath)) {
			try {
				Random delay_gen = new Random();
				int delay=delay_gen.nextInt(401);
				Thread.sleep(delay);
				//TODO if no record of someone having sent chunk, send chunk else return
				if(main_server.records_restore.containsKey(headerComponents[3]+headerComponents[4]))
					return;
					
				byte[] header = CreateMessages.createHeader("CHUNK", headerComponents[1], main_server.id, headerComponents[3], Integer.parseInt(headerComponents[4]),0);
				byte[] data = Files.readAllBytes(chunkpath);
				byte[] combined = new byte[header.length + data.length];
	
				for (int i = 0; i < combined.length; ++i)
				{
				    combined[i] = i < header.length ? header[i] : data[i - header.length];
				}
				
				InetAddress recovery_addr;
				recovery_addr = InetAddress.getByName(main_server.recovery_address);
				DatagramPacket packet = new DatagramPacket(combined,0,combined.length,recovery_addr,main_server.recovery_port);
				socket.send(packet);
				main_server.records_restore.put(headerComponents[3]+headerComponents[4], 0);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void handleDelete(String[] headerComponents) {	
		if(headerComponents[2].equals(main_server.id))
			return;
	
		Utils.deleteFile(headerComponents[3],main_server.id,main_server.records_backup,main_server.records_store);	
	}

	private void handleStored(String[] headerComponents) {
		if(headerComponents[2].equals(main_server.id))
			return;
		
		Integer curr_rep_degree;
		// se for initiator peer do backup
		curr_rep_degree=main_server.records_backup.get(headerComponents[3]+":"+headerComponents[4]);
		if(curr_rep_degree!=null) {
			main_server.records_backup.put(headerComponents[3]+":"+headerComponents[4],curr_rep_degree.intValue()+1);
			main_server.records_backup.put(headerComponents[3]+":"+headerComponents[4]+":"+headerComponents[2], -1);
		    //Utils.printRecords(records);
			return;
		}
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		File chunk = new File(home.getAbsolutePath()+"/sdis/files/"+main_server.id+"/"+headerComponents[3]+File.separator+headerComponents[4]);
		// se for um dos que faz store
		if(chunk.exists()) {
			int chunk_size=(int)chunk.length();
		    curr_rep_degree=main_server.records_store.get(headerComponents[3]+":"+headerComponents[4]+":"+chunk_size);
			if(curr_rep_degree!=null) {
				main_server.records_store.put(headerComponents[3]+":"+headerComponents[4]+":"+chunk_size,curr_rep_degree.intValue()+1);
			    //Utils.printRecords(records);
			}
		}
		
		// BACKUP ENHANCEMENT BEGIN;
		if(headerComponents[1].equals("2.0")) {
			
			  curr_rep_degree=main_server.volatile_store_records.get(headerComponents[3]+headerComponents[4]);
				if(curr_rep_degree!=null) {
					main_server.volatile_store_records.put(headerComponents[3]+headerComponents[4], curr_rep_degree.intValue()+1);
				    //Utils.printRecords(records);
				}
				else main_server.volatile_store_records.put(headerComponents[3]+headerComponents[4], 1);
		}
		// BACKUP ENHANCEMENT END;
		
		return;
	}
}
