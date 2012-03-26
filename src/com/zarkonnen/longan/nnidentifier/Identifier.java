package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.Histogram;
import com.zarkonnen.longan.Metadata;
import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.nnidentifier.Config.LetterClass;
import com.zarkonnen.longan.nnidentifier.network.Util;
import com.zarkonnen.longan.stage.LetterIdentifier;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class Identifier implements LetterIdentifier {
	private final Config config;
	static final int REFERENCE_INTENSITY_BOUNDARY = 165;
	static final double ALSO_RAN_PROMO     = 0.0001;
	static final double BEST_ALT_PROMOTION = 0.002;
	public static final Metadata.Key<Config.Identifier> IDENTIFIER_USED = Metadata.key("identifierUsed", Config.Identifier.class);
	public static final Metadata.Key<Integer> AVG_LETTER_SIZE = Metadata.key("avgLetterSize", Integer.class);
	static final double ASPECT_TO_SIZE_DIST_RATIO = 3.0;
	
	public Identifier(Config config) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.config = config;
	}
	
	public Identifier() {
		try {
			config = NetworkIO.readDefaultArchive();
		} catch (Exception e) {
			throw new RuntimeException("Could not initialise default neural network identifier.", e);
		}
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
		HashMap<Config.Identifier, Integer> votes = new HashMap<Config.Identifier, Integer>();
		HashMap<Config.Identifier, HashMap<Letter, HashMap<ArrayList<String>, Double>>> results =
				new HashMap<Config.Identifier, HashMap<Letter, HashMap<ArrayList<String>, Double>>>();
		HashMap<Letter, float[]> proportionalInputs = new HashMap<Letter, float[]>();
		HashMap<Letter, float[]> squareInputs = new HashMap<Letter, float[]>();
		for (Column col : result.columns) {
			Histogram sizeHistogram = new Histogram(100);
			for (Line line : col.lines) { for (Word word : line.words) { for (Letter letter : word.letters) {
				sizeHistogram.add((int) Math.sqrt(letter.width * letter.height));
			}}}
			sizeHistogram.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
			int avgLetterSize = sizeHistogram.secondPeakOrFirstIfUnavailable();
			col.metadata.put(AVG_LETTER_SIZE, avgLetterSize);
			votes.clear();
			results.clear();
			proportionalInputs.clear();
			squareInputs.clear();
			for (Config.Identifier identifier : config.identifiers) {
				if (identifier.root) { votes.put(identifier, 0); }
			}
			int loop = 0;
			int votesGiven = 0;
			for (Line line : col.lines) { for (Word w : line.words) { for (Letter letter : w.letters) {
				runRootIdentifiers(letter, result, intensityAdjustment, proportionalInputs,
						squareInputs, votes, results);
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
			col.metadata.put(IDENTIFIER_USED, bestIdentifier);
			for (Line line : col.lines) { for (Word w : line.words) { for (Letter letter : w.letters) {
				letter.setScores(results.get(bestIdentifier).get(letter));
				runSubIdentifiers(letter, bestIdentifier, proportionalInputs, squareInputs,
						result, intensityAdjustment);
			} } }
		}
	}
	
	public void reIdentify(Letter l, Letter source, Word word, Line line, Column col, Result result) {
		int intensityAdjustment = 0;
		if (result.metadata.containsKey("blackWhiteBoundary")) {
			int blackWhiteBoundary = Integer.parseInt(result.metadata.get("blackWhiteBoundary"));
			intensityAdjustment = (REFERENCE_INTENSITY_BOUNDARY - blackWhiteBoundary) * 3 / 4;
		}
		HashMap<Config.Identifier, HashMap<Letter, HashMap<ArrayList<String>, Double>>> results =
				new HashMap<Config.Identifier, HashMap<Letter, HashMap<ArrayList<String>, Double>>>();
		HashMap<Config.Identifier, Integer> votes = new HashMap<Config.Identifier, Integer>();
		HashMap<Letter, float[]> proportionalInputs = new HashMap<Letter, float[]>();
		HashMap<Letter, float[]> squareInputs = new HashMap<Letter, float[]>();
		votes.put(col.metadata.get(IDENTIFIER_USED), 0);
		runRootIdentifiers(l, result, intensityAdjustment, proportionalInputs, squareInputs, votes,
				results);
		l.setScores(results.get(col.metadata.get(IDENTIFIER_USED)).get(l));
		runSubIdentifiers(l, col.metadata.get(IDENTIFIER_USED), proportionalInputs, squareInputs,
				result, intensityAdjustment);
	}
	
	static int qq = 0;
	
	void runRootIdentifiers(
			Letter letter,
			Result result,
			int intensityAdjustment,
			HashMap<Letter, float[]> proportionalInputs,
			HashMap<Letter, float[]> squareInputs,
			HashMap<Config.Identifier, Integer> votes,
			HashMap<Config.Identifier, HashMap<Letter, HashMap<ArrayList<String>, Double>>> results)
	{
		float[] proportionalInput = null;
		float[] squareInput       = null;
		Config.Identifier identifierVote = null;
		double bestScore = -1000.0;
		for (Config.Identifier identifier : votes.keySet()) {
			if (!results.containsKey(identifier)) {
				results.put(identifier, new HashMap<Letter, HashMap<ArrayList<String>, Double>>());
			}
			HashMap<ArrayList<String>, Double> scores = new HashMap<ArrayList<String>, Double>();
			results.get(identifier).put(letter, scores);
			if (identifier instanceof Config.NNIdentifier) {
				Config.NNIdentifier id = ((Config.NNIdentifier) identifier);
				if (id.proportionalInput) {
					if (proportionalInput == null) {
						proportionalInput = Util.getInputForNN(letter, result.img, intensityAdjustment, true);
						proportionalInputs.put(letter, proportionalInput);
					}
				} else {
					if (squareInput == null) {
						squareInput = Util.getInputForNN(letter, result.img, intensityAdjustment, true);
						proportionalInputs.put(letter, squareInput);
					}
				}
				for (int i = 0; i < id.numberOfNetworks; i++) {
					float[] output = id.fastNetworks.get(i).run(id.proportionalInput
							? proportionalInput : squareInput);
					for (Config.LetterClass lc : identifier.classes) {
						for (float[] target : id.targets.get(i).get(lc)) {
							double score = score(output, target);
							double newScore =
									i == 0
									? (score / id.numberOfNetworks)
									: (scores.get(lc.members) + score / id.numberOfNetworks);
							scores.put(lc.members, newScore);
							if (newScore > bestScore) {
								bestScore = newScore;
								identifierVote = identifier;
							}
						}
					}
				}
			}
		}
		votes.put(identifierVote, votes.get(identifierVote) + 1);
	}
	
	public static final List<Class<? extends Config.Identifier>> PHASES;
	public static final double SUB_BONUS = 0.0001;
	
	static {
		List<Class<? extends Config.Identifier>> l = new ArrayList<Class<? extends Config.Identifier>>();
		l.add(Config.NumberOfPartsIdentifier.class);
		l.add(Config.NNIdentifier.class);
		l.add(Config.NearestNeighbourIdentifier.class);
		l.add(Config.TreeIdentifier.class);
		PHASES = Collections.unmodifiableList(l);
	}
	
	private void runSubIdentifiers(
			Letter l,
			Config.Identifier identifierUsed,
			HashMap<Letter, float[]> proportionalInputs,
			HashMap<Letter, float[]> squareInputs,
			Result result,
			int intensityAdjustment)
	{
		ArrayList<String> current = l.bestLetterClass();
		ArrayList<String> dismissed = new ArrayList<String>();
		System.out.print(Arrays.toString(current.toArray()));
		double baseScore = l.possibleLetters.get(current);
		l.possibleLetters.remove(current);
		boolean progress = true;
		HashMap<ArrayList<String>, Double> scores = new HashMap<ArrayList<String>, Double>();
		lp: while (progress) {
			scores.clear();
			progress = false;
			for (Class<? extends Config.Identifier> phase : PHASES) {
				for (Config.Identifier identifier : config.identifiers) {
					if (identifier.root)                                { continue; }
					if (!identifier.getClass().equals(phase))           { continue; }
					if (!identifier.fonts.equals(identifierUsed.fonts)) { continue; }
					
					int numberOfClassesWithCurrentLetters = 0;
					int numberOfClassesWithNonDismissedLetters = 0;
					for (LetterClass lc : identifier.classes) {
						for (String lcL : lc.members) {
							if (current.contains(lcL)) {
								numberOfClassesWithCurrentLetters++;
								break;
							}
						}
						if (!dismissed.containsAll(lc.members)) {
							numberOfClassesWithNonDismissedLetters++;
						}
					}
					if (numberOfClassesWithCurrentLetters == 0)      { continue; }
					if (numberOfClassesWithNonDismissedLetters < 2)  { continue; }
					
					ArrayList<String> bestClass = null;
					double bestScore = 0.0;
					if (identifier instanceof Config.NNIdentifier) {
						Config.NNIdentifier id = (Config.NNIdentifier) identifier;
						for (int i = 0; i < id.numberOfNetworks; i++) {
							float[] input;
							if (id.proportionalInput) {
								if (!proportionalInputs.containsKey(l)) {
									proportionalInputs.put(l, Util.getInputForNN(l, result.img, intensityAdjustment, true));
								}
								input = proportionalInputs.get(l);
							} else {
								if (!squareInputs.containsKey(l)) {
									squareInputs.put(l, Util.getInputForNN(l, result.img, intensityAdjustment, false));
								}
								input = squareInputs.get(l);
							}
							float[] output = id.fastNetworks.get(i).run(input);
							for (Config.LetterClass lc : identifier.classes) {
								double lcBestScore = -1;
								for (float[] target : id.targets.get(i).get(lc)) {
									double score = score(output, target);
									lcBestScore = Math.max(lcBestScore, score);
								}
								double newScore =
									i == 0
									? (lcBestScore / id.numberOfNetworks)
									: (scores.get(lc.members) + lcBestScore / id.numberOfNetworks);
								scores.put(lc.members, newScore);
								ArrayList<String> cl = new ArrayList<String>(lc.members);
								cl.removeAll(dismissed);
								if (cl.isEmpty()) { continue; }
								if (newScore > bestScore) {
									bestClass = cl;
									bestScore = newScore;
								}
							}
						}
						if (scores.isEmpty()) {
							bestClass = null;
							bestScore = 0.0;
							continue;
						}
						progress = true;
					}
					
					if (identifier instanceof Config.NumberOfPartsIdentifier) {
						Config.NumberOfPartsIdentifier id = (Config.NumberOfPartsIdentifier) identifier;
						
						ArrayList<String> cl0 = new ArrayList<String>(identifier.classes.get(0).members);
						cl0.removeAll(dismissed);
						if (cl0.isEmpty()) { continue; }
						
						ArrayList<String> cl1 = new ArrayList<String>(identifier.classes.get(1).members);
						cl1.removeAll(dismissed);
						if (cl1.isEmpty()) { continue; }
						
						int n = l.components.isEmpty() ? 1 : l.components.size();
						boolean aboveBoundary = n > id.numberOfPartsBoundary;
						if (aboveBoundary == id.firstIsAboveBoundary) {
							bestClass = cl0;
							bestScore = 1.0;
							scores.put(cl0, 1.0);
							scores.put(cl1, 0.0);
						} else {
							bestClass = cl1;
							bestScore = 1.0;
							scores.put(cl0, 0.0);
							scores.put(cl1, 1.0);
						}
						progress = true;
					}
					
					if (identifier instanceof Config.NearestNeighbourIdentifier) {
						if (!squareInputs.containsKey(l)) {
							squareInputs.put(l, Util.getInputForNN(l, result.img, intensityAdjustment, false));
						}
						float[] input = squareInputs.get(l);
						for (LetterClass lc : identifier.classes) {
							ArrayList<String> cl = new ArrayList<String>(lc.members);
							cl.removeAll(dismissed);
							if (cl.isEmpty()) { continue; }
							double leastErr = ((Config.NearestNeighbourIdentifier) identifier).comparisons.leastError(cl, input);
							double score = 1.0 - leastErr / 1024;
							scores.put(cl, score);
							if (score > bestScore) {
								bestClass = cl;
								bestScore = score;
							}
						}
						progress = true;
					}
					
					if (identifier instanceof Config.TreeIdentifier) {
						Config.TreeIdentifier id = (Config.TreeIdentifier) identifier;
						LetterClass winner = TreePredict.vote(id.tree.classify(
								TreePredict.getImg(l, result.img, intensityAdjustment)));
						bestClass = new ArrayList<String>(winner.members);
						bestScore = 1.0;
						for (LetterClass lc : id.classes) {
							if (lc == winner) {
								scores.put(new ArrayList<String>(lc.members), 1.0);
							} else {
								scores.put(new ArrayList<String>(lc.members), 0.0);
							}
						}
						progress = true;
					}
					
					scores.remove(bestClass);
					
					losers: for (Map.Entry<ArrayList<String>, Double> e : scores.entrySet()) {
						ArrayList<String> loserClass = new ArrayList<String>(e.getKey());
						for (String lcL : loserClass) {
							if (!dismissed.contains(lcL)) { dismissed.add(lcL); }
						}
						for (ArrayList<String> preExisting : l.possibleLetters.keySet()) {
							loserClass.removeAll(preExisting);
							if (loserClass.isEmpty()) { continue losers; }
						}
						l.possibleLetters.put(loserClass,
								baseScore + (e.getValue() / bestScore) * SUB_BONUS);
					}
					current = bestClass;
					baseScore += SUB_BONUS;
					System.out.print(" -> " + Arrays.toString(current.toArray()));
					if (progress) {
						continue lp;
					}
				}
			}
		}
		
		// if current size > 1, apply aspect/size discrimination
		if (current.size() > 1) {
			String best = null;
			double bestDist = Double.MAX_VALUE;
			for (String candidate : current) {
				double szDist = l.relativeSize / identifierUsed.expectedRelativeSizes.get(candidate);
				if (szDist < 1) { szDist = 1 / szDist; }
				double arDist = Math.sqrt(l.width * 1.0 / l.height) / identifierUsed.expectedAspectRatios.get(candidate);
				if (arDist < 1) { arDist = 1 / arDist; }
				double dist = szDist + ASPECT_TO_SIZE_DIST_RATIO * arDist;
				if (dist < bestDist) {
					bestDist = dist;
					best = candidate;
				}
			}
			
			ArrayList<String> others = new ArrayList<String>(current);
			others.remove(best);
			l.possibleLetters.put(others, baseScore + SUB_BONUS);
			current = new ArrayList<String>();
			current.add(best);
			baseScore += SUB_BONUS;
			System.out.print(" -> " + Arrays.toString(current.toArray()));
		}
		
		l.possibleLetters.put(current, baseScore + SUB_BONUS);
		System.out.println();
	}
		
	public void discriminateTopLetterClass(Letter l, Letter source, Word word, Line line, Column column, Result result) {
		int intensityAdjustment = 0;
		if (result.metadata.containsKey("blackWhiteBoundary")) {
			int blackWhiteBoundary = Integer.parseInt(result.metadata.get("blackWhiteBoundary"));
			intensityAdjustment = (REFERENCE_INTENSITY_BOUNDARY - blackWhiteBoundary) * 3 / 4;
		}
		runSubIdentifiers(l, column.metadata.get(IDENTIFIER_USED), new HashMap<Letter, float[]>(),
				new HashMap<Letter, float[]>(), result, intensityAdjustment);
	}
	
	public void finish() {
		
	}
	
	public static float score(float[] output, float[] target) {
		float error = 0.0f;
		for (int i = 0; i < output.length; i++) {
			error += (output[i] - target[i]) * (output[i] - target[i]);
		}
		return (output.length - error) / output.length;
	}
}
