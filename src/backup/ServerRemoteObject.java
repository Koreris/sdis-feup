package backup;

import java.rmi.RemoteException;

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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Backup initiated";
	}

	@Override
	public String delete(String filename) throws RemoteException {
		try {
			peer.control_thread.initiateDelete(filename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Delete initiated";
	}

	@Override
	public String restore(String filename) throws RemoteException {
		try {
			peer.recovery_thread.initiateRestore(filename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Restore initiated";
	}

	@Override
	public String reclaim(Integer space) throws RemoteException {
		try
		{
			peer.initiateReclaim(space);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return "Reclaim initiated";
	}
	


}
