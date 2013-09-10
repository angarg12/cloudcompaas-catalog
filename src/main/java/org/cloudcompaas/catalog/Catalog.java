package org.cloudcompaas.catalog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.wink.common.annotations.Scope;
import org.apache.wink.common.http.HttpStatus;
import org.cloudcompaas.catalog.database.DatabaseFactory;
import org.cloudcompaas.catalog.database.IDatabase;
import org.cloudcompaas.common.components.Component;
import org.cloudcompaas.common.components.Register;
import org.cloudcompaas.common.util.Util;

@Scope(Scope.ScopeType.SINGLETON)
@Path("/")
public class Catalog extends Component implements ICatalog {
	private IDatabase db;
	private String statePath;
	private String backupPath;
	private String dbname;
	private String state;

	public Catalog() throws Exception {
		super();
		Properties properties = new Properties();

		properties.load(getClass().getResourceAsStream("/conf/Catalog.properties"));

		state = properties.getProperty("state");
		statePath = properties.getProperty("statePath");
		dbname = properties.getProperty("dbname");
		backupPath = properties.getProperty("backupPath");
		
		db = DatabaseFactory.getInstance();
		db.setName(dbname);
		generateState();

		String service = properties.getProperty("service");
		String version = properties.getProperty("version");
		String epr = properties.getProperty("epr");

		Register register = new Register(Thread.currentThread(), service, version, epr);
		register.start();
	}

