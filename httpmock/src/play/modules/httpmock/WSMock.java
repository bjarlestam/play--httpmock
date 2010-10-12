package play.modules.httpmock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import play.Logger;
import play.Play;
import play.libs.IO;
import play.libs.XML;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.libs.ws.WSAsync;
import play.mvc.Http.Header;

public class WSMock extends WSAsync {
    
    /**
     * if true :
     *   if the request is known (cached), don't query the WebService but use the cache
     */
    public static boolean useCacheRequests = true;
    
    /**
     * if true :
     *   if the request is new (not cached), record it in the cache
     */
    public static boolean recordCacheRequests = true;
    
    
    @Override
    public WSRequest newRequest(String url) {
        return new WSMockRequest(url);
    }

    public WSCachedResponse createResponseFrom(File dir) {
        return new WSCachedResponse(dir);
    }
    
    public void writeResponseIntoDir(WSCachedResponse response, File dir) {
        response.writeIntoDir(dir);
    }
    
    
    public static File getDirByUrl(String method, String urlStr) {
        File dir = Play.getFile("httpmock/"+method+"/");
        if(!dir.exists()) dir.mkdirs();
        try {
            URL url = new URL(urlStr);
            File f = new File(dir, url.getHost()+url.getFile()+"/");
            return f;
        }
        catch(MalformedURLException e) {
            Logger.error("unable to parse url (%s)", urlStr);
        }
        catch(IOException e) {
            Logger.error("unable to create new file");
            e.printStackTrace();
        }
        return null;
    }
    
    
    public class WSMockRequest extends WSAsyncRequest {
        
        public WSMockRequest(String url) {
            super(url);
        }
        
        private HttpResponse cachedGet() {
            HttpResponse r = null;
            File f = getDirByUrl("GET", url);
            if(f!=null) {
                if(!f.exists()) {
                    if(recordCacheRequests) {
                        f.mkdirs();
                        Logger.debug("WSMockRequest: GET on %s : caching ...", url);
                        r = super.get();
                        writeResponseIntoDir(new WSCachedResponse(r), f);
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
        
        public WSCachedResponse(File dir) {
            try {
                stream = new FileInputStream(new File(dir, "stream"));
                status = Integer.parseInt(IO.readContentAsString(new FileInputStream(new File(dir, "status"))).trim());
                
                // TODO
                headers = new ArrayList<Header>();
                BufferedReader reader = new BufferedReader(new FileReader(new File(dir, "headers")));
                String line = reader.readLine();
                while(line!=null) {
                	int indexOf = line.indexOf(": ");
                	if(indexOf != -1) {
                		String key = line.substring(0, indexOf);
                		String value = line.substring(indexOf+2);
                		headersMap.put(key, value);
                		headers.add(new Header(key, value));
                	}
                	line = reader.readLine();
                }
                reader.close();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
        }
        
        public WSCachedResponse() {
            
        }
        
        public void writeIntoDir(File dir) {
            try {
                IO.write(stream, new File(dir, "stream"));
                IO.writeContent(""+status, new File(dir, "status"));
                File headersFile = new File(dir, "headers");
                BufferedWriter writer = new BufferedWriter(new FileWriter(headersFile));
                for(String key : headersMap.keySet()) {
                	writer.write(key+": "+headersMap.get(key));
                	writer.newLine();
                }
                writer.close();
            }
            catch(Exception e) {
                e.printStackTrace();
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
            return stream;
        }

        @Override
        public String getString() {
            return IO.readContentAsString(stream);
        }
        
    }
    
}
