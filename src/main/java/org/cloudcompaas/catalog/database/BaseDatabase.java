package org.cloudcompaas.catalog.database;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;


public class BaseDatabase {
	protected Connection conn = null;

    public String[][] baseQuery(String q) throws SQLException {   	
        // Get a statement from the connection
        Statement stmt = conn.createStatement();

        // Execute the query
        ResultSet rs = stmt.executeQuery(q);
        ResultSetMetaData rsmd = rs.getMetaData();
        
        Collection<String[]> rows = new Vector<String[]>();
        int cols = rsmd.getColumnCount();
        String[] columns = new String[cols];
        // Set for storing the participating tables names. Contains no duplicates.
        Set<String> tableNames = new HashSet<String>();
    	for(int i = 0; i < cols; i++){
    		// use i+1 since the column count starts at 1.
    		columns[i] = rsmd.getColumnName(i+1).toLowerCase();
    		tableNames.add(rsmd.getTableName(i+1));
    	}
    	rows.add(columns);
    	
        Iterator<String> i = tableNames.iterator();
        DatabaseMetaData meta = conn.getMetaData();
        
        ResultSet metaRs;
        while(i.hasNext()){
        	String tableName = i.next();
	        // The Oracle database stores its table names as Upper-Case,
	        // if you pass a table name in lowercase characters, it will not work.
	        // MySQL database does not care if table name is uppercase/lowercase.
	        //
        	String[] foreignKeys = new String[cols];
        	metaRs = meta.getImportedKeys(conn.getCatalog(), null, tableName);    
	        while (metaRs.next()) {
	            String fkTableName = metaRs.getString("FKTABLE_NAME");
	            String fkColumnName = metaRs.getString("FKCOLUMN_NAME");
	            String pkTableName = metaRs.getString("PKTABLE_NAME");
	            String pkColumnName = metaRs.getString("PKCOLUMN_NAME");
	            for(int j = 0; j < cols; j++){
	            	if(rsmd.getTableName(j+1).equals(fkTableName) && 
	            		rsmd.getColumnName(j+1).equals(fkColumnName)){
	            		foreignKeys[j] = pkTableName+"."+pkColumnName;
	            	}
	            }
	          }
	        
	        rows.add(foreignKeys);
	        foreignKeys = new String[cols];
	        metaRs.close();
	        metaRs = meta.getExportedKeys(conn.getCatalog(), null, tableName);
	        while (metaRs.next()) {
	          String fkTableName = metaRs.getString("FKTABLE_NAME");
	          String fkColumnName = metaRs.getString("FKCOLUMN_NAME");
	          String pkTableName = metaRs.getString("PKTABLE_NAME");
	          String pkColumnName = metaRs.getString("PKCOLUMN_NAME");
	            for(int j = 0; j < cols; j++){
	            	if(rsmd.getTableName(j+1).equals(pkTableName) && 
	            		rsmd.getColumnName(j+1).equals(pkColumnName)){
	            		foreignKeys[j] = fkTableName+"."+fkColumnName;
	            	}
	            }
	        }
	        rows.add(foreignKeys);
	        metaRs.close();
        }
        // Loop through the result set
        while(rs.next()){
        	columns = new String[cols];
        	for(int j = 0; j < cols; j++){
        		if(rs.getObject(j+1) != null){
        			columns[j] = rs.getObject(j+1).toString();
        		}
        	}
        	rows.add(columns);
        }
        // Close the result set, statement and the connection
        rs.close() ;
        stmt.close();

        String[][] result = new String[rows.size()][cols];
        return rows.toArray(result);
    }
    
    public int baseInsert(String i) throws SQLException {
        // Get a statement from the connection
        Statement stmt = conn.createStatement();

        // Execute the sentence
        stmt.executeUpdate(i,Statement.RETURN_GENERATED_KEYS);
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
        	int key = rs.getInt(1);
        	stmt.close();
        	return key;
        } else {
            throw new SQLException("Creating user failed, no generated key obtained.");
        }
    }
    
    public int baseDelete(String d) throws SQLException {
        // Get a statement from the connection
        Statement stmt = conn.createStatement();

        // Execute the sentence
        int count = stmt.executeUpdate(d);
        stmt.close();

        return count;
    }
    
    public int baseUpdate(String u) throws SQLException {
        // Get a statement from the connection
        Statement stmt = conn.createStatement();

        // Execute the sentence
        int count = stmt.executeUpdate(u);

        stmt.close();

        return count;
    }

    public void baseClose() throws SQLException {
		conn.close();
    }
}
