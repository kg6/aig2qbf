package at.jku.aig2qbf.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import at.jku.aig2qbf.component.Tree;

public class AIG extends Parser {
	private final String EXPECTED_EXTENSION = "aig";
	
	private final int EXPECTED_HEADER_LENGTH = 6;
	private final int AIG_HEADER_INDEX = 0;
	
	private final int HEADER_M_INDEX = 0;
	private final int HEADER_I_INDEX = 1;
	private final int HEADER_L_INDEX = 2;
	private final int HEADER_O_INDEX = 3;
	private final int HEADER_A_INDEX = 4;
	
	public Tree parse(String filename) {
		File inputFile = this.checkInputFile(filename, EXPECTED_EXTENSION);
		
		List<byte[]> lines = this.readBinaryFile(inputFile);
		
		return this.parse(lines);
	}
	
	private Tree parse(List<byte[]> lines) {
		if (lines.size() <= AIG_HEADER_INDEX) {
			throw new RuntimeException("No header was found in the AIG file.");
		}
		
		final int[] header = parseHeader(new String(lines.get(AIG_HEADER_INDEX)), EXPECTED_EXTENSION, EXPECTED_HEADER_LENGTH);
		final int multipliedMaximumVariableIndex = header[HEADER_M_INDEX] * 2 + 1;
		final int latchesLineOffset = AIG_HEADER_INDEX + 1;
		final int outputsLineOffset = header[HEADER_L_INDEX] + AIG_HEADER_INDEX + 1;
		
		final int[][] fileLatches = parseLatches(lines, header[HEADER_I_INDEX], header[HEADER_L_INDEX], multipliedMaximumVariableIndex, latchesLineOffset);
		final int[] fileOutputs = parseOutputs(lines, header[HEADER_O_INDEX], multipliedMaximumVariableIndex, outputsLineOffset);
		final int[][] fileAnds = parseBinary(lines, header[HEADER_I_INDEX], header[HEADER_L_INDEX], header[HEADER_O_INDEX], header[HEADER_A_INDEX], multipliedMaximumVariableIndex);
		
		return this.createTree(header[HEADER_I_INDEX], header[HEADER_M_INDEX], multipliedMaximumVariableIndex, fileLatches, fileOutputs, fileAnds);
	}
	
	protected int[] parseHeader(final String header, String expectedExtension, int expectedHeaderLength) {
		String[] miloa = header.split("\\s");
		
		if(miloa.length != expectedHeaderLength) {
			throw new RuntimeException("Corrupt header found in AIG file.");
		}
		
		if (miloa[0].compareTo(expectedExtension) != 0) {
			throw new RuntimeException("Corrupt header");
		}
		
		final int headerTagOffset = 1;
		
		return new int[] {
			Integer.parseInt(miloa[HEADER_M_INDEX + headerTagOffset]),
			Integer.parseInt(miloa[HEADER_I_INDEX + headerTagOffset]),
			Integer.parseInt(miloa[HEADER_L_INDEX + headerTagOffset]),
			Integer.parseInt(miloa[HEADER_O_INDEX + headerTagOffset]),
			Integer.parseInt(miloa[HEADER_A_INDEX + headerTagOffset])
		};
	}
	
	private int[][] parseLatches(final List<byte[]> lines, final int numberOfInputs, final int numberOfLatches, final int multipliedMaximumVariableIndex, final int lineOffset) {
		if(lines.size() < lineOffset + numberOfLatches) {
			throw new RuntimeException("Unable to parse the latches of the AIG file.");
		}
		
		int[][] fileLatches = new int[numberOfLatches][];
		
		for(int i = 0; i < numberOfLatches; i++) {
			int current = getLatchIndex(i, numberOfInputs);
			int next = Integer.parseInt(new String(lines.get(i + lineOffset)));
			
			if(current > multipliedMaximumVariableIndex || next > multipliedMaximumVariableIndex) {
				throw new RuntimeException("Header inconsistent with data: There was an index > maximum avariable index.");
			}
			
			fileLatches[i] = new int[] { current, next };
		}
		
		return fileLatches;
	}
	
