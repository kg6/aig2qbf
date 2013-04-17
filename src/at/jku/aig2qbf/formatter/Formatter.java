package at.jku.aig2qbf.formatter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import at.jku.aig2qbf.component.Tree;

public abstract class Formatter {
	abstract public String format(Tree tree);
	
	public boolean writeToFile (Tree tree, String filepath) {
		String out = this.format(tree);
		
		//Write to file
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filepath));
			writer.write(out);
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					
				}
			}
		}
		
		return false;
	}
}
