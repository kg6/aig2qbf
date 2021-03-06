package at.jku.aig2qbf.parser;

import at.jku.aig2qbf.Util;
import at.jku.aig2qbf.component.Tree;

public class AAG extends AIG {
	protected final String EXPECTED_EXTENSION = "aag";

	@Override
	public Tree parse(String inputFilePath) {
		this.checkInputFile(inputFilePath, EXPECTED_EXTENSION);

		String[] lines = Util.ReadFile(inputFilePath).split("\n");

		return this.parse(lines);
	}

	@Override
	public Tree parse(byte[] input) {
		String[] lines = new String(input).split("\n");

		return this.parse(lines);
	}

	private Tree parse(String[] lines) {
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

		if (numberOfAndGates == 0) {
			return fileAnds;
		}

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
