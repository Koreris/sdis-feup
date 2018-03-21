package backup;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentHashMap;

public class DataChannelPacketHandler implements Runnable 
{
	DatagramPacket data;
	String server_id;
	ConcurrentHashMap<String,Integer> records;
	
	public DataChannelPacketHandler(DatagramPacket packet,String server,ConcurrentHashMap<String,Integer> rec) 
	{
		data=packet;
		server_id=server;
		records=rec;
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
	}

	private void handlePutchunk(String[] headerComponents, String filedata) {
	
		if(headerComponents[2].equals(server_id))
			return;
		System.out.println("Header component: "+headerComponents[2]+" server id: "+server_id);
		//System.out.println("headerComponents[1]); //version
		System.out.println(headerComponents[3]); //fileID
		System.out.println("Received chunkno:"+headerComponents[4]);
		// guardar ficheiro no diretorio headerComponents[3]/headerComponents[4]
		
		//send stored here
		records.put(headerComponents[3]+"="+headerComponents[4]+" "+filedata.length(), 1);
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
