package backup;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIBackup extends Remote 
{
	String backup(String filename,int replication_degree) throws RemoteException;
	String delete(String filename) throws RemoteException;
	String restore(String filename) throws RemoteException;
	String reclaim(Integer space) throws RemoteException;
}