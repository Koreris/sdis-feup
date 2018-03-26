package backup;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.filechooser.FileSystemView;

public class ControlChannelPacketHandler implements Runnable{
	DatagramPacket data;
	DatagramSocket socket;
	String server_id;
	ConcurrentHashMap<String,Integer> records;
	
	public ControlChannelPacketHandler(DatagramPacket packet,String server,ConcurrentHashMap<String,Integer> rec,DatagramSocket sock) 
	{
		data=packet;
		server_id=server;
		records=rec;
		socket=sock;
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
			default:
				break;
		}
	}

	private void handleStored(String[] headerComponents) {
		
		if(headerComponents[2].equals(server_id))
			return;
		
		Integer curr_rep_degree;
		// se for initiator peer do backup
		curr_rep_degree=records.get(headerComponents[3]+"="+headerComponents[4]);
		if(curr_rep_degree!=null) {
			records.put(headerComponents[3]+"="+headerComponents[4],curr_rep_degree.intValue()+1);
			records.put(headerComponents[3]+"="+headerComponents[4]+"="+headerComponents[2], -1);
		   // Utils.printRecords(records);
			return;
		}
		File home = FileSystemView.getFileSystemView().getHomeDirectory();
		File chunk = new File(home.getAbsolutePath()+"/sdis/files/"+server_id+"/"+headerComponents[3]+File.separator+headerComponents[4]);
		// se for um dos que faz store
		if(chunk.exists()) {
			int chunk_size=(int)chunk.length();
		    curr_rep_degree=records.get(headerComponents[3]+"="+headerComponents[4]+" "+chunk_size);
			if(curr_rep_degree!=null) {
			    records.put(headerComponents[3]+"="+headerComponents[4]+" "+chunk_size,curr_rep_degree.intValue()+1);
			    //Utils.printRecords(records);
			}
		}
		return;
	
		
	}
}
