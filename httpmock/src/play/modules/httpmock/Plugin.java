package play.modules.httpmock;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Router;

public class Plugin extends PlayPlugin {
    
    @Override
    public void onLoad() {
    }
    
    @Override
    public void onConfigurationRead() {
        if (Play.mode == Play.Mode.DEV) {
            Play.configuration.setProperty("webservice", "play.modules.httpmock.WSMock");
        }
    }
    
    @Override
    public void onRoutesLoaded() {
        if (Play.mode == Play.Mode.DEV) {
            Router.addRoute("GET", "/@httpmock/?", "httpmock.HttpMock.index");
            Router.addRoute("GET", "/@httpmock/use/{status}", "httpmock.HttpMock.setCacheRequestsUsing");
            Router.addRoute("GET", "/@httpmock/record/{status}", "httpmock.HttpMock.setCacheRequestsRecording");
            Router.addRoute("GET", "/@httpmock/clean/{id}", "httpmock.HttpMock.cleanUrl");
            Router.addRoute("GET", "/@httpmock/clean/all", "httpmock.HttpMock.cleanAllCache");
            }
    }
}
