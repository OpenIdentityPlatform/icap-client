package net.rfc3507.client;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class ICAPResponse implements Serializable {
	
	private Map<String, List<String>> headerEntries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); 

	private Set<String> headers = new LinkedHashSet<>();
	
	private String protocol;
	private String version;
	private int status;
	private String message;
	
	private byte[] httpRequestHeader;
	private byte[] httpRequestBody;
	
	private byte[] httpResponseHeader;
	private byte[] httpResponseBody;
	
	ICAPResponse() {
		
	}
	
	void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	void setVersion(String version) {
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}
	
	void setStatus(int status) {
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
	
	void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String recoverStatusLine() {
		return "ICAP/"+getVersion()+" "+getStatus()+" "+getMessage();
	}
	
	void addHeader(String header, String value) {
		
		headers.add(header);
		
		String h = header.toLowerCase();
		
		List<String> values = headerEntries.get(h);
		if(values == null) {
			values = new LinkedList<>();
			headerEntries.put(h, values);
		}
		values.add(value);
		
	}
	
	public Set<String> getHeaders() {
		return Collections.unmodifiableSet(headers);
	}
	
	public Map<String, List<String>> getHeaderEntries() {
		return Collections.unmodifiableMap(headerEntries);
	}
	
	public List<String> getHeaderValues(String header) {
		return headerEntries.get(header.toLowerCase());
	}
	
	public boolean containHeaderValue(String header, String content) {
		
		List<String> values = headerEntries.get(header.toLowerCase());
		
		if(values == null) {
			return false;
		}
		
		for(String value: values) {
			if(value.contains(content)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	public String getHeader(String header) {
		return headerEntries.get(header.toLowerCase()).get(0);
	}
	
	public int getIntegerHeader(String header) {
		return Integer.parseInt(headerEntries.get(header.toLowerCase()).get(0));
	}
	
	void setHttpRequestHeader(byte[] httpRequestHeader) {
		this.httpRequestHeader = httpRequestHeader;
	}
	
	public byte[] getHttpRequestHeader() {
		return httpRequestHeader;
	}
	
	void setHttpRequestBody(byte[] httpRequestBody) {
		this.httpRequestBody = httpRequestBody;
	}
	
	public byte[] getHttpRawRequestBody() {
		return httpRequestBody;
	}
	
	public byte[] getHttpShrinkRequestBody() {
		return shrinkHttpPayload(httpRequestBody);
	}
	
	public static class ResponseHeader {
		final static Pattern statusLinePattern=Pattern.compile("\\S+\\s(\\d{3})\\s(.*)");
		final static Pattern headersPattern=Pattern.compile("(.*):\\s(.*)");
		
		public ResponseHeader(byte[] httpResponseHeader) {
			final String data=new String(httpResponseHeader);
			Matcher matcher = statusLinePattern.matcher(data);
		    if(matcher.find()) {
		    	status=Integer.parseInt(matcher.group(1));
		    	message=matcher.group(2);
		    }
		    headerEntries.clear();
		    matcher = headersPattern.matcher(data);
		    while (matcher.find()) {
		    	List<String> values=headerEntries.get(matcher.group(1));
		    	if (values==null) {
		    		values=new ArrayList<String>(1);
		    	}
		    	values.add(matcher.group(2));
		    	headerEntries.put(matcher.group(1), values);
		    }
		}

		int status=403;
		public int getStatus() {
			return this.status; 
		}
		
		String message="Forbidden";
		public String getMessage() {
			return this.message; 
		}
		
		Map<String, List<String>> headerEntries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); 
		public Map<String, List<String>> getHeaderEntries() {
			return Collections.unmodifiableMap(headerEntries);
		}
	}
	
	ResponseHeader responseHeader=null;
	public ResponseHeader getResponseHeader() {
		return responseHeader;
	}
	
	
	void setHttpResponseHeader(byte[] httpResponseHeader) {
		this.httpResponseHeader = httpResponseHeader;
		this.responseHeader=new ResponseHeader(httpResponseHeader);
	}
	
	public byte[] getHttpResponseHeader() {
		return httpResponseHeader;
	}
	
	void setHttpResponseBody(byte[] httpResponseBody) {
		this.httpResponseBody = httpResponseBody;
	}
	
	public byte[] getHttpRawResponseBody() {
		return httpResponseBody;
	}
	
	public byte[] getHttpShrinkResponseBody() {
		return shrinkHttpPayload(httpResponseBody);
	}
	
	private static byte[] shrinkHttpPayload(byte[] payload) {
		
		if(payload == null) return null;
		
		StringBuilder line = null;
		ByteArrayOutputStream shrink = new ByteArrayOutputStream();
		
		int mark1 = -1, mark2 = -1, mark3 = -1, mark4 = -1;
		int amountToRead = -1;
		
		for( int offset = 0; offset < payload.length; ++offset ) {
			
			mark1 = mark2;
			mark2 = mark3;
			mark3 = mark4;
			mark4 = payload[offset];
			
			if(    mark1 == '\r'
				&& mark2 == '\n'
				&& mark3 == '\r' 
				&& mark4 == '\n' ) {
				
				break;
				
			}
			
			if( mark4 == '\r' ) {
				continue;
			}
			
			if(    mark3 == '\r'
				&& mark4 == '\n' ) {
				
				offset++;
				
				amountToRead = Integer.parseInt(line.toString(), 16);
				shrink.write(payload, offset, amountToRead);
				offset += (amountToRead + 1);
				line = null;
				mark1 = mark2 = mark3 = mark4 = -1;
				continue;
				
			}
			
			if( line == null ) {
				line = new StringBuilder("");
			}
			
			line.append((char)mark4);
			
		}
		
		return shrink.toByteArray();
		
	}
	
	@Override
	public String toString() {
		return recoverStatusLine();
	}
	
	
}
