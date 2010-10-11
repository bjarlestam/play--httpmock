package controllers.httpmock;

import play.mvc.Controller;

public class HttpMock extends Controller {
    public static void index() {
        renderText("hello world !");
    }
}
