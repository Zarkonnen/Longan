package com.metalbeetle.longan.better;

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

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.LetterRect;
import com.metalbeetle.longan.Longan;
import com.metalbeetle.longan.stage.PostProcessor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Post-processor that takes low-scoring letters and checks if they're meant to be multiple letters,
 * by actually sawing them in half.
 */
public class AggressiveLetterSplittingPostProcessor implements PostProcessor {
	static final double LOW_SCORE_BOUNDARY = 0.8;
	static final double MIN_IMPROVEMENT = 0.05;
	static final double ALWAYS_ACCEPT_BOUNDARY = 0.95;
	static final double SAW_WIDTH_TOLERANCE = 1.2;
	
	int q = 0;
	
	public void process(
			ArrayList<ArrayList<ArrayList<Letter>>> lines,
			BufferedImage img,
			HashMap<String, String> metadata,
			Longan longan)
	{
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				lp: for (int i = 0; i < word.size(); i++) {
					Letter l = word.get(i);
					double bestScore = l.bestScore();
					ArrayList<Letter> ls = new ArrayList<Letter>();
					if (bestScore < LOW_SCORE_BOUNDARY) {
						ArrayList<LetterRect> lrs = sawApart(l.location, img);
						if (lrs == null) { continue lp; }
						for (LetterRect lr : lrs) {
							if (lr.width == 0 || lr.height == 0) {
								continue;
							}
							Letter newL = longan.letterIdentifier.identify(lr, img, metadata);
							if (newL.bestScore() < Math.min(ALWAYS_ACCEPT_BOUNDARY, bestScore + MIN_IMPROVEMENT) ||
								newL.bestLetter().equals(l.bestLetter()))
							{
								continue lp;
							}
							ls.add(newL);
						}
						word.remove(i);
						word.addAll(i, ls);
						i += ls.size() - 1;
					}
				}
			}
		}
	}
	
	static ArrayList<LetterRect> sawApart(LetterRect lr, BufferedImage img) {
		int bestSawPosition = -1;
		int lowestResistance = Integer.MAX_VALUE;
		for (int saw = lr.width / 4; saw < lr.width * 3 / 4 - 1; saw++) {
			int resistance = 0;
			for (int blade = 0; blade < lr.height; blade++) {
				if (lr.mask[blade][saw]) {
					Color c = new Color(img.getRGB(lr.x + saw, lr.y + blade));
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
				for (int blade = 0; blade < lr.height; blade++) {
					if (lr.mask[blade][saw]) {
						Color c = new Color(img.getRGB(lr.x + saw, lr.y + blade));
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
			for (int saw = bestSawPosition; saw < lr.width - 2; saw++) {
				int resistance = 0;
				for (int blade = 0; blade < lr.height; blade++) {
					if (lr.mask[blade][saw]) {
						Color c = new Color(img.getRGB(lr.x + saw, lr.y + blade));
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
			return lr.splitAlongXAxis(bestSawPosition, sawLeftWiden + sawRightWiden);
		} else {
			return null;
		}
	}
}
