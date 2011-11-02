package play.modules.httpmock;

import org.apache.commons.io.IOUtils;
import play.Logger;
import play.Play;
import play.libs.Codec;
import play.libs.F.Promise;
import play.libs.Files;
import play.libs.IO;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.libs.ws.WSAsync;
import play.mvc.Http.Header;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    public WSRequest newRequest(String url, String encoding) {
        return new WSMockRequest(url, encoding);
    }

    public WSCachedResponse createResponseFrom(File bodyFile, File headersFile) {
        return new WSCachedResponse(bodyFile, headersFile);
    }
    
    public void writeResponseBodyToFile(WSCachedResponse response, File file) throws IOException {
    	response.writeBodyToFile(file);
    }

    public void writeResponseHeadersToFile(WSCachedResponse response, File file) throws IOException {
    	response.writeHeadersToFile(file);
    }
    
    public static File getFileByUrl(String urlStr) {
        File dir = Play.getFile(".httpmock/");
        if(!dir.exists()) dir.mkdirs();
        return new File(dir, Codec.hexMD5(urlStr));
    }
    
    public static File getHeadersFileByUrl(String urlStr) {
        File dir = Play.getFile(".httpmock/");
        if(!dir.exists()) dir.mkdirs();
        return new File(dir, Codec.hexMD5(urlStr)+"_headers");
    }

    public static File getUrlsFile() {
    	File dir = Play.getFile(".httpmock/");
    	dir.mkdirs();
    	return new File(dir, "urls");
    }
    
    /**
     * 
     * @param urls : map of <md5hash, url>
     */
    public static void saveUrls(Map<String, String> urls) {
        BufferedWriter writer = null;
    	try {
			writer = new BufferedWriter(new FileWriter(getUrlsFile()));
			for(String key : urls.keySet()) {
				writer.write(key+" "+urls.get(key));
				writer.newLine();
			}
			retrieveUrls();
		} catch (Exception e) {
            throw new RuntimeException("failed to save urls", e);
		} finally {
            IOUtils.closeQuietly(writer);
        }
    }

	private static Map<String, String> retrieveUrls() {
		Map<String, String> urls = new HashMap<String, String>();
        BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(getUrlsFile()));
			String line = reader.readLine();
			while (line != null && line.length() > 0) {
				int indexOf = line.indexOf(" ");
				if (indexOf != -1) {
					String key = line.substring(0, indexOf);
					String value = line.substring(indexOf + 1);
					File file = Play.getFile(".httpmock/" + key);
					if (file.exists() && file.isFile())
						urls.put(key, value);
				}
				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
            //Ignore
		} catch (IOException e) {
            throw new RuntimeException("failed to retrieve urls", e);
		} finally {
            IOUtils.closeQuietly(reader);
        }
		
		return urls;
    }
    public static Map<String, String> getUrls() {
    	return retrieveUrls();
    }
	public static void removeUrl(String id) {
		Play.getFile(".httpmock/"+id).delete();
        Play.getFile(".httpmock/"+id+"_headers").delete();
		Map<String, String> urls = getUrls();
		urls.remove(id);
		saveUrls(urls);
	}
	public static void removeUrls() {
		File dir = Play.getFile(".httpmock/");
		if(dir.exists()) Files.deleteDirectory(dir);
		retrieveUrls();
	}
    
    public class WSMockRequest extends WSAsyncRequest {
        
        public WSMockRequest(String url, String encoding) {
            super(url, encoding);
        }
        
        private HttpResponse cachedGet() {
            HttpResponse httpResponse = null;
            File responseBodyFile = getFileByUrl(url);
            File responseHeadersFile = getHeadersFileByUrl(url);
            if(responseBodyFile!=null) {
                if(!responseBodyFile.exists()) {
                    if(recordCacheRequests) {
                        httpResponse = super.get();
                        if(httpResponse!=null) {
                        	Logger.debug("WSMockRequest: GET on %s : caching ...", url);
                        	try {
                                writeResponseHeadersToFile(new WSCachedResponse(httpResponse), responseHeadersFile);
								writeResponseBodyToFile(new WSCachedResponse(httpResponse), responseBodyFile);
						        Map<String, String> urls = getUrls();
						        urls.put(responseBodyFile.getName(), url);
						        saveUrls(urls);
							} catch (IOException e) {
								e.printStackTrace();
							}
                        }
                    }
                }
                else if(useCacheRequests) {
                    Logger.debug("WSMockRequest: GET on %s : using cached request", url);
                    httpResponse = createResponseFrom(responseBodyFile, responseHeadersFile);
                }
            }
            if(httpResponse==null)
                httpResponse = super.get();
            return httpResponse;
        }
        
        @Override
        public HttpResponse get() {
            return cachedGet();
        }
        
        @Override
        public Promise<HttpResponse> getAsync() { 
            // fake getAsync to be cached
            return new Promise<HttpResponse>() {
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
        File bodyFile;
        
        public WSCachedResponse(HttpResponse r) {
            headers = r.getHeaders();
            for(Header h : headers)
                headersMap.put(h.name, h.value());
            status = r.getStatus();
            stream = r.getStream();
        }
        
        public WSCachedResponse(File bodyFile, File headersFile) {
            BufferedReader headersFileReader = null;
            try {
                status = 200;
                headers = new ArrayList<Header>();
                headersFileReader = new BufferedReader(new FileReader(headersFile));
                String line = headersFileReader.readLine();
                while(line!=null && line.length()>0) {
                	int indexOf = line.indexOf(": ");
                	if(indexOf != -1) {
                		String key = line.substring(0, indexOf).toLowerCase();
                		String value = line.substring(indexOf+2);
                		headersMap.put(key, value);
                		headers.add(new Header(key, value));
                	}
                	line = headersFileReader.readLine();
                }
                this.bodyFile = bodyFile;
            }
            catch(Exception e) {
                Logger.error("failed to create WSCachedResponse", e);
            }
            finally {
                IOUtils.closeQuietly(headersFileReader);
            }
        }
        
        public void writeBodyToFile(File file) throws IOException {
            IO.copy(stream, new FileOutputStream(file));
        }
        
        public void writeHeadersToFile(File file) throws IOException {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(file));
                for (String key : headersMap.keySet()) {
                    writer.write(key + ": " + headersMap.get(key));
                    writer.newLine();
                }
            } finally {
                writer.close();
            }
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
            if(stream != null) {
                return stream;
            } else {
                try {
                    return new FileInputStream(bodyFile);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("file not found + " + bodyFile.getName(), e);
                }
            }
        }

        @Override
        public String getString() {
            return IO.readContentAsString(stream);
        }
    }
}
