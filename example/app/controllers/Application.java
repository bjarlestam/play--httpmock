package controllers;

import play.libs.WS;
import play.libs.XML;
import play.mvc.*;

public class Application extends Controller {

	private final static String[] searchs = { "playframework", "java", "RESTful", "web", "HTML5", "stateless" };
	
    public static void index() {
    	String search = searchs[(int)(Math.random()*searchs.length)];
        String twitter = WS.url("http://search.twitter.com/search.atom?rpp=7&q="+search).get().getString();
        render(search, twitter);
    }

}