	private int[] parseOutputs(final List<byte[]> lines, final int numberOfOutputs, final int multipliedMaximumVariableIndex, final int lineOffset) {
		if(lines.size() < lineOffset + numberOfOutputs) {
			throw new RuntimeException("Unable to parse the outputs of the AIG file.");
		}
		
		int[] fileOutputs = new int[numberOfOutputs];
		
		for(int i = 0; i < numberOfOutputs; i++) {
			fileOutputs[i] = Integer.parseInt(new String(lines.get(i + lineOffset)));
			
			if(fileOutputs[i] > multipliedMaximumVariableIndex) {
				throw new RuntimeException("Header inconsistent with data: There was an index > maximum avariable index.");
			}
		}
		
		return fileOutputs;
	}
	
	private int[][] parseBinary(final List<byte[]> lines, final int numberOfInputs, final int numberOfLatches, final int numberOfOutputs, final int numberOfAndGates, final int multipliedMaximumVariableIndex) {
		int[][] fileAnds = new int[numberOfAndGates][];
		
		int lineIndex = numberOfLatches + numberOfOutputs + AIG_HEADER_INDEX + 1;
		if(lineIndex >= lines.size()) {
			return fileAnds;
		}
		
		//Gather all lines that contains the encoding
		final int lineSize = lines.size();
		List<Byte> encodedBytesList = new ArrayList<Byte>();
		
		while(lineIndex < lineSize) {
			byte[] lineBytes = lines.get(lineIndex++);
			
			for(int i = 0; i < lineBytes.length; i++) {
				encodedBytesList.add(lineBytes[i]);
			}
			
			encodedBytesList.add((byte)'\n');
		}
		
		//Decode the line and extract its information
		Byte[] encodedBytes = new Byte[encodedBytesList.size()];
		encodedBytes = encodedBytesList.toArray(encodedBytes);
		
		List<Integer> decodedLine = decodeBinary(encodedBytes);
		
		int andCounter = 0;
		int delta0, delta1, lhs, rhs0, rhs1;
		for(int i = 0; i < decodedLine.size() - 1; i += 2) {
			delta0 = decodedLine.get(i);
			delta1 = decodedLine.get(i + 1);
			
			lhs = getAndIndex(andCounter, numberOfInputs, numberOfLatches);
			rhs0 = lhs - delta0;
			rhs1 = rhs0 - delta1;
			
			if(lhs <= rhs0 || lhs <= rhs1 || rhs0 < rhs1 || lhs < 0 || rhs0 < 0 || rhs1 < 0) {
				throw new RuntimeException("Unable to parse AIG file: Indizes in the AIG file are not consecutive!");
			}
			
			fileAnds[andCounter] = new int[] { 
				lhs, 
				rhs0, 
				rhs1 
			};
			
			andCounter++;
			
			if(lhs > multipliedMaximumVariableIndex || rhs0 > multipliedMaximumVariableIndex || rhs1 > multipliedMaximumVariableIndex) {
				throw new RuntimeException("Header inconsistent with data: There was an index > maximum avariable index.");
			}
			
			if(andCounter >= numberOfAndGates) {
				break;
			}
		}
		
		if(andCounter != numberOfAndGates){
			throw new RuntimeException("Header inconsistent with data: The number of AND gates is different to the number specified in the header.");
		}
		
		return fileAnds;
	}
	
	private List<Integer> decodeBinary(Byte[] lineBytes) {
		List<Integer> resultList = new ArrayList<Integer>();
		
		int sum = 0;
		int byteOffsetCounter = 0;
		
		boolean checkNextByte = true;
		
		for(int i = 0; i < lineBytes.length; i++) {
			checkNextByte = (lineBytes[i] & 0x80) != 0;
			final int byteValue = (int)(lineBytes[i] & 0x7F);
			
			sum += Math.pow(2, 7 * byteOffsetCounter) * byteValue;
			
			if(!checkNextByte) {
				resultList.add(sum);
				
				byteOffsetCounter = 0;
				sum = 0;
			} else {
				byteOffsetCounter++;
			}
		}
		
		return resultList;
	}
	
	private int getLatchIndex(final int index, final int numberOfInputs) {
		return 2 * (numberOfInputs + index + 1);
	}
	
	private int getAndIndex(final int index, final int numberOfInputs, final int numberOfLatches) {
		return 2 * (numberOfInputs + numberOfLatches + index + 1);
	}
}
