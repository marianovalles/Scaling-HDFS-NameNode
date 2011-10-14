package org.apache.hadoop.hdfs.server.namenode;

import java.io.IOException;

import org.apache.hadoop.io.DataOutputBuffer;

import se.sics.clusterj.InodeTable;

import com.mysql.clusterj.Session;
import com.mysql.clusterj.Transaction;

public class InodeTableHelper {
	protected Session s;
	private boolean entry_exists;
	
	public InodeTableHelper(){
		s = DBConnector.sessionFactory.getSession();
	}
	
	/*AddChild should take care of the different InodeOperations
	 * InodeDirectory, InodeDirectoryWithQuota, etc.
	 * TODO: InodeSymLink
	 */
	<T extends INode> void AddChild(T node){
		Transaction tx = s.currentTransaction();
	    tx.begin();
	    InodeTable inode = s.find(InodeTable.class, node.getFullPathName());
	    entry_exists = true;
	    if (inode == null)
	    {
	    	inode = s.newInstance(InodeTable.class);
	        inode.setName(node.getFullPathName());
	        entry_exists = false;
	    }
	    
	    inode.setModificationTime(node.modificationTime);
	    inode.setATime(node.getAccessTime());
	  
	    DataOutputBuffer permissionString = new DataOutputBuffer();
	    try {
			node.getPermissionStatus().write(permissionString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    long finalPerm = 0;
	    try {
			permissionString.writeLong(finalPerm);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	   // inode.setPermission(finalPerm);
	    inode.setParent(node.getParent().getFullPathName());
	    inode.setNSQuota(node.getNsQuota());
		inode.setDSQuota(node.getDsQuota());
		// TODO: Does not handle InodeDirectoryWithQuota yet
	    if (node instanceof INodeDirectory)
	    {
	    	inode.setIsDir(true);
	    }
	    if (node instanceof INodeDirectoryWithQuota)
	    {
	    	inode.setIsDirWithQuota(true);    	
	    	inode.setNSCount(((INodeDirectoryWithQuota) node).getNsCount());
	    	inode.setDSCount(((INodeDirectoryWithQuota) node).getDsCount());
	    }
	    if (node instanceof INodeFile)
	    {
	    	inode.setIsUnderConstruction(false);
	    	inode.setIsClosedFile(true);
	    	inode.setHeader(((INodeFile) node).getHeader());
	    }
	    if (node instanceof INodeFileUnderConstruction)
	    {
	    	inode.setIsUnderConstruction(true);
	    	inode.setIsClosedFile(false);
	    	inode.setClientName(((INodeFileUnderConstruction) node).getClientName());
	    	inode.setClientMachine(((INodeFileUnderConstruction) node).getClientMachine());
	    	inode.setClientNode(((INodeFileUnderConstruction) node).getClientNode().getName());
	    }
	    if (node instanceof INodeSymlink)
	    {
	    	//TODO:InodeSymlink here
	    	System.err.println("[Stateless] isInodeSymlink");    	
	    }
	    if (entry_exists)
	    	s.updatePersistent(inode);
	    else
	    	s.makePersistent(inode);
	    tx.commit();
	}

}