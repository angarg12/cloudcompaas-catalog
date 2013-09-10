package org.cloudcompaas.catalog;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public interface ICatalog {
	public Response query(String auth, UriInfo info, String table);
	public Response get(String auth, String table, String elementId, String column);
	public Response get(String auth, String table, String elementId);
	public Response get(String auth, String table);
	public Response post(String auth, String table, String values);
	public Response queryPut(String auth, UriInfo info, String table, String values);
	public Response put(String auth, String table, String elementId, String values);
	public Response put(String auth, String table, String elementId, String column, String value);
	public Response queryDelete(String auth, UriInfo info, String table);
	public Response delete(String auth, String table, String elementId);
}