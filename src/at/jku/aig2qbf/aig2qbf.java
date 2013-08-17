package at.jku.aig2qbf;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import at.jku.aig2qbf.FileIO.FileExtension;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.formatter.Formatter;
import at.jku.aig2qbf.parser.Parser;
import at.jku.aig2qbf.reduction.SimplePathReduction;
import at.jku.aig2qbf.test.BaseTest;
import at.jku.aig2qbf.visualizer.TreeVisualizer;

public class aig2qbf {

	private static Option getCommandlineOption(String opt, String longOpt, String description, boolean hasArg, String argName) {
		Option option = new Option(opt, hasArg, description);

		if (longOpt != null) {
			option.setLongOpt(longOpt);
		}

		if (hasArg && argName != null) {
			option.setArgName(argName);
		}

		return option;
	}

	public static void main(String[] args) {
		CommandLineParser argParser = new PosixParser();

		Options options = new Options();

		options.addOption(getCommandlineOption("h", "help", "Print help.", false, null));
		options.addOption(getCommandlineOption("k", "unroll", "Number of unrolling steps. Default is 1.", true, "INTEGER"));
		options.addOption(getCommandlineOption("i", "input", "The input file.", true, "FILE/AAG STRING"));
		options.addOption(getCommandlineOption("it", "input-type", "Overwrite the format type of the input file.", true, "TYPE"));
		options.addOption(getCommandlineOption("nr", "no-reduction", "Do not reduce the tree.", false, null));
		options.addOption(getCommandlineOption("ns", "no-sanity", "Do not apply any sanity checks.", false, null));
		options.addOption(getCommandlineOption("nt", "no-tseitin", "Do not apply Tseitin conversion. The output is not necessarily in CNF.", false, null));
		options.addOption(getCommandlineOption("nu", "no-unrolling", "No unrolling will be applied. Implies --no-reduction.", false, null));
		options.addOption(getCommandlineOption("o", "output", "The output file.", true, "FILE"));
		options.addOption(getCommandlineOption("ot", "output-type", "Overwrite the format type of the output file.", true, "TYPE"));
		options.addOption(getCommandlineOption("v", "verbose", "Enable verbose output.", false, null));
		options.addOption(getCommandlineOption("vt", "verbose-times", "Enable verbose output.", false, null));
		options.addOption(getCommandlineOption("vis", "visualize", "Visualize the parsed graph data structure after all processing steps were applied.", false, null));

		try {
			CommandLine commandLine = argParser.parse(options, args);

			String input = null;
			File inputFile = null;
			FileExtension inputExtension = null;
			String output = null;
			FileExtension outputExtension = FileExtension.QDIMACS;

			Configuration.FAST = true;
			Configuration.SANTIY = ! commandLine.hasOption("ns");
			Configuration.VERBOSE = commandLine.hasOption("v");
			Configuration.VERBOSETIMES = commandLine.hasOption("vt");
			
			if (commandLine.hasOption("i")) {
				input = commandLine.getOptionValue("i");
				inputExtension = FileIO.GetFileExtension((commandLine.hasOption("it")) ? "." + commandLine.getOptionValue("it") : input);
				
				inputFile = new File(input);
				
				if(!inputFile.exists()) {
					inputExtension = FileExtension.AAG;
				}

				if (inputExtension == null) {
					throw new RuntimeException(String.format("Unknown extension for input file \"%s\"", inputExtension));
				}
			}
			if (commandLine.hasOption("o")) {
				output = commandLine.getOptionValue("o");
				outputExtension = FileIO.GetFileExtension((commandLine.hasOption("ot")) ? "." + commandLine.getOptionValue("ot") : output);

				if (outputExtension == null) {
					throw new RuntimeException(String.format("Unknown extension for output file \"%s\"", output));
				}
			}
			else {
				if (commandLine.hasOption("ot")) {
					outputExtension = FileIO.GetFileExtension("." + commandLine.getOptionValue("ot"));
				}
			}
			if (commandLine.hasOption("k")) {
				try {
					Integer.parseInt(commandLine.getOptionValue("k"));
				}
				catch (NumberFormatException e) {
					throw new RuntimeException("k is not a number");
				}
			}

			if (commandLine.hasOption("i")) {
				Parser p = FileIO.GetParserForFileExtension(inputExtension);

				Tree t = null;

				if (Configuration.VERBOSETIMES) Configuration.timerStart();

				if(inputFile.exists()) {
					t = p.parse(input);
				} else {
					t = p.parse(input.getBytes());
				}

				if (Configuration.VERBOSETIMES) Configuration.timerEnd("TIME parse");

				int k = (commandLine.hasOption("k")) ? Integer.parseInt(commandLine.getOptionValue("k")) : 1;

				if (! commandLine.hasOption("nu")) {
					if (Configuration.VERBOSETIMES) Configuration.timerStart();

					t = t.unroll(k);					
					t.mergeToOneOutput();

					if (Configuration.VERBOSETIMES) Configuration.timerEnd("TIME unroll");

					if (! commandLine.hasOption("nr")) {
						if (Configuration.VERBOSETIMES) Configuration.timerStart();

						SimplePathReduction reduction = new SimplePathReduction();
						t = reduction.reduceTree(t, k);

						if (Configuration.VERBOSETIMES) Configuration.timerEnd("TIME reduce tree");
					}
				}

				if (! commandLine.hasOption("nt")) {
					if (Configuration.VERBOSETIMES) Configuration.timerStart("TIME START tseitin");

					t = t.toTseitinCNF();

					if (Configuration.VERBOSETIMES) Configuration.timerEnd("TIME tseitin");
				}

				if (commandLine.hasOption("vis")) {
					TreeVisualizer.CLOSE_ON_EXIT = true;

					TreeVisualizer.DisplayTree(t, input);
				}
				else {
					Formatter f = BaseTest.GetOutputFileFormatter(outputExtension);

					if (Configuration.VERBOSETIMES) Configuration.timerStart();

					if (output != null) {
						if(! FileIO.WriteFile(output, f.format(t))) {
							System.out.println("Unable to write output file.");
						}
					}
					else {
						System.out.println(f.format(t));
					}

					if (Configuration.VERBOSETIMES) Configuration.timerEnd("TIME formatter");
				}
			}
			else { // + h
				HelpFormatter helpFormatter = new HelpFormatter();

				helpFormatter.printHelp("aig2qbf", options);
			}
		}
		catch (ParseException e) {
			System.out.println("Unexpected parse exception " + e.getMessage());
		}
	}
}
