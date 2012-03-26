package com.zarkonnen.longan.nnidentifier;

import java.util.Random;
import com.zarkonnen.longan.nnidentifier.network.Network;
import java.awt.Rectangle;
import com.zarkonnen.longan.data.Letter;
import java.awt.Graphics2D;
import java.util.List;
import com.zarkonnen.fruitbat.atrio.ATRReader;
import com.zarkonnen.longan.Longan;
import com.zarkonnen.longan.better.BetterLetterFinder;
import com.zarkonnen.longan.nnidentifier.network.Util;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import static com.zarkonnen.longan.nnidentifier.network.Util.*;

public class ProfileGen {
	static final String INVOCATION = "java -jar profilegen.jar [OPTIONS] [INPUT FILE(S)]";
	static final int DEFAULT_PASSES = 60000;
	static final int OUTPUT_SIZE = 128;
	static final float N = 0.002f;
	static final float M = 0.0005f;
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, JSONException, IOException, NoSuchAlgorithmException, ParseException {
		Options options = new Options();
		Option helpO = OptionBuilder.withDescription("print help").create("h");
		Option versionO = OptionBuilder.withDescription("print version").create("v");
		Option generateO = OptionBuilder.withDescription("Generate set of neural network weights. Takes two file arguments: the source JSON file and the target zip.").withLongOpt("generate").create("g");
		Option testO = OptionBuilder.withDescription("Test set of neural networks against images. Takes two file arguments: the zip of weights and a folder of images.").withLongOpt("test").create("t");
		Option itersO = OptionBuilder.withDescription("Number of iterations to train the network(s) for.").withLongOpt("iters").hasArg().withArgName("iterations").create("i");
		Option seedO = OptionBuilder.withDescription("Seed to use for testing.").withLongOpt("seed").hasArg().withArgName("seed").create("s");
		options.addOption(helpO);
		options.addOption(versionO);
		options.addOption(generateO);
		options.addOption(testO);
		options.addOption(itersO);
		options.addOption(seedO);
		CommandLineParser clp = new GnuParser();
		try {
			CommandLine line = clp.parse(options, args);
			long seed = System.currentTimeMillis();
			if (line.hasOption("h")) {
				new HelpFormatter().printHelp(INVOCATION, options);
				System.exit(0);
			}
			if (line.hasOption("v")) {
				System.out.println(Longan.VERSION);
				System.exit(0);
			}
			if (line.hasOption("s")) {
				seed = Long.parseLong(line.getOptionValue("s", seed + ""));
			}
			if (line.hasOption("g")) {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File((String) line.getArgList().get(0))), "UTF-8"));
				Config config = new Config(new JSONObject(new JSONTokener(br)));
				generate(config, Integer.parseInt(line.getOptionValue("i", DEFAULT_PASSES + "")));
				NetworkIO.writeNetworks(config, new File((String) line.getArgList().get(1)));
				System.exit(0);
			}
			if (line.hasOption("t")) {
				long t = System.currentTimeMillis();
				Config config = NetworkIO.readArchive(new File((String) line.getArgList().get(0)));
				if (line.getArgList().size() == 1) {
					testNetworks(config, seed);
				} else {
					testNetworks(config, new File((String) line.getArgList().get(1)));
				}
				System.out.println((System.currentTimeMillis() - t) + " ms");
				System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	static int qq;
	public static void testNetworks(Config config, File imageFolder) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		for (Config.Identifier identifier : config.identifiers) {
			try {
				System.out.println();
				System.out.println("Testing " + identifier);
				int misses = 0;
				int tests = 0;
				Counter<Config.LetterClass> testCounts = new Counter<Config.LetterClass>();
				Counter<ArrayList<Config.LetterClass>> errCounts = new Counter<ArrayList<Config.LetterClass>>();
				for (Config.LetterClass lc : identifier.classes) {
					for (String l : lc.members) {
						File letterFolder = new File(imageFolder, letterToFilename(l));
						if (!letterFolder.exists()) {
							System.out.println("(No test data for letter \"" + l + "\".)");
							continue;
						}
						File fontFile = new File(imageFolder, letterToFilename(l) + "-font.atr");
						HashSet<String> doNotTest = new HashSet<String>();
						if (!fontFile.exists()) {
							System.out.println("(No font file \"" + letterToFilename(l) + "-font.atr\" found.)");
						} else {
							ArrayList<String> fontNames = new ArrayList<String>();
							for (Config.FontType ft : identifier.fonts) {
								fontNames.add(ft.font);
							}
							ATRReader r = new ATRReader(new BufferedInputStream(new FileInputStream(fontFile)));
							List<String> line = null;
							while ((line = r.readRecord()) != null) {
								if (!fontNames.contains(line.get(1))) {
									doNotTest.add(line.get(0));
								}
							}
						}
						for (File f : letterFolder.listFiles()) {
							if (f.getName().endsWith(".png") && !doNotTest.contains(f.getName().substring(0, f.getName().length() - 4))) {
								tests++;
								testCounts.increment(lc);
								if (runTest(identifier, ImageIO.read(f), lc, f.getName(), l, errCounts)) { misses++; }
							}
						}
					}
				}
				System.out.println((tests - misses) + "/" + tests);
				System.out.println(100.0 * (tests - misses) / tests + "%");
				ArrayList<Map.Entry<ArrayList<Config.LetterClass>, Integer>> errL = errCounts.sortedCountsHighestFirst();
				System.out.println("Error summary:");
				for (Map.Entry<ArrayList<Config.LetterClass>, Integer> e : errL) {
					double err = e.getValue() * 100.0 / testCounts.counts.get(e.getKey().get(0));
					System.out.println(e.getKey().get(0) + " -> " + e.getKey().get(1) + ": " + err + "%");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void testNetworks(Config config, long seed) {
		Random r = new Random(seed);
		for (Config.Identifier identifier : config.identifiers) {
			try {
				System.out.println();
				System.out.println("Testing " + identifier + " against synthetic letters");
				int misses = 0;
				int tests = 0;
				Counter<ArrayList<Config.LetterClass>> errCounts = new Counter<ArrayList<Config.LetterClass>>();
				Counter<Config.LetterClass> testCounts = new Counter<Config.LetterClass>();
				for (Config.LetterClass lc : identifier.classes) {
					for (String l : lc.members) {
						for (Config.FontType ft : identifier.fonts) {
							for (int i = 0; i < 50; i++) {
								tests++;
								testCounts.increment(lc);
								BufferedImage img = ExampleGenerator2.makeSemiVariableLetterImage(l, ft, r);
								if (runTest(identifier, img, lc, l + " in " + ft, l, errCounts)) { misses++; }
							}
						}
					}
				}
				System.out.println((tests - misses) + "/" + tests);
				System.out.println(100.0 * (tests - misses) / tests + "%");
				ArrayList<Map.Entry<ArrayList<Config.LetterClass>, Integer>> errL = errCounts.sortedCountsHighestFirst();
				System.out.println("Error summary:");
				for (Map.Entry<ArrayList<Config.LetterClass>, Integer> e : errL) {
					double err = e.getValue() * 100.0 / testCounts.counts.get(e.getKey().get(0));
					System.out.println(e.getKey().get(0) + " -> " + e.getKey().get(1) + ": " + err + "%");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	static boolean runTest(Config.Identifier identifier, BufferedImage img, Config.LetterClass lc, String name, String l,
			Counter<ArrayList<Config.LetterClass>> errCounts)
	{
		boolean miss = false;
		if (identifier instanceof Config.NNIdentifier) {
			Config.NNIdentifier id = ((Config.NNIdentifier) identifier);
			float[] input = Util.getInputForNN(img, id.proportionalInput);
			HashMap<Config.LetterClass, Double> scores = new HashMap<Config.LetterClass, Double>();
			for (int i = 0; i < id.numberOfNetworks; i++) {
				float[] output = id.fastNetworks.get(i).run(input);
				for (Config.LetterClass cmpLC : identifier.classes) {
					for (float[] target : id.targets.get(i).get(cmpLC)) {
						double score = Identifier.score(output, target);
						if (!scores.containsKey(cmpLC)) {
							scores.put(cmpLC, score);
						} else {
							scores.put(cmpLC, Math.max(scores.get(cmpLC), score));
						}
					}
				}
			}

			Config.LetterClass bestLC = null;
			double bestScore = 0;
			for (Map.Entry<Config.LetterClass, Double> e : scores.entrySet()) {
				if (bestLC == null || e.getValue() > bestScore) {
					bestLC = e.getKey();
					bestScore = e.getValue();
				}
			}
			if (bestLC != lc) {
				miss = true;
				ArrayList<Config.LetterClass> errPair = new ArrayList<Config.LetterClass>();
				errPair.add(lc);
				errPair.add(bestLC);
				errCounts.increment(errPair);
				System.out.println(l + "(" + name + ") mis-identified as " + bestLC);
			}
		}
		if (identifier instanceof Config.NumberOfPartsIdentifier) {
			Config.NumberOfPartsIdentifier id = (Config.NumberOfPartsIdentifier) identifier;
			int n = new BetterLetterFinder().find(img, new HashMap<String, String>()).size();
			boolean aboveBoundary = n > id.numberOfPartsBoundary;
			boolean shouldBeAboveBoundary =
					identifier.classes.indexOf(lc) == 0
					? id.firstIsAboveBoundary
					: !id.firstIsAboveBoundary;
			if (shouldBeAboveBoundary != aboveBoundary) {
				miss = true;
				ArrayList<Config.LetterClass> errPair = new ArrayList<Config.LetterClass>();
				errPair.add(lc);
				errPair.add(id.classes.get(id.classes.indexOf(lc) == 0 ? 1 : 0));
				errCounts.increment(errPair);
				System.out.println(l + "(" + name + ") mis-identified.");
			}
		}
		if (identifier instanceof Config.TreeIdentifier) {
			Config.TreeIdentifier id = (Config.TreeIdentifier) identifier;
			Config.LetterClass classification = TreePredict.vote(id.tree.classify(
				TreePredict.getImg(img, null)));
			if (!classification.equals(lc)) {
				miss = true;
				ArrayList<Config.LetterClass> errPair = new ArrayList<Config.LetterClass>();
				errPair.add(lc);
				errPair.add(classification);
				errCounts.increment(errPair);
				System.out.println(l + "(" + name + ") mis-identified as " + classification);
			}
		}
		if (identifier instanceof Config.NearestNeighbourIdentifier) {
			Config.NearestNeighbourIdentifier id = (Config.NearestNeighbourIdentifier) identifier;
			Config.LetterClass best = null;
			double leastError = -1;
			for (Config.LetterClass letterClass : id.classes) {
				double lcLeastError = id.comparisons.leastError(letterClass.members,
						Util.getInputForNN(img, false));
				if (best == null || lcLeastError < leastError) {
					best = letterClass;
					leastError = lcLeastError;
				}
			}
			if (!best.equals(lc)) {
				miss = true;
				ArrayList<Config.LetterClass> errPair = new ArrayList<Config.LetterClass>();
				errPair.add(lc);
				errPair.add(best);
				errCounts.increment(errPair);
				System.out.println(l + "(" + name + ") mis-identified as " + best);
			}
		}
		
		return miss;
	}
	
	static String letterToFilename(String l) {
		return
				l.equals(".")
				? "period"
				: l.equals(":")
				? "colon"
				: l.equals("/")
				? "slash"
				: l.toLowerCase().equals(l)
				? l
				: l.toLowerCase() + "-uc";
	}
	
	public static void generate(Config config, int iters) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		for (Config.Identifier identifier : config.identifiers) {
			if (identifier instanceof Config.NNIdentifier) {
				Config.NNIdentifier id = (Config.NNIdentifier) identifier;
				generateTargets(id);
				int numPasses = iters / identifier.fonts.size() / identifier.classes.size();
				for (int i = 0; i < id.numberOfNetworks; i++) {
					System.out.println("Training network #" + i + " for " + identifier);
					ArrayList<Config.LetterClass> classes = new ArrayList<Config.LetterClass>(identifier.classes);
					//boolean twoClasses = classes.size() == 2;
					Visualizer.VisFrame vf = new Visualizer.VisFrame();
					Network nw = new IdentifierNet(id.seed).nw;// qqDPS(twoClasses ? new DiscriminatorNet().nw : new IdentifierNet().nw);
					Random r = new Random(id.seed + i * 293083);
					for (int pass = 0; pass < numPasses; pass++) {
						Collections.shuffle(classes, r);
						for (Config.LetterClass lc : classes) {
							for (Config.FontType ft : identifier.fonts) {
								String exL = lc.members.get(r.nextInt(lc.members.size()));
								float[] input = getInputForNN(ExampleGenerator2.makeCorrectlyVariableLetterImage(exL, ft, r), id.proportionalInput);
								/*try {
									ImageIO.write(Util.convertInputToImg(input), "png", new File("/Users/zar/Desktop/owthe/" + qq++ + ".png"));
								} catch (Exception e) { e.printStackTrace(); }*/
								Example ex = new Example(
										exL,
										input,
										id.targets.get(i).get(lc).get(0));  
								nw.train(ex.input, ex.target, N, M);
							}
							vf.update(nw, pass);
						}
						Visualizer.saveFrame(nw, pass);
						if (pass % 10 == 0) { System.out.println(pass + "/" + numPasses); }
						// Half of the way through, adjust the targets.
						if (pass == numPasses / 2 || pass == numPasses * 3 / 4) {
							System.out.println("Adjusting targets.");
							for (Config.LetterClass lc : classes) {
								/*System.out.println(lc);
								System.out.println("Original:");
								System.out.println(Arrays.toString(lc.targets[i]));*/
								float[] newTarget = new float[OUTPUT_SIZE];
								for (int e = 0; e < 32; e++) {
									for (Config.FontType ft : identifier.fonts) {
										String exL = lc.members.get(r.nextInt(lc.members.size()));
										float[] input = getInputForNN(ExampleGenerator2.makeCorrectlyVariableLetterImage(exL, ft, r), id.proportionalInput);
										float[] output = nw.run(input);
										//System.out.println(Arrays.toString(output));
										for (int z = 0; z < OUTPUT_SIZE; z++) {
											newTarget[z] += (output[z] / (32 * identifier.fonts.size()));
										}
									}
								}
								/*System.out.println("New:");
								System.out.println(Arrays.toString(newTarget));*/
								id.targets.get(i).get(lc).set(0, newTarget);
							}
						}
					}
					id.networks.add(nw);
					vf.dispose();
				}
				// extendTargets(id, new Random(id.seed)); qqDPS This was a bad idea.
			}
			if (identifier instanceof Config.NumberOfPartsIdentifier) {
				System.out.println("Determining number of parts for " + identifier);
				setNumberOfPartsBoundary((Config.NumberOfPartsIdentifier) identifier);
			}
			if (identifier instanceof Config.TreeIdentifier) {
				System.out.println("Generating tree for " + identifier);
				((Config.TreeIdentifier) identifier).tree = TreePredict.buildTree((Config.TreeIdentifier) identifier);
			}
			if (identifier instanceof Config.NearestNeighbourIdentifier) {
				System.out.println("Generating comparison data for " + identifier);
				((Config.NearestNeighbourIdentifier) identifier).comparisons = NearestNeighbour.createComparisons(((Config.NearestNeighbourIdentifier) identifier));
			}
			setExpectedRelativeSizes(identifier);
			setAspectRatios(identifier);
		}
	}
	
	static double getAspectRatio(Config.FontType font, String letter) {
		Rectangle rect = getLetterRect(font, letter);
		return rect.getWidth() / rect.getHeight();//(rect.getWidth() + 10) / (rect.getHeight() + 10);
	}
	
	static void setExpectedRelativeSizes(Config.Identifier id) {
		id.expectedRelativeSizes = new HashMap<String, Double>();
		double sizeAcc = 0;
		for (Config.FontType ft : id.fonts) {
			for (int i = 0; i < id.sampleSentence.length(); i++) {
				double sz = getSize(ft, id.sampleSentence.substring(i, i + 1));
				if (sz < 99) {
					sizeAcc += sz;
				}
			}
		}
		double avgSize = sizeAcc / id.sampleSentence.length() / id.fonts.size();
		for (Config.LetterClass lc : id.classes) {
			for (String l : lc.members) {
				double avg = 0.0;
				for (Config.FontType ft : id.fonts) {
					avg += getSize(ft, l);
				}
				id.expectedRelativeSizes.put(l, avg / id.fonts.size() / avgSize);
			}
		}
	}
	
	static void setAspectRatios(Config.Identifier id) {
		id.expectedAspectRatios = new HashMap<String, Double>();
		for (Config.LetterClass lc : id.classes) {
			for (String l : lc.members) {
				double avg = 0.0;
				for (Config.FontType ft : id.fonts) {
					avg += getAspectRatio(ft, l);
				}
				id.expectedAspectRatios.put(l, avg / id.fonts.size());
			}
		}
	}
	
	static Rectangle getLetterRect(Config.FontType ft, String l) {
		BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setFont(new Font(ft.font, ft.italic ? Font.ITALIC : Font.PLAIN, 50));
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 100, 100);
		g.setColor(Color.BLACK);
		g.drawString(l, 50, 50);
		ArrayList<Letter> ls = new BetterLetterFinder().find(img, new HashMap<String, String>());
		Rectangle rect = ls.get(0);
		for (int i = 1; i < ls.size(); i++) {
			rect.add(ls.get(i));
		}
		if (rect.width == 100 && rect.height == 100) {
			HashMap<String, String> meta = new HashMap<String, String>();
			meta.put("blackWhiteBoundary", "250");
			ls = new BetterLetterFinder().find(img, meta);
			if (ls.isEmpty()) { return rect; }
			rect = ls.get(0);
			for (int i = 1; i < ls.size(); i++) {
				rect.add(ls.get(i));
			}
		}
		return rect;
	}
	
	static double getSize(Config.FontType ft, String l) {
		Rectangle rect = getLetterRect(ft, l);
		return Math.sqrt(rect.width * rect.height);
	}
	
	static void setNumberOfPartsBoundary(Config.NumberOfPartsIdentifier id) {
		if (id.classes.size() != 2) {
			id.enabled = false;
			System.out.println(id + ": NumberOfPartsIdentifiers need to have exactly 2 classes.");
			return;
		}
		BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		int n0 = -1;
		int n1 = -1;
		for (Config.FontType ft : id.fonts) {
			g.setFont(new Font(ft.font, ft.italic ? Font.ITALIC : Font.PLAIN, 50));
			for (String l : id.classes.get(0).members) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, 100, 100);
				g.setColor(Color.BLACK);
				g.drawString(l, 50, 50);
				int myN0 = new BetterLetterFinder().find(img, new HashMap<String, String>()).size();
				if (n0 == -1) {
					n0 = myN0;
				} else {
					if (n0 != myN0) {
						id.enabled = false;
						System.out.println(id + ": " + id.classes.get(0) + " doesn't all have the" +
								"same number of parts.");
						return;
					}
				}
			}
			for (String l : id.classes.get(1).members) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, 100, 100);
				g.setColor(Color.BLACK);
				g.drawString(l, 50, 50);
				int myN1 = new BetterLetterFinder().find(img, new HashMap<String, String>()).size();
				if (n1 == -1) {
					n1 = myN1;
				} else {
					if (n1 != myN1) {
						id.enabled = false;
						System.out.println(id + ": " + id.classes.get(1) + " doesn't all have the" +
								"same number of parts.");
						return;
					}
				}
			}
		}
		if (n0 == n1) {
			System.out.println(id + ": both classes have the same number of parts.");
		} else {
			id.enabled = true;
			/*
				Logic:
				n > boundary != triggerAbove --> use alt

				Trigger	Alt	Boundary	TA		n	n>b		Result	Correct
				3		2	2			true	2	false	alt		√
				3		2	2			true	3	true	tr		√
				2		3	2			false	2	false	tr		√
				2		3	2			false	3	true	alt		√
			 */
			if (n0 > n1) {
				id.numberOfPartsBoundary = n1;
				id.firstIsAboveBoundary = true;
			} else {
				id.numberOfPartsBoundary = n0;
				id.firstIsAboveBoundary = false;
			}
		}
	}
	
	public static void generateTargets(Config.NNIdentifier id) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		for (int n = 0; n < id.numberOfNetworks; n++) {
			HashMap<Config.LetterClass, ArrayList<float[]>> ts = new HashMap<Config.LetterClass, ArrayList<float[]>>();
			id.targets.add(ts);
			for (Config.LetterClass lc : id.classes) {
				String hashable = lc.toString() + n;
				byte[] digest = md.digest(hashable.getBytes("UTF-8"));
				float[] target = new float[OUTPUT_SIZE];
				for (int i = 0; i < 16; i++) {
					for (int j = 0; j < 8; j++) {
						//data[i * 8 + j] = ((digest[i] >>> j) & 1) == 1 ? 1.0f : 0f; // qqDPS
						target[i * 8 + j] = ((digest[i] >>> j) & 1) == 1 ? 0.85f : -0.85f;
					}
				}
				ArrayList<float[]> singleTarget = new ArrayList<float[]>();
				singleTarget.add(target);
				ts.put(lc, singleTarget);
			}
		}
	}
}