	@GET
	@Path("{table}/search")
	@Produces("text/xml")
	public Response query(@HeaderParam("Authorization") String auth, @Context UriInfo info, 
			@PathParam("table") String table){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}

		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try{
			String where = processQueryString(info.getQueryParameters());

			String[][] response = db.query(table, "*", where);
			
			if(response.length < 2){
				status = HttpStatus.NOT_FOUND.getCode();
				throw new Exception("Item not found: "+table);
			}
			
			String responseString = "";
			responseString+="<"+table+"s>";
			for(int i = 3; i < response.length; i++){
				responseString+="<"+table+">";
				for(int j = 0; j < response[i].length; j++){
					responseString+="<"+response[0][j];
					if(response[1][j] != null){
						// Since all foreign keys point to the other table PK, this reference points to the
						// entry in the table pointed by this field. That is why we extract the table name
						// and use the field value. 
						responseString+=" href=\"/"+response[1][j].split("\\.")[0]+"/"+response[i][j]+"\"";
					}
					responseString+=">";
					responseString+=response[i][j];
					responseString+="</"+response[0][j]+">";
				}
				responseString+="</"+table+">";
			}
			responseString+="</"+table+"s>";
			
			return Response
			   .status(HttpStatus.OK.getCode())
			   .entity(responseString)
			   .build();
		}catch(Exception e){
			e.printStackTrace();
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
	
	private String processQueryString(MultivaluedMap<String,String> pairs){
		Iterator<String> it = pairs.keySet().iterator();
		String where = "";
		// in the outer loop we append the AND parameters, that is, those separated by &
		if(it.hasNext()){
			String key = it.next();
			String[] parameters = pairs.getFirst(key).split(",");
			String value = processParameter(parameters[0]);
			// and build the where statement
		    where += key+value;
			// in the inner loop we append the OR parameters, that is, those separated by ,
			for(int i = 1; i < parameters.length; i++){
				value = processParameter(parameters[i]);
				// and build the where statement
			    where += " or "+key+value;
			}
			// outer loop
			while(it.hasNext()){
				key = it.next();
				parameters = pairs.getFirst(key).split(",");
				where += " and ";

				value = processParameter(parameters[0]);
				// and build the where statement
			    where += key+value;

				// inner loop again
				for(int i = 1; i < parameters.length; i++){
					value = processParameter(parameters[i]);
					// and build the where statement
				    where += " or "+key+value;
				}
			}
		}
		return where;
	}
	
	private String processParameter(String parameter){
		Pattern p = Pattern.compile("(?<!\\\\)(\\\\\\.)*(\\.)|(?<!\\\\)(\\\\\\*)*(\\*)");
		Matcher m = p.matcher(parameter);
		// lets check for three cases: first, it contains wildcards. We need to subtitute them and
		// build a LIKE statement
		if(m.find() == true){
			parameter = parameter.replaceAll("_", "\\\\_");
			parameter = parameter.replaceAll("%", "\\\\%");
			char[] string = parameter.toCharArray();
			m = p.matcher(parameter);
			while(m.find()){
				if(m.start(2) > -1){
					string[m.start(2)] = '_';
				}
				if(m.start(4) > -1){
					string[m.start(4)] = '%';
				}
			}
			parameter = " LIKE '"+String.valueOf(string)+"' ESCAPE '\\'";
	    }else if(Util.isNumeric(parameter)){ // if it is numeric, don't use quotes
	    	parameter = "="+parameter;
		}else{ // if it is not numeric and does not contain wildcards, use a regular equals
			parameter = "='"+parameter+"'";
		}
		// unscape escaped wildcards
		parameter = parameter.replaceAll("\\\\\\*", "\\*");
		parameter = parameter.replaceAll("\\\\\\.", "\\.");
		return parameter;
	}
	
	@GET
	@Produces("text/xml")
	@Path("{table}/{id}/{column}")
	public Response get(@HeaderParam("Authorization") String auth, @PathParam("table") String table, 
			@PathParam("id") String elementId, @PathParam("column") String column){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}
		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try {
			String where = "id_"+table+" = "+elementId;
			String[][] response;
			response = db.query(table, column, where);
			if(response.length < 4){
				status = HttpStatus.NOT_FOUND.getCode();
				throw new Exception("Item not found: "+table+"/"+elementId);
			}
			
			String responseString = "";
			for(int i = 3; i < response.length; i++){
				for(int j = 0; j < response[i].length; j++){
					if(response[i][j] != null){
						responseString+="<"+response[0][j];
						if(response[1][j] != null){
							// Since all foreign keys point to the other table PK, this reference points to the
							// entry in the table pointed by this field. That is why we extract the table name
							// and use the field value. 
							responseString+=" href=\"/"+response[1][j].split("\\.")[0]+"/"+response[i][j]+"\"";
						}
						responseString+=">";
						responseString+=response[i][j];
						responseString+="</"+response[0][j]+">";
					}
				}
			}

	        return Response
	        .status(HttpStatus.OK.getCode())
	        .entity(responseString)
	        .build();
		} catch (Exception e) {
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
	
	@GET
	@Produces("text/xml")
	@Path("{table}/{id}")
	public Response get(@HeaderParam("Authorization") String auth, @PathParam("table") String table, 
			@PathParam("id") String elementId){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}
		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try {
			String where = "id_"+table+" = "+elementId;
			String[][] response;
			response = db.query(table, "*", where);
			if(response.length < 4){
				status = HttpStatus.NOT_FOUND.getCode();
				throw new Exception("Item not found: "+table+"/"+elementId);
			}
			
			String responseString = "";
			for(int i = 3; i < response.length; i++){
				responseString+="<"+table+">";
				for(int j = 0; j < response[i].length; j++){
					if(response[i][j] != null){ 
						responseString+="<"+response[0][j];
						if(response[1][j] != null){
							// Since all foreign keys point to the other table PK, this reference points to the
							// entry in the table pointed by this field. That is why we extract the table name
							// and use the field value.
							responseString+=" href=\"/"+response[1][j].split("\\.")[0]+"/"+response[i][j]+"\"";
						}
						responseString+=">";
						responseString+=response[i][j];
						responseString+="</"+response[0][j]+">";
					}
				}
				responseString+="</"+table+">";
			}

	        return Response
	        .status(HttpStatus.OK.getCode())
	        .entity(responseString)
	        .build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
	
	@GET
	@Produces("text/xml")
	@Path("{table}")
	public Response get(@HeaderParam("Authorization") String auth, @PathParam("table") String table){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}
		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try {
			String[][] response;
			response = db.query(table, "id_"+table);
			if(response.length < 2){
				status = HttpStatus.NOT_FOUND.getCode();
				throw new Exception("Item not found: "+table);
			}
			
			String responseString = "";
			responseString+="<"+table+"s>";
			for(int i = 3; i < response.length; i++){
				if(response[i][0] != null){ 
					responseString+="<"+table+" href=\""+table+"/"+response[i][0]+"\">";
					responseString+=response[i][0];
					responseString+="</"+table+">";
				}
			}
			responseString+="</"+table+"s>";

	        return Response
	        .status(HttpStatus.OK.getCode())
	        .entity(responseString)
	        .build();
		} catch (Exception e) {
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
	
	@POST
	@Path("{table}")
	@Consumes("text/xml")
	@Produces("text/plain")
	public Response post(@HeaderParam("Authorization") String auth, @PathParam("table") String table, 
			String values) {
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}
		
		try{
			Properties properties = new Properties();
			// convert String into InputStream
			InputStream is = new ByteArrayInputStream(values.getBytes());
			properties.loadFromXML(is);
			
			Enumeration<Object> keys = properties.keys();
			
			String strNames = "";
			String strValues = "";
			if(keys.hasMoreElements()){
				String nextElement = (String) keys.nextElement();
				strNames += nextElement;
				strValues = "'"+properties.getProperty(nextElement).replace("'", "''");
				while(keys.hasMoreElements()){
					nextElement = (String) keys.nextElement();
					strNames += ","+nextElement;
					strValues += "','"+properties.getProperty(nextElement).replace("'", "''");
				}
				strValues += "'";
			}

			int key = db.insert(table, strNames, strValues);
			String keyXml = "<id_"+table+">"+key+"</id_"+table+">";
	        return Response
	        .status(HttpStatus.OK.getCode())
	        .entity(String.valueOf(keyXml))
	        .build();
		}catch(Exception e){
			return Response
	        .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
	        .entity(e.getMessage())
	        .build();
		}
	}

	@PUT
	@Path("{table}/search")
	@Consumes("text/xml")
	public Response queryPut(@HeaderParam("Authorization") String auth, @Context UriInfo info,
			@PathParam("table") String table, String values){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}

		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try{
			String where = processQueryString(info.getQueryParameters());

			Properties properties = new Properties();
			// convert String into InputStream
			InputStream is = new ByteArrayInputStream(values.getBytes());
			properties.loadFromXML(is);
			
			Enumeration<Object> keys = properties.keys();
			
			String strSet = "";
			if(keys.hasMoreElements()){
				String nextElement = (String) keys.nextElement();
				String value = properties.getProperty(nextElement).replace("'", "''");
				/* 
				 * We use the sequence ?! as an escape sequence for expressions 
				 * This allow us to inline sql expressions in the update command. 
				 */
				if(value.startsWith("?!")){
					strSet += nextElement+" = "+value.substring(2);
				}else{
					strSet += nextElement+" = '"+value+"'";
				}
				while(keys.hasMoreElements()){
					nextElement = (String) keys.nextElement();
					value = properties.getProperty(nextElement).replace("'", "''");
					if(value.startsWith("?!")){
						strSet += ","+nextElement+" = "+value.substring(2);
					}else{
						strSet += ","+nextElement+" = '"+value+"'";
					}
				}
			}
			
			db.update(table, strSet, where);
			
			return Response
			   .status(HttpStatus.OK.getCode())
			   .build();
		}catch(Exception e){
			e.printStackTrace();
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}

	@PUT
	@Path("{table}/{id}")
	@Consumes("text/xml")
	public Response put(@HeaderParam("Authorization") String auth, @PathParam("table") String table, 
			@PathParam("id") String elementId, String values){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}
		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try{
			Properties properties = new Properties();
			// convert String into InputStream
			InputStream is = new ByteArrayInputStream(values.getBytes());
			properties.loadFromXML(is);
			
			Enumeration<Object> keys = properties.keys();
			
			String strSet = "";
			if(keys.hasMoreElements()){
				String nextElement = (String) keys.nextElement();
				String value = properties.getProperty(nextElement).replace("'", "''");
				/* 
				 * We use the sequence ?! as an escape sequence for expressions 
				 * This allow us to inline sql expressions in the update command. 
				 */
				if(value.startsWith("?!")){
					strSet += nextElement+" = "+value.substring(2);
				}else{
					strSet += nextElement+" = '"+value+"'";
				}
				while(keys.hasMoreElements()){
					nextElement = (String) keys.nextElement();
					value = properties.getProperty(nextElement);
					if(value.startsWith("?!")){
						strSet += ","+nextElement+" = "+value.substring(2);
					}else{
						strSet += ","+nextElement+" = '"+value+"'";
					}
				}
			}

			int modifiedCount = db.update(table, strSet, "id_"+table+" = "+elementId);
			if(modifiedCount == 0){
				status = HttpStatus.NOT_FOUND.getCode();
				throw new Exception("Item not found: "+table+"/"+elementId);
			}
	        return Response
	        .status(HttpStatus.OK.getCode())
	        .build();
		} catch (Exception e) {
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
	
	@PUT
	@Path("{table}/{id}/{column}")
	@Consumes("text/plain")
	public Response put(@HeaderParam("Authorization") String auth, @PathParam("table") String table, 
			@PathParam("id") String elementId, @PathParam("column") String column, String value){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}
		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try{
			String set = "";
			if(value.startsWith("?!")){
				set = column+"="+value.substring(2);
			}else{
				set = column+"='"+value+"'";
			}
			int modifiedCount = db.update(table, 
					set, "id_"+table+" = "+elementId);
			if(modifiedCount == 0){
				status = HttpStatus.NOT_FOUND.getCode();
				throw new Exception("Item not found: "+table+"/"+elementId);
			}
	        return Response
	        .status(HttpStatus.OK.getCode())
	        .build();
		} catch (Exception e) {
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
	
	@DELETE
	@Path("{table}/search")
	public Response queryDelete(@HeaderParam("Authorization") String auth, @Context UriInfo info, 
			@PathParam("table") String table){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}

		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try{
			String where = processQueryString(info.getQueryParameters());

			db.delete(table, where);
			
			return Response
			   .status(HttpStatus.OK.getCode())
			   .build();
		}catch(Exception e){
			e.printStackTrace();
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
	
	@DELETE
	@Path("{table}/{id}")
	public Response delete(@HeaderParam("Authorization") String auth, @PathParam("table") String table, 
			@PathParam("id") String elementId){
		if(auth == null || securityHandler.authenticate(auth) == false){
			return Response
			.status(HttpStatus.UNAUTHORIZED.getCode())
			.build();
		}
		int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
		try{
			int deletedCount = db.delete(table, "id_"+table+" = "+elementId);
			if(deletedCount == 0){
				status = HttpStatus.NOT_FOUND.getCode();
				throw new Exception("Item not found: "+table+"/"+elementId);
			}
	        return Response
	        .status(HttpStatus.OK.getCode())
	        .build();
		} catch (Exception e) {
			return Response
	        .status(status)
	        .entity(e.getMessage())
	        .build();
		}
	}
		
	public void generateState() throws Exception {
		if(state.equalsIgnoreCase("database") == true){
			db.connectToState(statePath);
		}
		if(state.equalsIgnoreCase("backup") == true){
			db.recoverState(backupPath);
		}
	}
}
