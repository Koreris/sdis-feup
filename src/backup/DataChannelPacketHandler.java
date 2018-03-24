package backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.filechooser.FileSystemView;

public class DataChannelPacketHandler implements Runnable 
{
	DatagramPacket data;
	DatagramSocket socket;
	String server_id;
	ConcurrentHashMap<String,Integer> records;
	
	public DataChannelPacketHandler(DatagramPacket packet,String server,ConcurrentHashMap<String,Integer> rec,DatagramSocket sock) 
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
			case "PUTCHUNK":
				try {
					handlePutchunk(headerComponents,lines[2]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
		}
	}

	private void handlePutchunk(String[] headerComponents, String filedata) throws IOException, InterruptedException {
	
		if(headerComponents[2].equals(server_id))
			return;
		//System.out.println("Header component: "+headerComponents[2]+" server id: "+server_id);
		//System.out.println("headerComponents[1]); //version
		//System.out.println(headerComponents[3]); //fileID
		System.out.println("Received chunkno:"+headerComponents[4]);
		// guardar ficheiro no diretorio headerComponents[3]/headerComponents[4]
		byte[] stored = CreateMessages.createHeader("STORED", headerComponents[1], server_id, headerComponents[3], Integer.parseInt(headerComponents[4]),0);
		InetAddress control_addr = InetAddress.getByName("239.0.0.0");
		DatagramPacket packet = new DatagramPacket(stored,0,stored.length,control_addr,8888);
		Random delay_gen = new Random();
		int delay=delay_gen.nextInt(401);
		Thread.sleep(delay);
		socket.send(packet);
		records.put(headerComponents[3]+"="+headerComponents[4]+" "+filedata.length(), 1);
		
		FileOutputStream out;
		try {
			File home = FileSystemView.getFileSystemView().getHomeDirectory();
			File newfile = new File("/"+home.getAbsolutePath()+"/sdis/files/"+server_id+"/"+headerComponents[3]+File.separator+headerComponents[4]);
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
