package at.jku.aig2qbf.reduction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class SAT {
	private static final String SAT_SOLVER_PATH = "./tools/depqbf";
	
	/**
	 * Check whether a propositional formula in qDimacs format is satisfiable
	 * @param filepath
	 * @return true if the formula is satisfiable, otherwise false
	 */
	public static boolean Solve(String filepath) {
		Process process = null;
		BufferedReader inputStream = null;
		File tempFile = null;
		
		try {
			process = Runtime.getRuntime().exec(SAT_SOLVER_PATH + " "  + filepath);
			inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			String result = inputStream.readLine().toUpperCase();
			
			if(result.compareTo("SAT") == 0) {
				return true;
			} else if (result.compareTo("UNSAT") == 0) {
				return false;
			}
			
			throw new RuntimeException(String.format("SAT solver has returned an unexpected result: %s", result));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(tempFile != null) {
				tempFile.delete();
			}
		}
		
		return false;
	}
}
