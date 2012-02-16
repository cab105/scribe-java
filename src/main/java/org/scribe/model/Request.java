package org.scribe.model;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;

import org.scribe.exceptions.*;

/**
 * Represents an HTTP Request object
 * 
 * @author Pablo Fernandez
 */
class Request
{
  private static final String CONTENT_LENGTH = "Content-Length";
  private static final String CONTENT_TYPE = "Content-Type";
  public static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String MULTIPART_BOUNDARY = "----MULTIPART_BOUNDARY_1_0";
  public static final String MULTIPART_CONTENT_TYPE = "multipart/form-data; boundary=" + MULTIPART_BOUNDARY;
  private static final String CRLF = "\r\n";
  private static final String CONTENT_DISP = "Content-Disposition: form-data";

  private String url;
  private Verb verb;
  private ParameterList querystringParams;
  private ParameterList bodyParams;
  private Map<String, String> headers;
  private String payload = null;
  private HttpURLConnection connection;
  private String charset;
  private byte[] bytePayload = null;
  private boolean connectionKeepAlive = false;
  private Long connectTimeout = null;
  private Long readTimeout = null;

  private boolean isMultipart = false;
  private LinkedList<MultipartBody> multipartBody;

  /**
   * Creates a new Http Request
   * 
   * @param verb Http Verb (GET, POST, etc)
   * @param url url with optional querystring parameters.
   */
  public Request(Verb verb, String url)
  {
    this.verb = verb;
    this.url = url;
    this.querystringParams = new ParameterList();
    this.bodyParams = new ParameterList();
    this.headers = new HashMap<String, String>();
    this.multipartBody = new LinkedList<MultipartBody>();
  }

  /**
   * Execute the request and return a {@link Response}
   * 
   * @return Http Response
   * @throws RuntimeException
   *           if the connection cannot be created.
   */
  public Response send()
  {
    try
    {
      createConnection();
      return doSend();
    }
    catch (UnknownHostException uhe)
    {
      throw new OAuthException("Could not reach the desired host. Check your network connection.", uhe);
    }
    catch (IOException ioe)
    {
      throw new OAuthException("Problems while creating connection.", ioe);
    }
  }

  private void createConnection() throws IOException
  {
    String completeUrl = getCompleteUrl();
    if (connection == null)
    {
      System.setProperty("http.keepAlive", connectionKeepAlive ? "true" : "false");
      connection = (HttpURLConnection) new URL(completeUrl).openConnection();
    }
  }

  /**
   * Returns the complete url (host + resource + encoded querystring parameters).
   *
   * @return the complete url.
   */
  public String getCompleteUrl()
  {
    return querystringParams.appendTo(url);
  }

  Response doSend() throws IOException
  {
    connection.setRequestMethod(this.verb.name());
    if (connectTimeout != null) 
    {
      connection.setConnectTimeout(connectTimeout.intValue());
    }
    if (readTimeout != null)
    {
      connection.setReadTimeout(readTimeout.intValue());
    }
    addHeaders(connection);
    if (verb.equals(Verb.PUT) || verb.equals(Verb.POST))
    {
      addBody(connection, getByteBodyContents());
    }
    return new Response(connection);
  }

  void addHeaders(HttpURLConnection conn)
  {
    for (String key : headers.keySet())
      conn.setRequestProperty(key, headers.get(key));
  }

