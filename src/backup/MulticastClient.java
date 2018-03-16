package backup;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.*;
import java.util.*;

public class MulticastClient 
{	
	private static void printCommand(final String commandMessage)
	{
		System.out.print(String.format("(%s) Command sent: %s\n",
			(new Date()).toString(), commandMessage));
	}
	
	public static void main(String[] args) throws IOException, NotBoundException 
	{
		 if(args.length!=3 && args.length!=4) {
			 System.out.println("Usage: <RMI object> <sub_protocol> <opnd_1> [opnd_2(replication degree on backup)]");
		 }
			 
		 Registry registry = LocateRegistry.getRegistry("127.0.0.1",1098);
         RMIBackup stub = (RMIBackup) registry.lookup(args[0]);
         String response = stub.backup(args[2], Integer.parseInt(args[3]));
         printCommand(response);
	}
}