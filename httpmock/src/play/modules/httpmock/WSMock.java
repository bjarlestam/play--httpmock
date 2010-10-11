package play.modules.httpmock;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.libs.ws.WSAsync;

public class WSMock extends WSAsync {
    
    @Override
    public WSRequest newRequest(String url) {
        // TODO Auto-generated method stub
        return new WSMockRequest(url);
    }
    
    public class WSMockRequest extends WSAsyncRequest {
        
        public WSMockRequest(String url) {
            super(url);
        }
        
        @Override
        public HttpResponse get() {
            HttpResponse r = super.get();
            return r;
        }
        
        @Override
        public Future<HttpResponse> getAsync() { // fake getAsync
            return new Future<HttpResponse>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public HttpResponse get() throws InterruptedException, ExecutionException {
                    return get();
                }

                @Override
                public HttpResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return get();
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
}
