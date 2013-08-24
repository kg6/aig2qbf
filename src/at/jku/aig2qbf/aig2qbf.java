package at.jku.aig2qbf;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import at.jku.aig2qbf.Util.FileExtension;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.formatter.Formatter;
import at.jku.aig2qbf.parser.Parser;
import at.jku.aig2qbf.reduction.SimplePathReduction;
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
		options.addOption(getCommandlineOption("lit", "list-input-types", "List all supported input type formats.", false, null));
		options.addOption(getCommandlineOption("lot", "list-output-types", "List all supported output type formats.", false, null));
		options.addOption(getCommandlineOption("nr", "no-reduction", "Do not reduce the tree.", false, null));
		options.addOption(getCommandlineOption("nt", "no-tseitin", "Do not apply Tseitin conversion. The output is not necessarily in CNF.", false, null));
		options.addOption(getCommandlineOption("nu", "no-unrolling", "No unrolling will be applied. Implies --no-reduction.", false, null));
		options.addOption(getCommandlineOption("o", "output", "The output file.", true, "FILE"));
		options.addOption(getCommandlineOption("ot", "output-type", "Overwrite the format type of the output file.", true, "TYPE"));
		options.addOption(getCommandlineOption("v", "verbose", "Enable verbose output.", false, null));
		options.addOption(getCommandlineOption("vt", "verbose-times", "Output execution time of different conversion stages.", false, null));
		options.addOption(getCommandlineOption("vis", "visualize", "Visualize the parsed graph data structure after all processing steps were applied.", false, null));
		options.addOption(getCommandlineOption("ws", "with-sanity", "Apply sanity checks.", false, null));

		try {
			CommandLine commandLine = argParser.parse(options, args);

			String input = null;
			File inputFile = null;
			FileExtension inputExtension = null;
			String output = null;
			FileExtension outputExtension = FileExtension.QDIMACS;

			Configuration.SANTIY = commandLine.hasOption("ws");
			Configuration.VERBOSE = commandLine.hasOption("v");
			Configuration.VERBOSETIMES = commandLine.hasOption("vt");

			if (commandLine.hasOption("i")) {
				input = commandLine.getOptionValue("i");
				inputExtension = Util.GetFileExtension((commandLine.hasOption("it")) ? "." + commandLine.getOptionValue("it") : input);

				inputFile = new File(input);

				if (!inputFile.exists()) {
					throw new RuntimeException(String.format("File \"%s\" does not exists.", input));
				}

				if (inputExtension == null) {
					int i = input.lastIndexOf('.');

					throw new RuntimeException(String.format("Unknown extension for input file \"%s\"", (i == -1) ? null : input.substring(i).toLowerCase()));
				}
			}
			if (commandLine.hasOption("o")) {
				output = commandLine.getOptionValue("o");
				outputExtension = Util.GetFileExtension((commandLine.hasOption("ot")) ? "." + commandLine.getOptionValue("ot") : output);

				if (outputExtension == null) {
					int i = output.lastIndexOf('.');

					throw new RuntimeException(String.format("Unknown extension for output file \"%s\"", (i == -1) ? null : output.substring(i).toLowerCase()));
				}
			}
			else if (commandLine.hasOption("ot")) {
				outputExtension = Util.GetFileExtension("." + commandLine.getOptionValue("ot"));
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
				Parser p = Util.GetParser(inputExtension);

				Tree t = null;

				if (Configuration.VERBOSETIMES)
					Util.TimerStart();

				if (inputFile.exists()) {
					t = p.parse(input);
				}
				else {
					t = p.parse(input.getBytes());
				}

				if (Configuration.VERBOSETIMES)
					Util.TimerEnd("TIME parse");

				int k = (commandLine.hasOption("k")) ? Integer.parseInt(commandLine.getOptionValue("k")) : 1;

				if (!commandLine.hasOption("nu")) {
					if (Configuration.VERBOSETIMES)
						Util.TimerStart();

					t = t.unroll(k);
					t.mergeToOneOutput();

					if (Configuration.VERBOSETIMES)
						Util.TimerEnd("TIME unroll");

					if (!commandLine.hasOption("nr")) {
						if (Configuration.VERBOSETIMES)
							Util.TimerStart();

						SimplePathReduction reduction = new SimplePathReduction();
						t = reduction.reduceTree(t, k);

						if (Configuration.VERBOSETIMES)
							Util.TimerEnd("TIME reduce tree");
					}
				}

				if (!commandLine.hasOption("nt")) {
					if (Configuration.VERBOSETIMES)
						Util.TimerStart("TIME START tseitin");

					t = t.toTseitinCNF();

					if (Configuration.VERBOSETIMES)
						Util.TimerEnd("TIME tseitin");
				}

				if (commandLine.hasOption("vis")) {
					TreeVisualizer.CLOSE_ON_EXIT = true;

					TreeVisualizer.DisplayTree(t, input);
				}
				else {
					Formatter f = Util.GetFormatter(outputExtension);

					if (Configuration.VERBOSETIMES)
						Util.TimerStart();

					if (output != null) {
						if (!Util.WriteFile(output, f.format(t))) {
							System.out.println("Unable to write output file.");
						}
					}
					else {
						System.out.println(f.format(t));
					}

					if (Configuration.VERBOSETIMES)
						Util.TimerEnd("TIME formatter");
				}
			}
			else if (commandLine.hasOption("lit")) {
				Object[] list = Util.INPUT_FORMAT_TYPES.keySet().toArray();

				Arrays.sort(list);

				System.out.printf("Supported input format types: %s\n", Util.Join(", ", list));
			}
			else if (commandLine.hasOption("lot")) {
				Object[] list = Util.OUTPUT_FORMAT_TYPES.keySet().toArray();

				Arrays.sort(list);

				System.out.printf("Supported output format types: %s\n", Util.Join(", ", list));
			}
			else { // + h
				HelpFormatter helpFormatter = new HelpFormatter();

				helpFormatter.printHelp("aig2qbf v" + Configuration.VERSION, options);
			}
		}
		catch (ParseException e) {
			System.out.println("Unexpected parse exception " + e.getMessage());
		}
	}
}
