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
import com.zarkonnen.longan.stage.PostProcessor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Post-processor that takes low-scoring letters and checks if they're meant to be multiple letters,
 * by actually sawing them in half.
 */
public class AggressiveLetterSplittingPostProcessor implements PostProcessor {
	static final double LOW_SCORE_BOUNDARY  = 0.85;
	static final double MIN_AVG_IMPROVEMENT = 0.01;
	static final double SAW_WIDTH_TOLERANCE = 1.2;
	static final int    MAX_RECURSION       = 3;
	static final double OVERSIZE_THRESHOLD  = 1.5;
	static final double OVERSIZE_ASPECT_THRESHOLD = 1.6;
	static final double OVERSIZE_LOW_SCORE_BOUNDARY  = 0.93;
	
	int q = 0;
	
	ArrayList<Letter> split(Letter l, Letter source, Word word, Line line, Column c, Result result, Longan longan, double improvementThreshold, int recursion) {
		ArrayList<Letter> output = new ArrayList<Letter>();
		ArrayList<Letter> lrs = sawApart(l, result.img);
		if (lrs == null) { return null; }
		for (Letter letter : lrs) {
			if (letter.width == 0 || letter.height == 0) {
				continue;
			}
			
			longan.letterIdentifier.reIdentify(letter, source, word, line, c, result);
			
			double bestScore = letter.bestScore();
			if (bestScore < improvementThreshold) {
				if (recursion == MAX_RECURSION) {
					return null;
				} else {
					ArrayList<Letter> sub = split(letter, source, word, line, c, result, longan,
							improvementThreshold, recursion + 1);
					if (sub == null) {
						return null;
					} else {
						output.addAll(sub);
					}
				}
			} else {
				boolean oversize = bestScore < OVERSIZE_LOW_SCORE_BOUNDARY &&
						l.relativeSize > OVERSIZE_THRESHOLD &&
						l.width / l.height > OVERSIZE_ASPECT_THRESHOLD;
				if (oversize) {
					ArrayList<Letter> sub = split(letter, source, word, line, c, result, longan,
							bestScore, recursion + 1);
					if (sub == null) {
						output.add(letter);
					} else {
						output.addAll(sub);
					}
				} else {
					output.add(letter);
				}
			}
		}
		
		//for (int i = 0; i < recursion; i++) { System.out.print(" "); }
		//System.out.println(output);
		
		return output.isEmpty() ? null : output;
	}
	
	public void process(Result result, Longan longan) {
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				for (Word word : line.words) {
					lp: for (int i = 0; i < word.letters.size(); i++) {
						Letter l = word.letters.get(i);
						//System.out.print(l.bestLetter());
						double bestScore = l.bestScore();
						//ArrayList<Letter> ls = new ArrayList<Letter>();
						boolean oversize = bestScore < OVERSIZE_LOW_SCORE_BOUNDARY &&
							l.relativeSize > OVERSIZE_THRESHOLD &&
							l.width / l.height > OVERSIZE_ASPECT_THRESHOLD &&
							bestScore >= LOW_SCORE_BOUNDARY;
						if (bestScore < LOW_SCORE_BOUNDARY || oversize) {
							/*if (l.bestLetter().equals("-")) { System.out.println("- " + (int) (l.bestScore() * 100)); }
							ArrayList<Letter> lrs = sawApart(l, result.img);
							if (lrs == null) { continue lp; }
							double improvement = 0.0;
							for (Letter letter : lrs) {
								if (letter.width == 0 || letter.height == 0) {
									continue;
								}
								longan.letterIdentifier.reIdentify(letter, l, word, line, c, result);
								improvement += letter.bestScore() - bestScore;
								ls.add(letter);
								if (l.bestLetter().equals("-")) {
									System.out.println(letter.bestLetter() + " " + (int) (letter.bestScore() * 100));
								}
							}
							System.out.println();
							if (ls.size() > 0 && improvement / ls.size() >= MIN_AVG_IMPROVEMENT) {
								word.letters.remove(i);
								word.letters.addAll(i, ls);
								word.regenBoundingRect();
								i += ls.size() - 1;
							}*/
							ArrayList<Letter> newLs = split(l, l, word, line, c, result, longan,
									oversize ? 0.5 : (bestScore + MIN_AVG_IMPROVEMENT), 0);
							if (newLs != null) {
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
