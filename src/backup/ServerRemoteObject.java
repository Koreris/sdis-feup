package backup;

import java.rmi.RemoteException;

public class ServerRemoteObject implements RMIBackup {
	MulticastServer peer;
	
	public ServerRemoteObject(MulticastServer serv) {
		peer=serv;
	}
	
	@Override
	public String backup(String filename, int replication_degree) throws RemoteException {
		peer.data_thread.initiate_backup(filename,replication_degree);
		return "Putchunk sent";
	}

}
