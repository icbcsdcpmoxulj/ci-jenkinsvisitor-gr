/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License") you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//      Contributors:      Xu Lijia

package ci.xlj.libs.jenkinsvisitor

import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.auth.AuthScheme
import org.apache.http.auth.AuthScope
import org.apache.http.auth.AuthState
import org.apache.http.auth.Credentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.protocol.ClientContext
import org.apache.http.protocol.ExecutionContext
import org.apache.http.protocol.HttpContext

class PreemptiveAuth implements HttpRequestInterceptor{

	void process(HttpRequest request, HttpContext context)
	throws HttpException, IOException {
		// Get the AuthState
		AuthState authState = (AuthState) context
				.getAttribute(ClientContext.TARGET_AUTH_STATE)

		// If no auth scheme available yet, try to initialize it
		// preemptively
		if (!authState.getAuthScheme()) {
			AuthScheme authScheme = (AuthScheme) context
					.getAttribute("preemptive-auth")
			CredentialsProvider credsProvider = (CredentialsProvider) context
					.getAttribute(ClientContext.CREDS_PROVIDER)
			HttpHost targetHost = (HttpHost) context
					.getAttribute(ExecutionContext.HTTP_TARGET_HOST)
					
			if (authScheme) {
				Credentials creds = credsProvider.getCredentials(new AuthScope(
						targetHost.getHostName(), targetHost.getPort()))
				if (!creds) {
					throw new HttpException(
					"No credentials for preemptive authentication")
				}
				
				authState.setAuthScheme(authScheme)
				authState.setCredentials(creds)
			}
		}
	}

}
