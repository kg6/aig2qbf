package at.jku.aig2qbf.parser;

import java.io.File;

import at.jku.aig2qbf.component.Tree;

public class AAG extends Parser {
	private final String EXPECTED_EXTENSION = "aag";

	private final int EXPECTED_HEADER_LENGTH = 6;
	private final int AIG_HEADER_INDEX = 0;

	private final int HEADER_M_INDEX = 0;
	private final int HEADER_I_INDEX = 1;
	private final int HEADER_L_INDEX = 2;
	private final int HEADER_O_INDEX = 3;
	private final int HEADER_A_INDEX = 4;

	@Override
	public Tree parse(String filename) {
		File inputFile = this.checkInputFile(filename, EXPECTED_EXTENSION);

		String[] lines = this.readFile(inputFile);

		return this.parse(lines);
	}

	public Tree parse(String[] lines) {
		if (lines.length <= AIG_HEADER_INDEX) {
			throw new RuntimeException("No header was found in the AAG file.");
		}

		final int[] header = parseHeader(lines[AIG_HEADER_INDEX], EXPECTED_EXTENSION, EXPECTED_HEADER_LENGTH);
		final int multipliedMaximumVariableIndex = header[HEADER_M_INDEX] * 2 + 1;
		final int latchesLineOffset = header[HEADER_I_INDEX] + AIG_HEADER_INDEX + 1;
		final int outputsLineOffset = header[HEADER_I_INDEX] + header[HEADER_L_INDEX] + AIG_HEADER_INDEX + 1;

		final int[][] fileLatches = parseLatches(lines, header[HEADER_L_INDEX], multipliedMaximumVariableIndex, latchesLineOffset);
		final int[] fileOutputs = parseOutputs(lines, header[HEADER_O_INDEX], multipliedMaximumVariableIndex, outputsLineOffset);
		final int[][] fileAnds = parseAnds(lines, header[HEADER_I_INDEX], header[HEADER_L_INDEX], header[HEADER_O_INDEX], header[HEADER_A_INDEX], multipliedMaximumVariableIndex);

		return createTree(header[HEADER_I_INDEX], header[HEADER_M_INDEX], multipliedMaximumVariableIndex, fileLatches, fileOutputs, fileAnds);
	}

	protected int[] parseHeader(final String header, String expectedExtension, int expectedHeaderLength) {
		String[] miloa = header.split("\\s");

		if (miloa.length != expectedHeaderLength) {
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

	private int[][] parseLatches(final String[] lines, final int numberOfLatches, final int multipliedMaximumVariableIndex, final int lineOffset) {
		if (lines.length < lineOffset + numberOfLatches) {
			throw new RuntimeException("Unable to parse the latches of the AAG file.");
		}

		int[][] fileLatches = new int[numberOfLatches][];

		for (int i = 0; i < numberOfLatches; i++) {
			String[] tmp = lines[i + lineOffset].split("\\s");

			if (tmp.length != 2) {
				throw new RuntimeException("Invalid AAG file format: Unable to parse latches.");
			}

			fileLatches[i] = new int[] {
				Integer.parseInt(tmp[0]),
				Integer.parseInt(tmp[1])
			};

			if (fileLatches[i][0] > multipliedMaximumVariableIndex || fileLatches[i][1] > multipliedMaximumVariableIndex) {
				throw new RuntimeException("Header inconsistent with data: There was an index > maximum avariable index.");
			}
		}

		return fileLatches;
	}

	private int[] parseOutputs(final String[] lines, final int numberOfOutputs, final int multipliedMaximumVariableIndex, final int lineOffset) {
		if (lines.length < lineOffset + numberOfOutputs) {
			throw new RuntimeException("Unable to parse the outputs of the AIG file.");
		}

		int[] fileOutputs = new int[numberOfOutputs];

		for (int i = 0; i < numberOfOutputs; i++) {
			fileOutputs[i] = Integer.parseInt(lines[i + lineOffset]);

			if (fileOutputs[i] > multipliedMaximumVariableIndex) {
				throw new RuntimeException("Header inconsistent with data: There was an index > maximum avariable index.");
			}
		}

		return fileOutputs;
	}

	private int[][] parseAnds(final String[] lines, final int numberOfInputs, final int numberOfLatches, final int numberOfOutputs, final int numberOfAndGates, final int multipliedMaximumVariableIndex) {
		int[][] fileAnds = new int[numberOfAndGates][];

		int lineIndex = numberOfInputs + numberOfLatches + numberOfOutputs + AIG_HEADER_INDEX + 1;

		if (lineIndex >= lines.length) {
			return fileAnds;
		}

		int andCounter = 0;

		while (andCounter + lineIndex < lines.length) {
			String[] tmp = lines[andCounter + lineIndex].split("\\s");

			if (tmp.length != 3) {
				throw new RuntimeException("Invalid AAG file format: Unable to parse AND gates.");
			}

			int lhs = Integer.parseInt(tmp[0]);
			int rhs0 = Integer.parseInt(tmp[1]);
			int rhs1 = Integer.parseInt(tmp[2]);

			fileAnds[andCounter] = new int[] {
				lhs,
				rhs0,
				rhs1
			};

			andCounter++;

			if (lhs > multipliedMaximumVariableIndex || rhs0 > multipliedMaximumVariableIndex || rhs1 > multipliedMaximumVariableIndex) {
				throw new RuntimeException("Header inconsistent with data: There was an index > maximum avariable index.");
			}

			if (andCounter >= numberOfAndGates) {
				break;
			}
		}

		if (andCounter != numberOfAndGates) {
			throw new RuntimeException("Header inconsistent with data: The number of AND gates is different to the number specified in the header.");
		}

		return fileAnds;
	}
}
