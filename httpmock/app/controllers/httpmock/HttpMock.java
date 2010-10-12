package controllers.httpmock;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import play.Play;
import play.modules.httpmock.WSMock;
import play.mvc.Controller;

public class HttpMock extends Controller {
    
	static private List<String> getAllUrls(File dir) {
		try {
			return getAllUrls(dir, "http:/");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}
	static private List<String> getAllUrls(File dir, String relativePath) throws FileNotFoundException {
		List<String> result = new ArrayList<String>();
		if(dir.exists()) {
			File[] filesAndDirs = dir.listFiles();
			List<File> filesDirs = Arrays.asList(filesAndDirs);
			for (File file : filesDirs) {
				if (!file.isFile()) {
					String path = relativePath + "/" + file.getName();
					if (new File(file, "stream").exists())
						result.add(path);
					else {
						List<String> deeperList = getAllUrls(file, path);
						result.addAll(deeperList);
					}
				}
			}
		}
		return result;
	}

	static private boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (File f : files) {
				if (f.isDirectory())
					deleteDirectory(f);
				else
					f.delete();
			}
		}
		return (path.delete());
	}


	public static void index() {
    	File dir = Play.getFile("httpmock/GET/");
    	List<String> allGetUrls = getAllUrls(dir);
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
	
	public static void cleanAllCache() {
		deleteDirectory(Play.getFile("httpmock/GET/"));
		index();
	}
	
}
