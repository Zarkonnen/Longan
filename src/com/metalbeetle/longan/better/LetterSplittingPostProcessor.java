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

import com.metalbeetle.longan.data.Letter;
import com.metalbeetle.longan.Longan;
import com.metalbeetle.longan.data.Column;
import com.metalbeetle.longan.data.Line;
import com.metalbeetle.longan.data.Result;
import com.metalbeetle.longan.data.Word;
import com.metalbeetle.longan.stage.PostProcessor;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Post-processor that takes low-scoring letters and checks if they're meant to be multiple letters.
 */
public class LetterSplittingPostProcessor implements PostProcessor {
	static final double LOW_SCORE_BOUNDARY = 0.8;
	static final double MIN_IMPROVEMENT = 0.1;
	static final double ALWAYS_ACCEPT_BOUNDARY = 0.95;
	
	public void process(Result result, Longan longan) {
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				for (Word word : line.words) {
					lp: for (int i = 0; i < word.letters.size(); i++) {
						Letter l = word.letters.get(i);
						double bestScore = l.bestScore();
						if (bestScore < LOW_SCORE_BOUNDARY && l.components.size() > 1) {
							Collections.sort(l.components, new LetterXComparator());
							ArrayList<Letter> ls = new ArrayList<Letter>();
							for (Letter lr : l.components) {
								// Ignore fragments, they're probably what messed this up.
								if (lr.fragment) { continue; }
								Letter newL = longan.letterIdentifier.identify(lr, result);
								if (newL.bestScore() < Math.min(ALWAYS_ACCEPT_BOUNDARY, bestScore + MIN_IMPROVEMENT) ||
									newL.bestLetter().equals(l.bestLetter()))
								{
									continue lp;
								}
								ls.add(newL);
							}
							word.letters.remove(i);
							word.letters.addAll(i, ls);
							word.regenBoundingRect();
							i += ls.size() - 1;
						}
					}
				}
			}
		}
	}
}
