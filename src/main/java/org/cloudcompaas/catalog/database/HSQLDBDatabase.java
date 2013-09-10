/*******************************************************************************
 * Copyright (c) 2013, Andrés García García All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * (2) Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * (3) Neither the name of the Universitat Politècnica de València nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.cloudcompaas.catalog.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.hsqldb.lib.tar.DbBackup;

import org.cloudcompaas.common.util.Util;

import au.com.bytecode.opencsv.CSVReader;

public class HSQLDBDatabase extends BaseDatabase implements IDatabase {
	private String name;
	String cloudName = "mycloud";
	private String dbPath = "db";
	
    public void init(boolean ifexists) throws Exception { 
		Class.forName("org.hsqldb.jdbc.JDBCDriver");
		conn = DriverManager.getConnection("jdbc:hsqldb:file:"+dbPath+"/"+name+";ifexists="+ifexists, "sa", "");
    }
    
    public String createBackup() throws Exception {
    	Statement stmnt = conn.createStatement();
    	File tempfile = File.createTempFile("hsqldatabase", "");
    	File tempfile2 = new File(tempfile+".write.backup.tar.gz");
    	tempfile.delete();
		stmnt.execute("BACKUP DATABASE TO '"+tempfile2.getAbsolutePath()+"' COMPRESSED BLOCKING");
		return tempfile2.getAbsolutePath();
    }
    
	public synchronized String[][] query(String table, String select, String where) throws SQLException {   	
		String query = "select "+select+" from "+table+" where "+where;
    	return baseQuery(query);
    }
	
	public synchronized String[][] query(String table, String select) throws SQLException {   	
		String query = "select "+select+" from "+table;
    	return baseQuery(query);
    }
	
    public synchronized int insert(String table, String fields, String values) throws SQLException {
		String insert = "insert into "+table+" ("+fields+") values ("+values+")";
    	return baseInsert(insert);
    }
	
    public synchronized int delete(String table, String where) throws SQLException {
    	String delete = "delete from "+table+" where "+where;
    	return baseDelete(delete);
    }
    
    public synchronized int delete(String table) throws SQLException {
    	String delete = "delete from "+table;
    	return baseDelete(delete);
    }
    
    public synchronized int update(String table, String set, String where) throws SQLException {
    	String update = "update "+table+" set "+set+" where "+where;
    	return baseUpdate(update);
    }
    
    public void setName(String name_){
    	name = name_;
    }
    
	public synchronized void generateState(String repositoryPath) throws Exception{
		init(false);
		// Get a statement from the connection
        Statement stmt = conn.createStatement();
        String create = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Catalog.sql")));
        
        String line = br.readLine();
        while(line != null){
        	create += line;
        	line = br.readLine();
        }
        
        stmt.execute(create);
        
        File[] tables = new File(repositoryPath).listFiles();
        if(tables == null){
        	throw new Exception("State could not be generated. Path for repository invalid: "+repositoryPath);
        }
        
        stmt.execute("SET DATABASE REFERENTIAL INTEGRITY FALSE");
        
        for(int i = 0; i < tables.length; i++){
        	try{
           		if(tables[i].isDirectory() == true){
        			continue;
        		}
	        	String tablename = tables[i].getName().split("\\x2E")[0];
	        	br = new BufferedReader(new FileReader(tables[i]));

	            CSVReader reader = new CSVReader(br);

	            String[] values = reader.readNext();
	            if(values == null){
	            	continue;
	            }
	            String fields = "(";
	            for(int j = 0; j < values.length; j++){
	            	fields += values[j]+",";
	            }
	            fields = fields.substring(0, fields.length()-1);
	            fields += ")";
	            while(values != null){
	            	values = reader.readNext();
	            	if(values == null) break;
	            	Collection<File> binaryfiles = new Vector<File>();
	            	String insert = "insert into "+tablename+" "+fields+" values (";

	            	for(int j = 0; j < values.length; j++){
	            		if(values[j].startsWith("?{file") == true){
	            			insert += "?,";
	            			File binaryfile = new File(values[j].split("'")[1]);
	            			if(binaryfile.isAbsolute() == false){
	            				binaryfile = new File(repositoryPath+File.separatorChar+binaryfile.getPath());
	            			}
	            			binaryfiles.add(binaryfile);
	            		}else{
	            			insert += values[j]+",";
	            		}
	            	}
	            	insert = insert.substring(0, insert.length()-1);
	            	insert += ")";
	            	
	            	PreparedStatement pstm = conn.prepareStatement(insert);
	            	Iterator<File> bfit = binaryfiles.iterator();
	            	int binfileindex = 1;
	            	while(bfit.hasNext()){
	            		File binfile = bfit.next();
		            	pstm.setBinaryStream(binfileindex, new FileInputStream(binfile));
		            	binfileindex++;
	            	}
	            	pstm.execute();
	            	pstm.close();
	            }
        	}catch(Exception e){
	        	throw new Exception("State could not be generated. Bad formatted database file: "+tables[i].getAbsolutePath());
        	}
        }
        stmt.execute("SET DATABASE REFERENTIAL INTEGRITY TRUE");
	}
    
	public synchronized void connectToState(String servicesRepoPath) throws Exception {
		try{
			init(true);
		}catch(SQLException e){
			generateState(servicesRepoPath);
		}
	}
	
	public synchronized void recoverState(String backupPath) throws Exception {
		init(false);
    	File ff = new File(dbPath);
    	Util.deleteDir(ff);
    	
    	String[] args = new String[3];
    	args[0] = "--extract";
    	args[1] = backupPath;
    	args[2] = dbPath+"/";
    	DbBackup.main(args);

		name = "";
		
		File files[] = ff.listFiles();
		for(int i = 0; i < files.length; i++) {
			if (files[i].getName().endsWith(".script")) {
				name += files[i].getName().substring(0,files[i].getName().length()-".script".length());
			}
		}
    	
    	Class.forName("org.hsqldb.jdbcDriver");
    	conn = DriverManager.getConnection("jdbc:hsqldb:file:"+dbPath+"/"+name+";ifexists=true", "sa", "");
	}
	
    public void close() throws SQLException {
    	baseClose();
    }
}
