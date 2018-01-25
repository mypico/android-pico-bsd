/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.crypto.AuthToken;
import android.content.Context;
import android.content.Intent;

import com.google.common.base.Optional;

final public class HttpWriteAuthToken extends WriteAuthToken {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            HttpWriteAuthToken.class.getSimpleName());
	
	public static final String PARAM_NAME = "auth_token";
	public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
	
	public static enum Method {
		GET,
		POST
	}

    private URL url;
    private Method method;
    private int timeout;

    public HttpWriteAuthToken(Context context, Optional<Intent> fallback, URL url, Method method, int timeout) {
    	super(context, fallback);
    	
    	this.url = url;
    	this.method = method;
        this.timeout = timeout;
    }

    @Override
    protected boolean write(AuthToken token) {
        LOGGER.debug("Sending auth token to ", url.getHost());
        
        String data;
		try {
			data = PARAM_NAME + "=" + URLEncoder.encode(token.getFull(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 not supported", e);
		}
        
        HttpURLConnection connection;
        HttpURLConnection.setFollowRedirects(false);
        
        try {
	        switch (method) {
	        	case GET:
					URL requestUrl = null;
					try {
						requestUrl = new URL(url, "?" + data);
					} catch (MalformedURLException e) {
						throw new RuntimeException("Unexpected MalformedURLException", e);
					}
	        		LOGGER.debug("GET {}", requestUrl);
	        		
	        		connection = (HttpURLConnection) requestUrl.openConnection();
	        		connection.setRequestMethod("GET");
	        	case POST:
	        	default:
	        		LOGGER.debug("POST {}", url);
	        		
	        		final byte[] postBytes = data.getBytes("UTF-8");
	        		connection = (HttpURLConnection) url.openConnection();
	        		connection.setRequestMethod("POST");
	                connection.setRequestProperty("Content-Type", CONTENT_TYPE);
	                connection.setRequestProperty("Content-Length", Integer.toString(postBytes.length));
	
	                connection.setDoOutput(true);
	                OutputStream os = null;
	                try {
	                    os = connection.getOutputStream();
	                    os.write(postBytes);
	                    os.flush();
	                } finally {
	                    if (os != null) {
	                        os.close();
	                    }
	                }
	        }
	        LOGGER.trace("Request headers: {}", connection.getRequestProperties());
	        
			connection.setReadTimeout(timeout);
			return (connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
        	LOGGER.warn("IOException occured whilst writing auth token", e);
        	return false;
        }
    }
}
