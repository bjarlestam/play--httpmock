package controllers;

import play.libs.WS;
import play.mvc.*;

public class Application extends Controller {

	private final static String[] searchs = { "playframework", "java", "RESTful", "web", "HTML5", "stateless" };
	
    public static void index() {
    	String search = searchs[(int)(Math.random()*searchs.length)];
        String twitter = WS.url("http://search.twitter.com/search.atom?rpp=7&q="+search).get().getString();
        render(search, twitter);
    }

    public static void image() {
        WS.HttpResponse response = WS.url("http://www.google.se/images/logo.png").get();
        renderBinary(response.getStream(), "logo.png", response.getContentType(), true);
    }
}