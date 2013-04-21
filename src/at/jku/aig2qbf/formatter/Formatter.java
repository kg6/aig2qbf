package at.jku.aig2qbf.formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import at.jku.aig2qbf.component.Tree;

public abstract class Formatter {
	abstract public String format(Tree tree);

	public boolean writeToFile(Tree tree, String filepath) {
		File outputFile = new File(filepath);

		if (outputFile.exists()) {
			outputFile.delete();
		}

		File parentDirectory = outputFile.getParentFile();

		if (parentDirectory == null || ! parentDirectory.exists()) {
			parentDirectory.mkdirs();
		}

		// Write to file
		String out = this.format(tree);

		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(filepath));
			writer.write(out);

			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (writer != null) {
				try {
					writer.close();
				}
				catch (IOException e) {

				}
			}
		}

		return false;
	}
}
