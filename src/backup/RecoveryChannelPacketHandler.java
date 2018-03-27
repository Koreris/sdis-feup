package backup;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

public class RecoveryChannelPacketHandler implements Runnable 
{
	DatagramPacket data;
	DatagramSocket socket;
	String server_id;
	ConcurrentHashMap<String,Integer> records_backup;
	ConcurrentHashMap<String,Integer> records_store;
	
	public RecoveryChannelPacketHandler(DatagramPacket packet,String server,ConcurrentHashMap<String,Integer> recbac,ConcurrentHashMap<String,Integer> recsto,DatagramSocket sock) 
	{
		data=packet;
		server_id=server;
		records_backup=recbac;
		records_store=recsto;
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
			case "PUTCHUNK":
				try {
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
		}
	}

	
	
}
