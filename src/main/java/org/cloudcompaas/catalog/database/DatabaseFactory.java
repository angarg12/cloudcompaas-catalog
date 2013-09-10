package org.cloudcompaas.catalog.database;

public class DatabaseFactory {

	public static IDatabase getInstance(){
		return new HSQLDBDatabase();
	}
}
