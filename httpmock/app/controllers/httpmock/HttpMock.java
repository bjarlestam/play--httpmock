package controllers.httpmock;

import java.util.Map;
import play.modules.httpmock.WSMock;
import play.mvc.Controller;

public class HttpMock extends Controller {

	public static void index() {
    	Map<String, String> allGetUrls = WSMock.getUrls();
    	boolean useCacheRequests = WSMock.useCacheRequests;
    	boolean recordCacheRequests = WSMock.recordCacheRequests;
        render(allGetUrls, useCacheRequests, recordCacheRequests);
    }

	public static void setCacheRequestsUsing(boolean status) {
		WSMock.useCacheRequests = status;
		index();
	}
	public static void setCacheRequestsRecording(boolean status) {
		WSMock.recordCacheRequests = status;
		index();
	}
	
	public static void cleanUrl(String id) {
		WSMock.removeUrl(id);
		index();
	}
	
	public static void cleanAllCache() {
		WSMock.removeUrls();
		index();
	}
	
}
