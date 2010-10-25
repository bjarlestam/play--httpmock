package play.modules.httpmock;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import play.Logger;
import play.Play;
import play.libs.*;
import play.libs.WS.*;
import play.libs.ws.*;
import play.mvc.Http.Header;

public class WSMock extends WSAsync {
    
    /**
     * if true : if the request is known (cached), don't query the WebService but use the cache
     */
    public static boolean useCacheRequests = true;
    
    /**
     * if true : if the request is new (not cached), record it in the cache
     */
    public static boolean recordCacheRequests = true;
    
    @Override
    public WSRequest newRequest(String url) {
        return new WSMockRequest(url);
    }

    public WSCachedResponse createResponseFrom(File file) {
        return new WSCachedResponse(file);
    }
    
    public void writeResponseIntoFile(WSCachedResponse response, File file) throws IOException {
    	response.writeIntoFile(file);
    }
    
    public static File getFileByUrl(String urlStr) {
        File dir = Play.getFile("httpmock/");
        if(!dir.exists()) dir.mkdirs();
        return new File(dir, Codec.hexMD5(urlStr));
    }
    
    public static File getUrlsFile() {
    	File dir = Play.getFile("httpmock/");
    	dir.mkdirs();
    	return new File(dir, "urls");
    }
    
    /**
     * 
     * @param urls : map of <md5hash, url>
     */
    public static void saveUrls(Map<String, String> urls) {
    	try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(getUrlsFile()));
			for(String key : urls.keySet()) {
				writer.write(key+" "+urls.get(key));
				writer.newLine();
			}
			writer.close();
			retrieveUrls();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	private static Map<String, String> retrieveUrls() {
		Map<String, String> urls = new HashMap<String, String>();
		try {
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(getUrlsFile()));
			String line = reader.readLine();
			while (line != null && line.length() > 0) {
				int indexOf = line.indexOf(" ");
				if (indexOf != -1) {
					String key = line.substring(0, indexOf);
					String value = line.substring(indexOf + 1);
					File file = Play.getFile("httpmock/" + key);
					if (file.exists() && file.isFile())
						urls.put(key, value);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return urls;
    }
    public static Map<String, String> getUrls() {
    	return retrieveUrls();
    }
	public static void removeUrl(String id) {
		Play.getFile("httpmock/"+id).delete();
		Map<String, String> urls = getUrls();
		urls.remove(id);
		saveUrls(urls);
	}
	public static void removeUrls() {
		File dir = Play.getFile("httpmock/");
		if(dir.exists()) Files.deleteDirectory(dir);
		retrieveUrls();
	}
    
    public class WSMockRequest extends WSAsyncRequest {
        
        public WSMockRequest(String url) {
            super(url);
        }
        
        private HttpResponse cachedGet() {
            HttpResponse r = null;
            File f = getFileByUrl(url);
            if(f!=null) {
                if(!f.exists()) { // TODO : maybe it's better to override cache (idea to expore)
                    if(recordCacheRequests) {
                        r = super.get();
                        if(r!=null) {
                        	Logger.debug("WSMockRequest: GET on %s : caching ...", url);
                        	try {
								writeResponseIntoFile(new WSCachedResponse(r), f);
						        Map<String, String> urls = getUrls();
						        urls.put(f.getName(), url);
						        saveUrls(urls);
							} catch (IOException e) {
								e.printStackTrace();
							}
                        }
                    }
                }
                else if(useCacheRequests) {
                    Logger.debug("WSMockRequest: GET on %s : using cached request", url);
                    r = createResponseFrom(f);
                }
            }
            if(r==null)
                r = super.get();
            return r;
        }
        
        @Override
        public HttpResponse get() {
            return cachedGet();
        }
        
        @Override
        public Future<HttpResponse> getAsync() { 
            // fake getAsync to be cached
            return new Future<HttpResponse>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }
                @Override
                public HttpResponse get() throws InterruptedException, ExecutionException {
                    return cachedGet();
                }
                @Override
                public HttpResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return cachedGet();
                }
                @Override
                public boolean isCancelled() {
                    return false;
                }
                @Override
                public boolean isDone() {
                    return false;
                }
                
            };
        }
    }
    
    @SuppressWarnings("serial")
	public class WSCachedResponse extends HttpResponse implements Serializable {
        
        Map<String, String> headersMap = new HashMap<String, String>();
        List<Header> headers;
        Integer status;
        InputStream stream;
        
        public WSCachedResponse(HttpResponse r) {
            headers = r.getHeaders();
            for(Header h : headers)
                headersMap.put(h.name, h.value());
            status = r.getStatus();
            stream = r.getStream();
        }
        
        public WSCachedResponse(File file) {
            try {
                status = 200;
                headers = new ArrayList<Header>();
                final BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                while(line!=null && line.length()>0) {
                	int indexOf = line.indexOf(": ");
                	if(indexOf != -1) {
                		String key = line.substring(0, indexOf);
                		String value = line.substring(indexOf+2);
                		headersMap.put(key, value);
                		headers.add(new Header(key, value));
                	}
                	line = reader.readLine();
                }
                stream = new InputStream() {
					@Override
					public int read() throws IOException {
						return reader.read();
					}
				};
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        
        public void writeIntoFile(File file) throws IOException {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for (String key : headersMap.keySet()) {
				writer.write(key + ": " + headersMap.get(key));
				writer.newLine();
			}
			writer.newLine(); // empty line separating headers and content
			writer.write(IO.readContentAsString(stream));
			writer.close();
        }
        
        @Override
        public String getHeader(String key) {
            return headersMap.get(key);
        }

        @Override
        public List<Header> getHeaders() {
            return headers;
        }

        @Override
        public Integer getStatus() {
            return status;
        }

        @Override
        public InputStream getStream() {
            return stream;
        }

        @Override
        public String getString() {
            return IO.readContentAsString(stream);
        }
    }
    
}
