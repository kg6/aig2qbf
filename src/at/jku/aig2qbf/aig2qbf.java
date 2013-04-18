package at.jku.aig2qbf;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.formatter.Formatter;
import at.jku.aig2qbf.formatter.QDIMACS;
import at.jku.aig2qbf.parser.Parser;
import at.jku.aig2qbf.parser.Parser.Extension;
import at.jku.aig2qbf.reduction.SimplePathReduction;
import at.jku.aig2qbf.visualizer.TreeVisualizer;

public class aig2qbf {
	
	private static Option getCommandlineOption(String opt, String longOpt, String description, boolean hasArg, String argName) {
		Option option = new Option(opt, hasArg, description);
		
		if(longOpt != null) {
			option.setLongOpt(longOpt);
		}
		
		if(hasArg && argName != null) {
			option.setArgName(argName);
		}
		
		return option;
	}
	
	public static void main(String[] args) {
		CommandLineParser argParser = new PosixParser();
		
		Options options = new Options();
		
		options.addOption(getCommandlineOption("h", "help", "Print help.", false, null));
		options.addOption(getCommandlineOption("k", "unroll", "The unroll count k. Default is 1.", true, "INTEGER"));
		options.addOption(getCommandlineOption("i", "input", "The input file.", true, "FILE"));
		options.addOption(getCommandlineOption("noreduction", null, "Do not reduce the tree.", false, null));
		options.addOption(getCommandlineOption("o", "output", "The output file.", true, "FILE"));
		options.addOption(getCommandlineOption("visualize", null, "Visualize the tree before the QBF format.", false, null));

		try {
			CommandLine commandLine = argParser.parse(options, args);
			
			String input = null;
			Extension inputExtension = null;
			String output = null;
			Extension outputExtension = Extension.QDIMACS;

			if (commandLine.hasOption('i')) {
				input = commandLine.getOptionValue('i');
				inputExtension = Parser.getExtension(input);
				
				if (inputExtension == null) {
					throw new RuntimeException(String.format("Unknown extension for input file \"%s\"", inputExtension));
				}
			}
			if (commandLine.hasOption('o')) {
				output = commandLine.getOptionValue('o');
				outputExtension = Parser.getExtension(output);
				
				if (outputExtension == null) {
					throw new RuntimeException(String.format("Unknown extension for output file \"%s\"", output));
				}
			}
			if (commandLine.hasOption('k')) {
				try {
					Integer.parseInt(commandLine.getOptionValue('k'));
				}
				catch (NumberFormatException e) {
					throw new RuntimeException("k is not a number");
				}
			}
			
			if (commandLine.hasOption('i')) {
				Parser p = null;
				
				switch (inputExtension) {
					case AAG:
						p = new at.jku.aig2qbf.parser.AAG();
					break;
					case AIG:
						p = new at.jku.aig2qbf.parser.AIG();	
					break;
					default:
						throw new RuntimeException(String.format("Parser for input file \"%s\" not implemented", input));
				}
				
				Tree t = p.parse(input);
				
				int k = (commandLine.hasOption('k')) ? Integer.parseInt(commandLine.getOptionValue('k')) : 1;
				
				t = t.unroll(k);
				t.mergeToOneOutput();

				if (! commandLine.hasOption("noreduction")) {
					SimplePathReduction reduction = new SimplePathReduction();
					t = reduction.reduceTree(t, k);
				}
				
				if (commandLine.hasOption("visualize")) {
					TreeVisualizer.DisplayTree(t, input);
				}
				else {
					Formatter f = new QDIMACS();
					
					switch (outputExtension) {
						case AAG:
							f = new at.jku.aig2qbf.formatter.AAG();
						break;
						case QDIMACS:
							f = new at.jku.aig2qbf.formatter.QDIMACS();
						break;
						default:
							throw new RuntimeException(String.format("Formatter for output file \"%s\" not implemented", output));
					}
					
					if (output != null) {
						f.writeToFile(t, output);
					}
					else {
						System.out.println(f.format(t));
					}
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
