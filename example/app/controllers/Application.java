package controllers;

import play.libs.WS;
import play.libs.XML;
import play.mvc.*;

public class Application extends Controller {

    public static void index() {
        renderArgs.put("playframeworkTwitter", WS.url("http://search.twitter.com/search.atom?rpp=7&q=playframework").get().getString());
        render();
    }

}