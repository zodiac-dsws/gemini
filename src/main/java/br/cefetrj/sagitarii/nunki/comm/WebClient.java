package br.cefetrj.sagitarii.nunki.comm;
/**
 * Copyright 2015 Carlos Magno Abreu
 * magno.mabreu@gmail.com 
 *
 * Licensed under the Apache  License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required  by  applicable law or agreed to in  writing,  software
 * distributed   under the  License is  distributed  on  an  "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the  specific language  governing  permissions  and
 * limitations under the License.
 * 
 */

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import br.cefetrj.sagitarii.nunki.Configurator;

public class WebClient {
	private Configurator gf;

	public WebClient( Configurator gf) {
		this.gf = gf;
	}
	
	public Configurator getConfig() {
		return gf;
	}

	public void doPost( String action, String parameter, String content) throws Exception {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost httppost = new HttpPost( gf.getHostURL() + "/" + action );

		// Request parameters and other properties.
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair( parameter, content) );
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		httpClient.execute(httppost);
		httpClient.close();

	}

	public String doGet(String action, String parameter) throws Exception {
		String result = "NO_ANSWER";
		String mhpHost = gf.getHostURL();
		String url = mhpHost + "/" + action + "?" + parameter;
		
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		
		/*
		if ( gf.useProxy() ) {
			if ( gf.getProxyInfo() != null ) {
				HttpHost httpproxy = new HttpHost( gf.getProxyInfo().getHost(), gf.getProxyInfo().getPort() ); 
				
				httpClient.getCredentialsProvider().setCredentials(
						  new AuthScope( gf.getProxyInfo().getHost(), gf.getProxyInfo().getPort() ),
						  new UsernamePasswordCredentials( gf.getProxyInfo().getUser(), gf.getProxyInfo().getPassword() ));
				httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, httpproxy);
						
			}
		}
		*/
		
		HttpGet getRequest = new HttpGet(url);
		getRequest.addHeader("accept", "application/json");
		getRequest.addHeader("Content-Type", "plain/text; charset=utf-8");
		getRequest.setHeader("User-Agent", "TaskManager Node");
		
		HttpResponse response = httpClient.execute(getRequest);
		response.setHeader("Content-Type", "plain/text; charset=UTF-8");

		int stCode = response.getStatusLine().getStatusCode();
		
		
		if ( stCode != 200) {
			// Error
		} else {
			HttpEntity entity = response.getEntity();
			InputStreamReader isr = new InputStreamReader(entity.getContent(), "UTF-8");
			result = convertStreamToString(isr);
			Charset.forName("UTF-8").encode(result);
			
			isr.close();
		}
		
		httpClient.close();
		
		return result;
	}
	
	private String convertStreamToString(java.io.InputStreamReader is) {
	    java.util.Scanner s = new java.util.Scanner(is);
		s.useDelimiter("\\A");
	    String retorno = s.hasNext() ? s.next() : "";
	    s.close();
	    return retorno;
	}	
}
