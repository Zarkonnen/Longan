package com.zarkonnen.longan.profilegen;

import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.opencl.CompiledOpenCLNetwork;
import com.zarkonnen.longan.profilegen.network.Util;
import com.zarkonnen.longan.stage.LetterIdentifier;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class Identifier implements LetterIdentifier {
	private final Config config;
	static final int REFERENCE_INTENSITY_BOUNDARY = 165;
	static final double ALSO_RAN_PROMO =     0.0001;
	static final double BEST_ALT_PROMOTION = 0.002;
	
	public Identifier(Config config) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.config = config;
		ProfileGen.generateTargets(config);
	}
	
	public Identifier() {
		try {
			config = NetworkIO.readDefaultArchive();
			ProfileGen.generateTargets(config);
		} catch (Exception e) {
			throw new RuntimeException("Could not initialise default neural network identifier.", e);
		}
	}
	
	static final class LetterClassInFont {
		final Config.LetterClass letterClass;
		final Config.FontType font;

		public LetterClassInFont(Config.LetterClass letterClass, Config.FontType font) {
			this.letterClass = letterClass;
			this.font = font;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof LetterClassInFont)) { return false; }
			LetterClassInFont lef2 = (LetterClassInFont) o;
			return letterClass.equals(lef2.letterClass) && font.equals(lef2.font); 
		}
		
		@Override
		public int hashCode() { return 41 + letterClass.hashCode() * 13 + font.hashCode(); }
	}
	
	static boolean done = false;
	
	public Letter identify(Letter l, Result result) {
		if (!done) {
			identify(result);
			done = true;
		}
		return l;
	}
	
	static final class LetterClassInIdentifier {
		final Config.LetterClass letterClass;
		final Config.Identifier identifier;

		public LetterClassInIdentifier(Config.LetterClass letterClass, Config.Identifier identifier) {
			this.letterClass = letterClass;
			this.identifier = identifier;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof LetterClassInIdentifier)) { return false; }
			LetterClassInIdentifier lef2 = (LetterClassInIdentifier) o;
			return letterClass.equals(lef2.letterClass) && identifier.equals(lef2.identifier); 
		}
		
		@Override
		public int hashCode() { return 41 + letterClass.hashCode() * 13 + identifier.hashCode(); }
	}
	
	static final double MIN_VOTE_PROPORTION_TO_STAY_IN_RACE = 0.2;
	static final int MEASURE_VOTE_PROPORTION_EVERY = 16;
		
	public void identify(Result result) {
		int intensityAdjustment = 0;
		if (result.metadata.containsKey("blackWhiteBoundary")) {
			int blackWhiteBoundary = Integer.parseInt(result.metadata.get("blackWhiteBoundary"));
			intensityAdjustment = (REFERENCE_INTENSITY_BOUNDARY - blackWhiteBoundary) * 3 / 4;
		}
		boolean enableOpenCL = result.metadata.get("enableOpenCL").equals("true");
		boolean openCLTested = false;
		HashMap<Letter, HashMap<LetterClassInIdentifier, Double>> scores = new HashMap<Letter, HashMap<LetterClassInIdentifier, Double>>();
		HashMap<Config.Identifier, Integer> votes = new HashMap<Config.Identifier, Integer>();
		HashMap<Letter, float[]> inputs = new HashMap<Letter, float[]>();
		HashMap<Config.Identifier, CompiledOpenCLNetwork> openCLIdentifiers = new HashMap<Config.Identifier, CompiledOpenCLNetwork>();
		HashMap<Config.NNDiscriminator, CompiledOpenCLNetwork> openCLDiscriminators = new HashMap<Config.NNDiscriminator, CompiledOpenCLNetwork>();
		for (Column col : result.columns) {
			ArrayList<Integer> sizes = new ArrayList<Integer>();
			for (Line line : col.lines) { for (Word word : line.words) { for (Letter letter : word.letters) {
				sizes.add((int) Math.sqrt(letter.width * letter.height));
			}}}
			Collections.sort(sizes);
			long sizeSum = 0;
			for (int sz : sizes.subList(sizes.size() / 4, sizes.size() * 3 / 4)) {
				sizeSum += sz;
			}
			double avgLetterSize = sizeSum * 1.0 / (sizes.size() / 2);
			votes.clear();
			scores.clear();
			inputs.clear();
			for (Config.Identifier identifier : config.identifiers) {
				votes.put(identifier, 0);
			}
			int loop = 0;
			int votesGiven = 0;
			for (Line line : col.lines) { for (Word w : line.words) { for (Letter letter : w.letters) {
				float[] data = Util.getInputForNN(letter, result.img, intensityAdjustment);
				inputs.put(letter, data);
				HashMap<LetterClassInIdentifier, Double> lScores = new HashMap<LetterClassInIdentifier, Double>();
				Config.Identifier identifierVote = null;
				double bestScore = -1.0;
				for (Config.Identifier identifier : votes.keySet()) {
					CompiledOpenCLNetwork cocl = null;
					if (enableOpenCL) {
						if (openCLIdentifiers.containsKey(identifier)) {
							cocl = openCLIdentifiers.get(identifier);
						} else {
							cocl = new CompiledOpenCLNetwork(identifier.network.nw);
							try {
								cocl.init();
								if (!openCLTested) {
									cocl.test();
								}
								openCLIdentifiers.put(identifier, cocl);
							} catch (Exception e) {
								System.err.println("Unable to use openCL. Switching to CPU.");
								enableOpenCL = false;
								cocl = null;
							} finally {
								openCLTested = true;
							}
						}
					}
					float[] output = cocl == null ? identifier.network.run(data) : cocl.run(data);
					for (Config.LetterClass lc : identifier.classes) {
						double score = score(output, lc.target);
						if (score > bestScore) {
							bestScore = score;
							identifierVote = identifier;
						}
						lScores.put(new LetterClassInIdentifier(lc, identifier), score);
					}
				}
				scores.put(letter, lScores);
				votes.put(identifierVote, votes.get(identifierVote) + 1);
				loop++;
				votesGiven++;
				int currentVotesGiven = votesGiven;
				if (loop % MEASURE_VOTE_PROPORTION_EVERY == 0) {
					for (Iterator<Map.Entry<Config.Identifier, Integer>> it = votes.entrySet().iterator(); it.hasNext();) {
						int eVotes = it.next().getValue();
						if (eVotes < 0.2 * currentVotesGiven) {
							votesGiven -= eVotes;
							it.remove();
						}
					}
				}
			} } }
			// Vote on which identifier we believe.
			int highestVote = 0;
			Config.Identifier bestIdentifier = null;
			for (Map.Entry<Config.Identifier, Integer> e : votes.entrySet()) {
				if (e.getValue() > highestVote) {
					highestVote = e.getValue();
					bestIdentifier = e.getKey();
				}
			}
			// We have decided! Use the output of the bestIdentifier.
			for (Line line : col.lines) { for (Word w : line.words) { for (Letter letter : w.letters) {
				for (Map.Entry<LetterClassInIdentifier, Double> e : scores.get(letter).entrySet()) {
					if (e.getKey().identifier == bestIdentifier) {
						for (String possibleL : e.getKey().letterClass.members) {
							letter.possibleLetters.put(possibleL, e.getValue());
						}
					}
				}
				
				// Now see if we wanna apply discriminators.
				// Phase 1: NumberOfParts
				String best = letter.bestLetter();
				System.out.print(best);
				System.out.print(" " + Math.sqrt(letter.width * letter.height) / avgLetterSize);
				String prev = best;
				HashSet<String> alsoRans = new HashSet<String>();
				for (Config.Discriminator discriminator : config.discriminators) {
					if (discriminator instanceof Config.NumberOfPartsDiscriminator && discriminator.font.equals(bestIdentifier.font) && discriminator.trigger.equals(best)) {
						Config.NumberOfPartsDiscriminator d = (Config.NumberOfPartsDiscriminator) discriminator;
						int n = letter.components.size();
						n = n == 0 ? 1 : n;
						if (n > d.numberOfPartsBoundary != d.triggerIsAboveBoundary) {
							best = d.alternative;
							break;
						}
					}
				}
				alsoRans.add(best);
				if (!best.equals(prev)) {
					System.out.print(" -P-> " + best);
					prev = best;
				}
				
				// Phase 2: NeuralNetwork
				double bestDiscScore = 0.5;
				String newBestLetter = best;
				for (Config.Discriminator discriminator : config.discriminators) {
					if (discriminator instanceof Config.NNDiscriminator && discriminator.font.equals(bestIdentifier.font) && discriminator.trigger.equals(best)) {
						Config.NNDiscriminator nnd = (Config.NNDiscriminator) discriminator;
						CompiledOpenCLNetwork cocl = null;
						if (enableOpenCL) {
							if (openCLDiscriminators.containsKey(nnd)) {
								cocl = openCLDiscriminators.get(nnd);
							} else {
								cocl = new CompiledOpenCLNetwork(nnd.network.nw);
								try {
									cocl.init();
									if (!openCLTested) {
										cocl.test();
									}
									openCLDiscriminators.put(nnd, cocl);
								} catch (Exception e) {
									System.err.println("Unable to use openCL. Switching to CPU.");
									enableOpenCL = false;
									cocl = null;
								} finally {
									openCLTested = true;
								}
							}
						}
						float output = cocl == null ? nnd.network.run(inputs.get(letter))[0] : cocl.run(inputs.get(letter))[0];
						if (output > bestDiscScore) {
							bestDiscScore = output;
							newBestLetter = discriminator.alternative;
						}
						if (output > 0.5) {
							alsoRans.add(discriminator.alternative);
						}
					}
				}
				best = newBestLetter;
				alsoRans.add(best);
				if (!best.equals(prev)) {
					System.out.print(" -N-> " + best);
					prev = best;
				}
				
				// Phase 3: AspectRatio
				for (Config.Discriminator discriminator : config.discriminators) {
					if (discriminator instanceof Config.AspectRatioDiscriminator && discriminator.font.equals(bestIdentifier.font) && discriminator.trigger.equals(best)) {
						Config.AspectRatioDiscriminator ard = (Config.AspectRatioDiscriminator) discriminator;
						double aspectRatio = (letter.width * 1.0) / letter.height;
						if (aspectRatio > ard.boundaryRatio != ard.triggerIsAboveBoundary) {
							best = discriminator.alternative;
							break;
						}
					}
				}
				if (!best.equals(prev)) {
					System.out.print(" -A-> " + best);
					prev = best;
				}
				
				// Phase 4: Relative size
				for (Config.Discriminator discriminator : config.discriminators) {
					if (discriminator instanceof Config.RelativeSizeDiscriminator && discriminator.font.equals(bestIdentifier.font) &&
						(discriminator.trigger.equals(best) || discriminator.alternative.equals(best))) {
						double relSize = Math.sqrt(letter.width * letter.height) / avgLetterSize;
						Config.RelativeSizeDiscriminator rsd = (Config.RelativeSizeDiscriminator) discriminator;
						if (discriminator.trigger.equals(best)) {
							if (relSize > rsd.boundarySize != rsd.triggerIsAboveBoundary) {
								best = discriminator.alternative;
								break;
							}
						}
						if (discriminator.alternative.equals(best)) {
							if (relSize > rsd.boundarySize == rsd.triggerIsAboveBoundary) {
								best = discriminator.trigger;
								break;
							}
						}
					}
				}
				
				if (!best.equals(prev)) {
					System.out.print(" -S-> " + best);
					prev = best;
				}
				
				// Modify the scores accordingly.
				double bestScore = letter.bestScore();
				for (String ar : alsoRans) {
					letter.possibleLetters.put(ar, bestScore + letter.possibleLetters.get(ar) * ALSO_RAN_PROMO);
				}
				letter.possibleLetters.put(best, bestScore + BEST_ALT_PROMOTION);
				
				System.out.println();
			} } }
		}
	}
	
	public static float score(float[] output, float[] target) {
		float error = 0.0f;
		for (int i = 0; i < ProfileGen.OUTPUT_SIZE; i++) {
			error += (output[i] - target[i]) * (output[i] - target[i]);
		}
		return (ProfileGen.OUTPUT_SIZE - error) / ProfileGen.OUTPUT_SIZE;
	}
}
