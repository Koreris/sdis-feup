package backup;

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

public class ServerRemoteObject implements RMIBackup {
	MulticastServer peer;
	
	public ServerRemoteObject(MulticastServer serv) {
		peer=serv;
	}
	
	@Override
	public String backup(String filename, int replication_degree) throws RemoteException 
	{
		try {
			peer.data_thread.initiateBackup(filename,replication_degree);
		} catch (NoSuchAlgorithmException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Putchunk sent";
	}
	
	public String restore(String filename) 
	{
		
	}

}
