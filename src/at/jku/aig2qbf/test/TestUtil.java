package at.jku.aig2qbf.test;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.formatter.QDIMACS;
import at.jku.aig2qbf.reduction.SAT;

public class TestUtil {
	
	public static boolean CheckSatisfiablity(Tree tree, String outputFilePath) {
		QDIMACS q = new QDIMACS();

		if(! q.writeToFile(tree, outputFilePath)) {
			fail("Unable to write SAT temporary file");
			return false;
		}
		
		return SAT.Solve(outputFilePath);
	}
	
	public static String ReadQDIMACSFile(String inputFilePath) {
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath)));
			
			StringBuilder builder = new StringBuilder();
			
			String line = "";
			while((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
			}
			
			return builder.toString();
		} catch (Exception e) {
			fail(e.toString());
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					
				}
			}
		}
		
		return "";
	}
	
	public static void RemoveOutputFile(String outputFilePath) {
		File tempFile = new File(outputFilePath);
		
		if(tempFile.exists()) {
			tempFile.delete();
		}
	}
}
