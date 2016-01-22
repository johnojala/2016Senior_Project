

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileResource {
	
	public static String requestResource(String file) throws IOException { 
		Path resourcePath;
		Path reqPath = Paths.get(file).toAbsolutePath();
		System.out.printf("Loading resource file: %s...", file);
		//try {
			resourcePath = reqPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
		/*} catch (IOException e) {
			System.out.println("Failure.");
			System.out.printf("File %s was not found.\n", reqPath);
			e.printStackTrace();
			return null;
		}*/
		System.out.println("Success.");
		System.out.printf("\t%s\n", resourcePath.toString());
		return resourcePath.toString();
	}
}
