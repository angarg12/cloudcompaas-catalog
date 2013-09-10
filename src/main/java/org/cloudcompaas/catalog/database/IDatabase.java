package org.cloudcompaas.catalog.database;

import java.sql.SQLException;

public interface IDatabase {

	public String[][] query(String table, String select, String where) throws SQLException;
	
	public String[][] query(String table, String select) throws SQLException;
	
    public int insert(String table, String fields, String values) throws SQLException;
    
    public int delete(String table, String where) throws SQLException;
    
    public int delete(String table) throws SQLException;
    
    public int update(String table, String set, String where) throws SQLException;

    public void setName(String name);
    
    public void generateState(String repositoryPath) throws Exception;
    
    public void connectToState(String servicesRepoPath) throws Exception;
    
    public void recoverState(String backupPath) throws Exception;
    
    public void close() throws SQLException;
}
