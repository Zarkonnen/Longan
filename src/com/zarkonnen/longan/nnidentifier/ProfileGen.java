package com.zarkonnen.longan.nnidentifier;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
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
	static final int DEFAULT_PASSES = 640;
	static final int OUTPUT_SIZE = 128;
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, JSONException, IOException, NoSuchAlgorithmException, ParseException {
		Options options = new Options();
		Option helpO = OptionBuilder.withDescription("print help").create("h");
		Option versionO = OptionBuilder.withDescription("print version").create("v");
		Option generateO = OptionBuilder.withDescription("Generate set of neural network weights. Takes two file arguments: the source JSON file and the target zip.").withLongOpt("generate").create("g");
		Option testO = OptionBuilder.withDescription("Test set of neural networks against images. Takes two file arguments: the zip of weights and a folder of images.").withLongOpt("test").create("t");
		Option itersO = OptionBuilder.withDescription("Number of iterations to train the network(s) for.").withLongOpt("iters").hasArg().withArgName("iterations").create("i");
		options.addOption(helpO);
		options.addOption(versionO);
		options.addOption(generateO);
		options.addOption(testO);
		options.addOption(itersO);
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
				testNetworks(config, new File((String) line.getArgList().get(1)));
				System.out.println((System.currentTimeMillis() - t) + " ms");
				System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void testNetworks(Config config, File imageFolder) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		generateTargets(config);
		for (Config.Identifier identifier : config.identifiers) {
			try {
				System.out.println();
				System.out.println("Testing " + identifier);
				int misses = 0;
				int tests = 0;
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
							ATRReader r = new ATRReader(new BufferedInputStream(new FileInputStream(fontFile)));
							List<String> line = null;
							while ((line = r.readRecord()) != null) {
								if (!line.get(1).equals(identifier.font.toString())) {
									doNotTest.add(line.get(0));
								}
							}
						}
						for (File f : letterFolder.listFiles()) {
							if (f.getName().endsWith(".png") && !doNotTest.contains(f.getName().substring(0, f.getName().length() - 4))) {
								float[] input = Util.getInputForNN(ImageIO.read(f));
								float[] output = identifier.network.run(input);
								
								Config.LetterClass bestLC = null;
								double bestScore = 0;
								for (Config.LetterClass cmpLC : identifier.classes) {
									double score = Identifier.score(output, cmpLC.target);
									if (score > bestScore) {
										bestLC = cmpLC;
										bestScore = score;
									}
								}
								tests++;
								if (bestLC != lc) {
									misses++;
									System.out.println(l + "(" + f.getName() + ") mis-identified as " + bestLC);
								}
							}
						}
					}
				}
				System.out.println((tests - misses) + "/" + tests);
				System.out.println(100.0 * (tests - misses) / tests + "%");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (Config.Discriminator discriminator : config.discriminators) {
			try {
				System.out.println();
				System.out.println("Testing " + discriminator);
				int misses = 0;
				int tests = 0;
				File letterFolder = new File(imageFolder, letterToFilename(discriminator.trigger));
				if (!letterFolder.exists()) {
					System.out.println("(No test data for letter \"" + discriminator.trigger + "\".)");
					continue;
				}
				File fontFile = new File(imageFolder, letterToFilename(discriminator.trigger) + "-font.atr");
				HashSet<String> doNotTest = new HashSet<String>();
				if (!fontFile.exists()) {
					System.out.println("(No font file \"" + letterToFilename(discriminator.trigger) + "-font.atr + \" found.)");
				} else {
					ATRReader r = new ATRReader(new BufferedInputStream(new FileInputStream(fontFile)));
					List<String> line = null;
					while ((line = r.readRecord()) != null) {
						if (!line.get(1).equals(discriminator.font.toString())) {
							doNotTest.add(line.get(0));
						}
					}
				}
				HashMap<String, Double> sizes = new HashMap<String, Double>();
				File sizeFile = new File(imageFolder, letterToFilename(discriminator.trigger) + "-size.atr");
				if (!sizeFile.exists()) {
					System.out.println("(No size file \"" + letterToFilename(discriminator.trigger) + "-size.atr + \" found.)");
				} else {
					ATRReader r = new ATRReader(new BufferedInputStream(new FileInputStream(sizeFile)));
					List<String> line = null;
					while ((line = r.readRecord()) != null) {
						sizes.put(line.get(0), Double.parseDouble(line.get(1)));
					}
				}
				if (discriminator instanceof Config.NNDiscriminator ||
					discriminator instanceof Config.AspectRatioDiscriminator ||
					discriminator instanceof Config.NumberOfPartsDiscriminator ||
					discriminator instanceof Config.RelativeSizeDiscriminator)
				{
					for (File f : letterFolder.listFiles()) {
						if (f.getName().endsWith(".png") && !doNotTest.contains(f.getName().substring(0, f.getName().length() - 4))) {
							tests++;
							if (discriminator instanceof Config.NNDiscriminator) {
								float[] output = ((Config.NNDiscriminator) discriminator).network.run(Util.getInputForNN(ImageIO.read(f)));
								if (output[0] > 0.5f) {
									misses++;
									System.out.println(discriminator.trigger + "(" + f.getName() + ") mis-identified as " + discriminator.alternative);
								}
							}
							if (discriminator instanceof Config.AspectRatioDiscriminator) {
								BufferedImage img = ImageIO.read(f);
								double aspectRatio = (img.getWidth() * 1.0) / img.getHeight();
								boolean aboveBoundary = aspectRatio > ((Config.AspectRatioDiscriminator) discriminator).boundaryRatio;
								if (((Config.AspectRatioDiscriminator) discriminator).triggerIsAboveBoundary != aboveBoundary) {
									misses++;
									System.out.println(discriminator.trigger + "(" + f.getName() + ") mis-identified as " + discriminator.alternative);
								}
							}
							if (discriminator instanceof Config.NumberOfPartsDiscriminator) {
								BufferedImage img = ImageIO.read(f);
								int n = new BetterLetterFinder().find(img, new HashMap<String, String>()).size();
								boolean aboveBoundary = n > ((Config.NumberOfPartsDiscriminator) discriminator).numberOfPartsBoundary;
								if (((Config.NumberOfPartsDiscriminator) discriminator).triggerIsAboveBoundary != aboveBoundary) {
									misses++;
									System.out.println(discriminator.trigger + "(" + f.getName() + ") mis-identified as " + discriminator.alternative);
								}
							}
							if (discriminator instanceof Config.RelativeSizeDiscriminator) {
								if (!sizes.containsKey(f.getName().substring(0, f.getName().length() - 4))) { continue; }
								double sz = sizes.get(f.getName().substring(0, f.getName().length() - 4));
								boolean aboveBoundary = sz > ((Config.RelativeSizeDiscriminator) discriminator).boundarySize;
								if (((Config.RelativeSizeDiscriminator) discriminator).triggerIsAboveBoundary != aboveBoundary) {
									misses++;
									System.out.println(discriminator.trigger + "(" + f.getName() + ") mis-identified as " + discriminator.alternative);
								}
							}
						}
					}
				}


				letterFolder = new File(imageFolder, letterToFilename(discriminator.alternative));
				if (!letterFolder.exists()) {
					System.out.println("(No test data for letter \"" + discriminator.alternative + "\".)");
					continue;
				}
				fontFile = new File(imageFolder, letterToFilename(discriminator.alternative) + "-font.atr");
				doNotTest = new HashSet<String>();
				if (!fontFile.exists()) {
					System.out.println("(No font file \"" + letterToFilename(discriminator.alternative) + "-font.atr + \" found.)");
				} else {
					ATRReader r = new ATRReader(new BufferedInputStream(new FileInputStream(fontFile)));
					List<String> line = null;
					while ((line = r.readRecord()) != null) {
						if (!line.get(1).equals(discriminator.font.toString())) {
							doNotTest.add(line.get(0));
						}
					}
				}
				sizes.clear();
				sizeFile = new File(imageFolder, letterToFilename(discriminator.alternative) + "-size.atr");
				if (!sizeFile.exists()) {
					System.out.println("(No size file \"" + letterToFilename(discriminator.alternative) + "-size.atr + \" found.)");
				} else {
					ATRReader r = new ATRReader(new BufferedInputStream(new FileInputStream(sizeFile)));
					List<String> line = null;
					while ((line = r.readRecord()) != null) {
						sizes.put(line.get(0), Double.parseDouble(line.get(1)));
					}
				}
				if (discriminator instanceof Config.NNDiscriminator ||
					discriminator instanceof Config.AspectRatioDiscriminator ||
					discriminator instanceof Config.NumberOfPartsDiscriminator ||
					discriminator instanceof Config.RelativeSizeDiscriminator)
				{
					for (File f : letterFolder.listFiles()) {
						if (f.getName().endsWith(".png") && !doNotTest.contains(f.getName().substring(0, f.getName().length() - 4))) {
							tests++;
							if (discriminator instanceof Config.NNDiscriminator) {
								float[] output = ((Config.NNDiscriminator) discriminator).network.run(Util.getInputForNN(ImageIO.read(f)));
								if (output[0] <= 0.5f) {
									misses++;
									System.out.println(discriminator.alternative + "(" + f.getName() + ") mis-identified as " + discriminator.trigger);
								}
							}
							if (discriminator instanceof Config.AspectRatioDiscriminator) {
								BufferedImage img = ImageIO.read(f);
								double aspectRatio = (img.getWidth() * 1.0) / img.getHeight();
								boolean aboveBoundary = aspectRatio > ((Config.AspectRatioDiscriminator) discriminator).boundaryRatio;
								if (((Config.AspectRatioDiscriminator) discriminator).triggerIsAboveBoundary == aboveBoundary) {
									misses++;
									System.out.println(discriminator.alternative + "(" + f.getName() + ") mis-identified as " + discriminator.trigger);
								}
							}
							if (discriminator instanceof Config.NumberOfPartsDiscriminator) {
								BufferedImage img = ImageIO.read(f);
								int n = new BetterLetterFinder().find(img, new HashMap<String, String>()).size();
								boolean aboveBoundary = n > ((Config.NumberOfPartsDiscriminator) discriminator).numberOfPartsBoundary;
								if (((Config.NumberOfPartsDiscriminator) discriminator).triggerIsAboveBoundary == aboveBoundary) {
									misses++;
									System.out.println(discriminator.alternative + "(" + f.getName() + ") mis-identified as " + discriminator.trigger);
								}
							}
							if (discriminator instanceof Config.RelativeSizeDiscriminator) {
								if (!sizes.containsKey(f.getName().substring(0, f.getName().length() - 4))) { continue; }
								double sz = sizes.get(f.getName().substring(0, f.getName().length() - 4));
								boolean aboveBoundary = sz > ((Config.RelativeSizeDiscriminator) discriminator).boundarySize;
								if (((Config.RelativeSizeDiscriminator) discriminator).triggerIsAboveBoundary == aboveBoundary) {
									misses++;
									System.out.println(discriminator.alternative + "(" + f.getName() + ") mis-identified as " + discriminator.trigger);
								}
							}
						}
					}
				}
				System.out.println((tests - misses) + "/" + tests);
				System.out.println(100.0 * (tests - misses) / tests + "%");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
		generateTargets(config);
		Random r = new Random();
		for (Config.Identifier identifier : config.identifiers) {
			System.out.println("Training network for " + identifier);
			IdentifierNet in = new IdentifierNet();
			ArrayList<Config.LetterClass> classes = new ArrayList<Config.LetterClass>(identifier.classes);
			for (int pass = 0; pass < iters; pass++) {
				Collections.shuffle(classes);
				for (Config.LetterClass lc : classes) {
					String exL = lc.members.get(r.nextInt(lc.members.size()));
					Example ex = new Example(
							exL,
							getInputForNN(ExampleGenerator2.makeLetterImage(exL, identifier.font)),
							lc.target);
					in.train(ex, 0.002f, 0.0005f);
				}
				if (pass % 10 == 0) { System.out.println(pass + "/" + iters); }
			}
			identifier.network = in;
			setExpectedRelativeSizes(identifier);
			setAspectRatios(identifier);
		}
		
		for (Config.Discriminator discriminator : config.discriminators) {
			if (discriminator instanceof Config.NNDiscriminator) {
				System.out.println("Training network for " + discriminator);
				DiscriminatorNet nw = new DiscriminatorNet();
				for (int pass = 0; pass < iters * 20; pass++) {
					boolean alternative = r.nextBoolean();
					String exL = alternative ? discriminator.alternative : discriminator.trigger;
					Example ex = new Example(
							exL,
							getInputForNN(ExampleGenerator2.makeLetterImage(exL, discriminator.font)),
							new float[] { alternative ? 1.0f : 0.0f });
					nw.train(ex, 0.002f, 0.0005f);
					if (pass % 100 == 0) { System.out.println(pass / 20 + "/" + iters); }
				}
				((Config.NNDiscriminator) discriminator).network = nw;
			}
			if (discriminator instanceof Config.AspectRatioDiscriminator) {
				System.out.println("Determining aspect ratios for " + discriminator);
				setAspectRatioBoundary((Config.AspectRatioDiscriminator) discriminator);
			}
			if (discriminator instanceof Config.NumberOfPartsDiscriminator) {
				System.out.println("Determining number of parts for " + discriminator);
				setNumberOfPartsBoundary((Config.NumberOfPartsDiscriminator) discriminator);
			}
			if (discriminator instanceof Config.RelativeSizeDiscriminator) {
				System.out.println("Determining relative sizes for " + discriminator);
				setRelativeSizeBoundary((Config.RelativeSizeDiscriminator) discriminator);
			}
		}
	}
	
	static double getAspectRatio(Config.FontType font, String letter) {
		Rectangle rect = getLetterRect(font, letter);
		return (rect.getWidth() + 10) / (rect.getHeight() + 10);
	}
	
	static void setAspectRatioBoundary(Config.AspectRatioDiscriminator d) {
		double triggerRatio = getAspectRatio(d.font, d.trigger);
		double altRatio = getAspectRatio(d.font, d.alternative);
		d.boundaryRatio = triggerRatio / 2 + altRatio / 2;
		d.triggerIsAboveBoundary = triggerRatio > altRatio;
	}
	
	static void setExpectedRelativeSizes(Config.Identifier id) {
		id.expectedRelativeSizes = new HashMap<String, Double>();
		double sizeAcc = 0;
		for (int i = 0; i < id.sampleSentence.length(); i++) {
			double sz = getSize(id.font, id.sampleSentence.substring(i, i + 1));
			if (sz < 99) {
				sizeAcc += sz;
			}
		}
		double avgSize = sizeAcc / id.sampleSentence.length();
		for (Config.LetterClass lc : id.classes) {
			for (String l : lc.members) {
				id.expectedRelativeSizes.put(l, getSize(id.font, l) / avgSize);
			}
		}
	}
	
	static void setAspectRatios(Config.Identifier id) {
		id.expectedAspectRatios = new HashMap<String, Double>();
		for (Config.LetterClass lc : id.classes) {
			for (String l : lc.members) {
				id.expectedAspectRatios.put(l, getAspectRatio(id.font, l));
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
	
	static void setRelativeSizeBoundary(Config.RelativeSizeDiscriminator d) {
		double sizeAcc = 0;
		for (int i = 0; i < d.sampleSentence.length(); i++) {
			double sz = getSize(d.font, d.sampleSentence.substring(i, i + 1));
			if (sz < 99) {
				sizeAcc += sz;
			}
		}
		double avgSize = sizeAcc / d.sampleSentence.length();
		System.out.println("Avg size is " + avgSize);
		double triggerRelSize = getSize(d.font, d.trigger) / avgSize;
		System.out.println("Trigger rel size: " + triggerRelSize);
		double altRelSize = getSize(d.font, d.alternative) / avgSize;
		System.out.println("Alt rel size: " + altRelSize);
		d.boundarySize = triggerRelSize / 2 + altRelSize / 2;
		System.out.println("Boundary: " + d.boundarySize);
		d.triggerIsAboveBoundary = triggerRelSize > altRelSize;
	}
	
	static void setNumberOfPartsBoundary(Config.NumberOfPartsDiscriminator d) {
		BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setFont(new Font(d.font.font, d.font.italic ? Font.ITALIC : Font.PLAIN, 50));
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 100, 100);
		g.setColor(Color.BLACK);
		g.drawString(d.trigger, 50, 50);
		int triggerN = new BetterLetterFinder().find(img, new HashMap<String, String>()).size();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 100, 100);
		g.setColor(Color.BLACK);
		g.drawString(d.alternative, 50, 50);
		int altN = new BetterLetterFinder().find(img, new HashMap<String, String>()).size(); //towers
		if (triggerN == altN) {
			System.out.println("Warning: " + d.trigger + " and " + d.alternative + " have the " +
					"same number of parts. Their NumberOfPartsDiscriminator has been disabled.");
		} else {
			d.enabled = true;
			/*
				Logic:
				n > boundary != triggerAbove --> use alt

				Trigger	Alt	Boundary	TA		n	n>b		Result	Correct
				3		2	2			true	2	false	alt		√
				3		2	2			true	3	true	tr		√
				2		3	2			false	2	false	tr		√
				2		3	2			false	3	true	alt		√
			 */
			if (triggerN > altN) {
				d.numberOfPartsBoundary = altN;
				d.triggerIsAboveBoundary = true;
			} else {
				d.numberOfPartsBoundary = triggerN;
				d.triggerIsAboveBoundary = false;
			}
		}
	}
	
	public static void generateTargets(Config config) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		for (Config.Identifier identifier : config.identifiers) {
			HashSet<FloatArray> used = new HashSet<FloatArray>();
			for (Config.LetterClass lc : identifier.classes) {
				String hashable = lc.toString();
				while (true) {
					byte[] digest = md.digest(hashable.getBytes("UTF-8"));
					FloatArray data = new FloatArray(new float[OUTPUT_SIZE]);
					for (int i = 0; i < 16; i++) {
						for (int j = 0; j < 8; j++) {
							data.data[i * 8 + j] = (digest[i] >>> j) & 1;
						}
					}
					if (!used.contains(data)) {
						lc.target = data.data;
						break;
					}
					hashable += "+";
				}
			}
		}
	}
	
	static class FloatArray {
		float[] data;
		public FloatArray(float[] data) { this.data = data; }
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof FloatArray)) { return false; }
			return Arrays.equals(data, ((FloatArray) o).data);
		}

		@Override
		public int hashCode() {
			return 111 + Arrays.hashCode(this.data);
		}
	}
}
