package com.zarkonnen.longan.better;

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

import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.Longan;
import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.nnidentifier.Identifier;
import com.zarkonnen.longan.stage.PostProcessor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Post-processor that takes low-scoring letters and checks if they're meant to be multiple letters,
 * by actually sawing them in half.
 */
public class AggressiveLetterSplittingPostProcessor implements PostProcessor {
	static final double LOW_SCORE_BOUNDARY  = 0.75;
	static final double RECURSE_COST        = 0.03;
	static final double SAW_WIDTH_TOLERANCE = 1.2;
	static final int    MAX_RECURSION       = 3;
	static final double MIN_IMPLAUSIBILITY_SCORE_DELTA = 0.02;
	static final double MIN_IMPROVEMENT     = 0.14;
	
	static final double MAX_SZ_DEV = 1.6;
	
	int q = 0;
	
	ArrayList<Letter> split(Letter srcLetter, Letter topLetter, Word word, Line line, Column c, Result result, Longan longan, int recursion) {
		if (recursion > MAX_RECURSION) { return null; }
		ArrayList<Letter> output = new ArrayList<Letter>();
		ArrayList<Letter> lrs = sawApart(srcLetter, result.img);
		if (lrs == null) { return null; }
		for (Letter letter : lrs) {
			if (letter.width == 0 || letter.height == 0) {
				continue;
			}
			
			longan.letterIdentifier.reIdentify(letter, topLetter, word, line, c, result);
			ArrayList<Letter> sub = split(letter, topLetter, word, line, c, result, longan, recursion + 1);
			if (sub == null) {
				output.add(letter);
			} else {
				output.addAll(sub);
			}
		}
		
		return output.isEmpty() || worstScoreIn(output, c) - RECURSE_COST < bestPlausibleScore(srcLetter, c) ? null : output;
	}
	
	double bestPlausibleScore(Letter l, Column c) {
		double score = 0.0;
		HashMap<String, Double> expectedSizes = 
				c.metadata.has(Identifier.IDENTIFIER_USED)
				? c.metadata.get(Identifier.IDENTIFIER_USED).expectedRelativeSizes
				: new HashMap<String, Double>();
		for (Map.Entry<ArrayList<String>, Double> e : l.possibleLetters.entrySet()) {
			boolean plausible = false;
			for (String pLetter : e.getKey()) {
				if (expectedSizes.containsKey(pLetter) &&
					l.relativeSize < expectedSizes.get(pLetter) * MAX_SZ_DEV)
				{
					plausible = true;
					break;
				}
			}
			if (plausible) { score = Math.max(score, e.getValue()); }
		}
		return score;
	}
	
	double worstScoreIn(ArrayList<Letter> list, Column c) {
		double ws = 1;
		for (Letter l : list) { ws = Math.min(ws, bestPlausibleScore(l, c)); }
		return ws;
	}
	
	double avgPlausibleScoreIn(ArrayList<Letter> list, Column c) {
		double acc = 0;
		for (Letter l : list) {  acc += bestPlausibleScore(l, c); }
		return acc / list.size();
	}
	
	public void process(Result result, Longan longan) {
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				for (Word word : line.words) {
					lp: for (int i = 0; i < word.letters.size(); i++) {
						Letter l = word.letters.get(i);
						double score = l.bestScore();
						double plausibleScore = bestPlausibleScore(l, c);
						boolean implausibleLetter = plausibleScore + MIN_IMPLAUSIBILITY_SCORE_DELTA < score;
						if (plausibleScore < LOW_SCORE_BOUNDARY) {
							ArrayList<Letter> newLs = split(l, l, word, line, c, result, longan, 0);
							if (newLs != null && (implausibleLetter || avgPlausibleScoreIn(newLs, c) > plausibleScore + MIN_IMPROVEMENT)) {
								word.letters.remove(i);
								word.letters.addAll(i, newLs);
								i += newLs.size() - 1;
							}
						}
					}
				}
			}
		}
	}
	
	static ArrayList<Letter> sawApart(Letter l, BufferedImage img) {
		int bestSawPosition = -1;
		int lowestResistance = Integer.MAX_VALUE;
		for (int saw = l.width / 4; saw < l.width * 3 / 4 - 1; saw++) {
			int resistance = 0;
			for (int blade = 0; blade < l.height; blade++) {
				if (l.mask[blade][saw]) {
					Color c = new Color(img.getRGB(l.x + saw, l.y + blade));
					resistance += (255 * 3 - c.getRed() - c.getGreen() - c.getBlue());
				}
			}
			if (resistance < lowestResistance) {
				lowestResistance = resistance;
				bestSawPosition = saw;
			}
		}
		if (bestSawPosition != -1) {
			// Widen the saw.
			int sawLeftWiden = 0;
			for (int saw = bestSawPosition - 1; saw > 0; saw--) {
				int resistance = 0;
				for (int blade = 0; blade < l.height; blade++) {
					if (l.mask[blade][saw]) {
						Color c = new Color(img.getRGB(l.x + saw, l.y + blade));
						resistance += (255 * 3 - c.getRed() - c.getGreen() - c.getBlue());
					}
				}
				if (resistance <= lowestResistance * SAW_WIDTH_TOLERANCE) {
					sawLeftWiden++;
				} else {
					break;
				}
			}
			int sawRightWiden = 0;
			for (int saw = bestSawPosition; saw < l.width - 2; saw++) {
				int resistance = 0;
				for (int blade = 0; blade < l.height; blade++) {
					if (l.mask[blade][saw]) {
						Color c = new Color(img.getRGB(l.x + saw, l.y + blade));
						resistance += (255 * 3 - c.getRed() - c.getGreen() - c.getBlue());
					}
				}
				if (resistance <= lowestResistance * SAW_WIDTH_TOLERANCE) {
					sawRightWiden++;
				} else {
					break;
				}
			}
			bestSawPosition -= sawLeftWiden;
			return l.splitAlongXAxis(bestSawPosition, sawLeftWiden + sawRightWiden);
		} else {
			return null;
		}
	}
}