  void addBody(HttpURLConnection conn, byte[] content) throws IOException
  {
    conn.setRequestProperty(CONTENT_LENGTH, String.valueOf(content.length));

    // Set default content type if none is set.
    if (conn.getRequestProperty(CONTENT_TYPE) == null)
    {
      conn.setRequestProperty(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
    }
    conn.setDoOutput(true);
    conn.getOutputStream().write(content);
  }

  /**
   * Add an HTTP Header to the Request
   * 
   * @param key the header name
   * @param value the header value
   */
  public void addHeader(String key, String value)
  {
    this.headers.put(key, value);
  }

  /**
   * Add a body Parameter (for POST/ PUT Requests)
   * 
   * @param key the parameter name
   * @param value the parameter value
   */
  public void addBodyParameter(String key, String value)
  {
    this.bodyParams.add(key, value);
  }
  
  /**
   * Add large data blobs that will become a part of a multi-part
   * http request (normally used for file uploads).
   *
   * @param type a MIME type describing the encoded data
   * @param encode base64 encode the data
   * @param data the blob
   */
   public void addMultipartBody(String type, String name, String filename, boolean encode, byte[] data) {
     MultipartBody mb;

	 if (encode) {
       // base64 encode the data stream and then store it
       mb = new MultipartBody(type, name, filename, true, Base64.encodeBase64URLSafe(data));
     } else {
       mb = new MultipartBody(type, name, filename, false, data);
     }
     multipartBody.add(mb);
     
     if (!isMultipart) {
       isMultipart = true;
       addHeader(CONTENT_TYPE, MULTIPART_CONTENT_TYPE);
     }
   }

  /**
   * Add a QueryString parameter
   *
   * @param key the parameter name
   * @param value the parameter value
   */
  public void addQuerystringParameter(String key, String value)
  {
    this.querystringParams.add(key, value);
  }

  /**
   * Add body payload.
   * 
   * This method is used when the HTTP body is not a form-url-encoded string,
   * but another thing. Like for example XML.
   * 
   * Note: The contents are not part of the OAuth signature
   * 
   * @param payload the body of the request
   */
  public void addPayload(String payload)
  {
    this.payload = payload;
  }

  /**
   * Overloaded version for byte arrays
   *
   * @param payload
   */
  public void addPayload(byte[] payload)
  {
    this.bytePayload = payload;
  }

  /**
   * Get a {@link ParameterList} with the query string parameters.
   * 
   * @return a {@link ParameterList} containing the query string parameters.
   * @throws OAuthException if the request URL is not valid.
   */
  public ParameterList getQueryStringParams()
  {
    try
    {
      ParameterList result = new ParameterList();
      String queryString = new URL(url).getQuery();
      result.addQuerystring(queryString);
      result.addAll(querystringParams);
      return result;
    }
    catch (MalformedURLException mue)
    {
      throw new OAuthException("Malformed URL", mue);
    }
  }

  /**
   * Obtains a {@link ParameterList} of the body parameters.
   * 
   * @return a {@link ParameterList}containing the body parameters.
   */
  public ParameterList getBodyParams()
  {
    return bodyParams;
  }

  /**
   * Obtains the URL of the HTTP Request.
   * 
   * @return the original URL of the HTTP Request
   */
  public String getUrl()
  {
    return url;
  }

  /**
   * Returns the URL without the port and the query string part.
   * 
   * @return the OAuth-sanitized URL
   */
  public String getSanitizedUrl()
  {
    return url.replaceAll("\\?.*", "").replace("\\:\\d{4}", "");
  }

  /**
   * Returns the body of the request
   * 
   * @return form encoded string
   * @throws OAuthException if the charset chosen is not supported
   */
  public String getBodyContents()
  {
    try
    {
      return new String(getByteBodyContents(),getCharset());
    }
    catch(UnsupportedEncodingException uee)
    {
      throw new OAuthException("Unsupported Charset: "+charset, uee);
    }
  }

  byte[] byteArrayConcat(byte[] a1, byte[] a2)
  {
	byte[] results = new byte[a1.length + a2.length];

	for (int i = 0; i < a1.length; i++) {
	  results[i] = a1[i];
	}

	for (int i = 0; i < a2.length; i++) {
		results[a1.length + i] = a2[i];
	}

	return results;
  }

  byte[] getByteBodyContents()
  {
	Logger log = Logger.getLogger("getbyteBodyContents");
    if (bytePayload != null) return bytePayload;
    String body = "";
	byte[] results, tmp;

	if (isMultipart) {
	  /* Given the multipart nature, we'll need to split off each parameter */
	  String params = (payload != null) ? payload : bodyParams.asFormUrlEncodedString();
	  
	  body += "--" + MULTIPART_BOUNDARY;

	  if (params.length() > 0) {
		body += CRLF + CONTENT_TYPE + ": application/x-url-encoded" + CRLF + CRLF;
		body += (payload != null) ? payload : bodyParams.asFormUrlEncodedString();
		body += CRLF + "--" + MULTIPART_BOUNDARY;
	  }

	  results = body.getBytes();
	  String footer = CRLF + "--" + MULTIPART_BOUNDARY;
	  Iterator<MultipartBody> i = multipartBody.iterator();
      while (i.hasNext()) {
        MultipartBody m = i.next();
        body = CRLF + CONTENT_TYPE + ": " + m.getHeader();

		if (m.getName() != null) {
			body += CRLF + CONTENT_DISP + "; name=\"" + m.getName() + "\"";
		}

		if (m.getFilename() != null) {
			body += "; filename=\"" + m.getFilename() + "\"";
		}

        if (m.isEncoded()) {
          body += CRLF + "Content-Transfer-Encoding: base64";
        }

        body += CRLF + CRLF;

		tmp = byteArrayConcat(results, body.getBytes());
		results = tmp;
		tmp = byteArrayConcat(results, m.getContents());
		results = tmp;
		tmp = byteArrayConcat(results, footer.getBytes());
		results = tmp;
      }
	  tmp = byteArrayConcat(results, new String("--" + CRLF).getBytes());
	  results = tmp;
    } else {
		body = (payload != null) ? payload : bodyParams.asFormUrlEncodedString();
		results = body.getBytes();
	}

	//log.info("body output:\n" + body);
/*
	try
	{
*/
	return results;
/*
      return body.getBytes(getCharset());
    } catch(UnsupportedEncodingException uee)
    {
      throw new OAuthException("Unsupported Charset: "+getCharset(), uee);
    }
 */
  }

  /**
   * Returns the HTTP Verb
   * 
   * @return the verb
   */
  public Verb getVerb()
  {
    return verb;
  }
  
  /**
   * Returns the connection headers as a {@link Map}
   * 
   * @return map of headers
   */
  public Map<String, String> getHeaders()
  {
    return headers;
  }

  /**
   * Returns the connection charset. Defaults to {@link Charset} defaultCharset if not set
   *
   * @return charset
   */
  public String getCharset()
  {
    return charset == null ? Charset.defaultCharset().name() : charset;
  }

  /**
   * Sets the connect timeout for the underlying {@link HttpURLConnection}
   * 
   * @param duration duration of the timeout
   * 
   * @param unit unit of time (milliseconds, seconds, etc)
   */
  public void setConnectTimeout(int duration, TimeUnit unit)
  {
    this.connectTimeout = unit.toMillis(duration);
  }

  /**
   * Sets the read timeout for the underlying {@link HttpURLConnection}
   * 
   * @param duration duration of the timeout
   * 
   * @param unit unit of time (milliseconds, seconds, etc)
   */
  public void setReadTimeout(int duration, TimeUnit unit)
  {
    this.readTimeout = unit.toMillis(duration);
  }

  /**
   * Set the charset of the body of the request
   *
   * @param charsetName name of the charset of the request
   */
  public void setCharset(String charsetName)
  {
    this.charset = charsetName;
  }

  /**
   * Sets whether the underlying Http Connection is persistent or not.
   *
   * @see http://download.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html
   * @param connectionKeepAlive
   */
  public void setConnectionKeepAlive(boolean connectionKeepAlive)
  {
    this.connectionKeepAlive = connectionKeepAlive;
  }

  /*
   * We need this in order to stub the connection object for test cases
   */
  void setConnection(HttpURLConnection connection)
  {
    this.connection = connection;
  }

  @Override
  public String toString()
  {
    return String.format("@Request(%s %s)", getVerb(), getUrl());
  }
}
