package backup;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIBackup extends Remote {
	String backup(String filename,int replication_degree) throws RemoteException;
}