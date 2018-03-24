package backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
				try {
					handleStored(headerComponents);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
		}
	}

	private void handleStored(String[] headerComponents) {
	
		if(headerComponents[2].equals(server_id))
			return;
		//System.out.println("Header component: "+headerComponents[2]+" server id: "+server_id);
		//System.out.println("headerComponents[1]); //version
		//System.out.println(headerComponents[3]); //fileID
		
		// guardar ficheiro no diretorio headerComponents[3]/headerComponents[4]
		int curr_rep_degree;
		//records.get(headerComponents[3]+"="+headerComponents[4]+" "+filedata.length(), curr_rep_degree);
		
	}
}
