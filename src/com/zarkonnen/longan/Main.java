package com.zarkonnen.longan;

/*
 * Copyright 2011 David Stark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.simple.SimpleWordPlaintextConverter;
import com.zarkonnen.longan.stage.ResultConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
	static final String INVOCATION = "java -Xmx512m -jar longan.jar [OPTIONS] [INPUT FILE]";
	
	static final ResultConverter<String> DEFAULT_FORMAT = new SimpleWordPlaintextConverter();
	static final HashMap<String, ResultConverter> FORMATS = new HashMap<String, ResultConverter>();
	static {
		FORMATS.put("plaintext", DEFAULT_FORMAT);
		FORMATS.put("visualize", new Visualizer());
	}
	
    public static void main(String[] args) throws IOException {
		// Use Apache Commons CLI (packaged into the Jar) to parse command line options.
		Options options = new Options();
		Option helpO = OptionBuilder.withDescription("print help").create("h");
		Option versionO = OptionBuilder.withDescription("print version").create("v");
		Option outputO = OptionBuilder.withDescription("output file").withLongOpt("out").hasArg().withArgName("file").create("o");
		Option formatO = OptionBuilder.withDescription("output format: one of plaintext (default) and visualize (debug output in png)").hasArg().withArgName("format").withLongOpt("format").create();
		Option serverO = OptionBuilder.withDescription("launches server mode: Server mode reads " +
				"command line strings one per line exactly as above. If no output file is " +
				"specified, returns a line containing the number of output lines before the " +
				"output. If there is an error, returns a single line with the error message. " +
				"Shut down server by sending \"quit\".").withLongOpt("server").create();
		Option openCLO = OptionBuilder.withDescription("enables use of the graphics card to "
				+ "support the OCR system. Defaults to true.").withLongOpt("enable-opencl").
				hasArg().withArgName("enabled").create();
		options.addOption(helpO);
		options.addOption(versionO);
		options.addOption(outputO);
		options.addOption(formatO);
		options.addOption(serverO);
		options.addOption(openCLO);
		CommandLineParser clp = new GnuParser();
		try {
			CommandLine line = clp.parse(options, args);
			if (line.hasOption("h")) {
				new HelpFormatter().printHelp(INVOCATION, options);
				System.exit(0);
			}
			if (line.hasOption("v")) {
				System.out.println(Longan.VERSION);
				System.exit(0);
			}
			boolean enableOpenCL = true;
			if (line.hasOption("enable-opencl")) {
				enableOpenCL =
						line.getOptionValue("enable-opencl").toLowerCase().equals("true") ||
						line.getOptionValue("enable-opencl").equals("1");
			}
			if (line.hasOption("server")) {
				Longan longan = Longan.getDefaultImplementation(enableOpenCL);
				BufferedReader inputR = new BufferedReader(new InputStreamReader(System.in));
				while (true) {
					String input = inputR.readLine();
					if (input.trim().equals("quit")) {
						return;
					}
					String[] args2 = splitInput(input);
					Options o2 = new Options();
					o2.addOption(outputO);
					o2.addOption(formatO);
					try {
						line = clp.parse(o2, args2);
						
						File outFile = null;
						if (line.hasOption("o")) {
							outFile = new File(line.getOptionValue("o"));
						}
						
						ResultConverter format = FORMATS.get(line.getOptionValue("format", "plaintext"));
						if (format != DEFAULT_FORMAT && outFile == null) {
							System.out.println("You must specify an output file for non-plaintext output.");
							continue;
						}
						
						if (line.getArgList().isEmpty()) {
							System.out.println("Please specify an input image.");
							continue;
						}
						if (line.getArgList().size() > 1) {
							System.err.println("Please specify one input image at a time");
							continue;
						}
						
						File inFile = new File((String) line.getArgList().get(0));

						if (!inFile.exists()) {
							System.out.println("The input image does not exist.");
							continue;
						}

						try {
							Result result = longan.process(ImageIO.read(inFile));
							if (outFile == null) {
								String txt = DEFAULT_FORMAT.convert(result);
								System.out.println(numNewlines(txt) + 1);
								System.out.print(txt);
							} else {
								if (outFile.getAbsoluteFile().getParentFile() != null &&
									!outFile.getAbsoluteFile().getParentFile().exists())
								{
									outFile.getParentFile().mkdirs();
								}
								FileOutputStream fos = new FileOutputStream(outFile);
								try {
									format.write(result, fos);
								} finally {
									fos.close();
								}
							}
						} catch (Exception e) {
							System.out.println("Processing error: " + exception(e));
						}
					} catch (ParseException e) {
						System.out.println("Input not recognized: " + exception(e));
					}
				} // End server loop
			} else {
				// Single invocation
				File outFile = null;
				if (line.hasOption("o")) {
					outFile = new File(line.getOptionValue("o"));
				}
				
				ResultConverter format = FORMATS.get(line.getOptionValue("format", "plaintext"));
				if (format != DEFAULT_FORMAT && outFile == null) {
					System.err.println("You must specify an output file for non-plaintext output.");
					System.exit(1);
				}
				
				if (line.getArgList().isEmpty()) {
					System.err.println("Please specify an input image.");
					new HelpFormatter().printHelp(INVOCATION, options);
					System.exit(1);
				}
				if (line.getArgList().size() > 1) {
					System.err.println("Please specify one input image only. To process multiple " +
							"images, use server mode.");
					System.exit(1);
				}
				File inFile = new File((String) line.getArgList().get(0));
				
				if (!inFile.exists()) {
					System.err.println("The input image does not exist.");
					System.exit(1);
				}
				
				try {
					Result result = Longan.getDefaultImplementation(enableOpenCL).process(ImageIO.read(inFile));
					if (outFile == null) {
						String txt = DEFAULT_FORMAT.convert(result);
						System.out.print(txt);
					} else {
						if (outFile.getAbsoluteFile().getParentFile() != null &&
							!outFile.getAbsoluteFile().getParentFile().exists())
						{
							outFile.getParentFile().mkdirs();
						}
						FileOutputStream fos = new FileOutputStream(outFile);
						try {
							format.write(format.convert(result), fos);
						} finally {
							fos.close();
						}
					}
				} catch (Exception e) {
					System.err.println("Processing error: " + exception(e));
					System.exit(1);
				}
			}
		} catch (ParseException e) {
			System.err.println("Parsing command line input failed: " + exception(e));
			System.exit(1);
		}
	}
	
	static int numNewlines(String str) {
		int n = 0;
		for (int i = 0; i < str.length(); i++) {
			n += str.charAt(i) == '\n' ? 1 : 0;
		}
		return n;
	}
	
	/** Splits an input string in the same way as the shell does. */
    static String[] splitInput(String input) {
		StringBuilder b = new StringBuilder();
		ArrayList<String> args = new ArrayList<String>();
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean escaped = false;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (escaped) {
				b.append(c);
				escaped = false;
			} else if ((singleQuoted && c != '\'') || (doubleQuoted && c == '\'')) {
				b.append(c);
			} else {
				switch (c) {
					case '\\': escaped = true; break;
					case '\"': doubleQuoted = !doubleQuoted; break;
					case '\'': singleQuoted = !singleQuoted; break;
					case ' ':
						if (singleQuoted || doubleQuoted) {
							b.append(c);
						} else {
							if (b.length() > 0) {
								args.add(b.toString());
								b = new StringBuilder();
							}
						}
						break;
					default:
						b.append(c);
						break;
				}
			}
		}
		if (doubleQuoted) {
			throw new RuntimeException("Mismatched quotes in input.");
		}
		if (b.length() > 0) {
			args.add(b.toString());
		}
		return args.toArray(new String[args.size()]);
    }
	
	static String exception(Exception e) {
		e.printStackTrace();
		return e.getMessage() == null ? e.getClass().getName() : e.getMessage().replace("\n", " ").replace("\r", " ");
	}
}
