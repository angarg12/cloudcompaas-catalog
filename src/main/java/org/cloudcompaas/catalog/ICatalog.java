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
