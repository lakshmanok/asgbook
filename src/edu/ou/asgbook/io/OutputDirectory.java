/**
 * 
 */
package edu.ou.asgbook.io;

import java.io.File;
import java.io.IOException;

/**
 * Change this to change the output directory that is used by all the main()
 * 
 * @author Valliappa.Lakshmanan
 * 
 */
public class OutputDirectory {
	public static File temporary(String prefix)
			throws IOException {

		File out = File.createTempFile(prefix, "_files");
		out.delete();
		out.mkdirs();
		System.out.println("Output will be in " + out);
		return out;

	}

	public static File relative(String prefix)
			throws IOException {
		// current directory
		File out = new File("output/" + prefix + "_files");
		out.delete();
		out.mkdirs();
		System.out.println("Output will be in " + out);
		return out;
	}

	/**
	 * Change this to change the output directory that is used by all the main()
	*/
	public static File getDefault(String prefix) throws IOException {
		// return temporary(prefix);
		return relative(prefix);
	}
}
