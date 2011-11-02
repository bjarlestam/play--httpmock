package play.modules.httpmock;

import play.Play;
import play.PlayPlugin;
import play.mvc.Router;

public class Plugin extends PlayPlugin {
    
    @Override
    public void onLoad() {
    }
    
    @Override
    public void onConfigurationRead() {
        if (isHttpMockEnabled()) {
            Play.configuration.setProperty("webservice", "play.modules.httpmock.WSMock");
        }
    }
    
    @Override
	public void onRoutesLoaded() {
		if (isHttpMockEnabled()) {
			Router.addRoute("GET", "/@httpmock/?", "httpmock.HttpMock.index");
			Router.addRoute("GET", "/@httpmock/use/{status}", "httpmock.HttpMock.setCacheRequestsUsing");
			Router.addRoute("GET", "/@httpmock/record/{status}", "httpmock.HttpMock.setCacheRequestsRecording");
			Router.addRoute("GET", "/@httpmock/clean/{id}", "httpmock.HttpMock.cleanUrl");
			Router.addRoute("GET", "/@httpmock/clean/all", "httpmock.HttpMock.cleanAllCache");
		}
	}

    private boolean isHttpMockEnabled() {
        if(Play.mode == Play.Mode.PROD) {
            return false;
        }
        String enabled = Play.configuration.getProperty("httpmock.enabled","true");
        return "true".equals(enabled);
    }
}
