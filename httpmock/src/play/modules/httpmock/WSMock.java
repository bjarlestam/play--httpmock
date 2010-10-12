package play.modules.httpmock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    
    
    public static File getDirByUrl(String urlStr) {
        File dir = Play.getFile("httpmock/GET/");
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
            File f = getDirByUrl(url);
            if(f!=null) {
                if(!f.exists()) {
                    f.mkdirs();
                    Logger.debug("WSMockRequest: GET on %s : caching ...", url);
                    r = super.get();
                    writeResponseIntoDir(new WSCachedResponse(r), f);
                }
                else {
                    Logger.debug("WSMockRequest: GET on %s : using cached request", url);
                    r = createResponseFrom(f);
                }
            }
            else {
                r = super.get();
            }
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
        String asString;
        
        public WSCachedResponse(HttpResponse r) {
            headers = r.getHeaders();
            for(Header h : headers)
                headersMap.put(h.name, h.value());
            status = r.getStatus();
            stream = r.getStream();
            asString = r.getString();
        }
        
        public WSCachedResponse(File dir) {
            try {
                stream = new FileInputStream(new File(dir, "stream"));
                asString = IO.readContentAsString(stream);
                
                // TODO
                headers = new ArrayList<Header>();
                status = 200;
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
            return asString;
        }
        
    }
    
}
