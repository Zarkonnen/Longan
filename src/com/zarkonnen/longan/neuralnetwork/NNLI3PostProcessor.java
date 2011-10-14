package com.zarkonnen.longan.neuralnetwork;

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
import java.util.HashMap;

/** Post-processor to fix the systemic errors NNLetterIdentifier3 makes. */
public class NNLI3PostProcessor implements PostProcessor {
	static final double NUDGE = 0.0001;
	
	static final HashMap<String, Double> LOWER_TO_UPPER_SIZE_BOUNDARY = new HashMap<String, Double>();
	static {
		// Values arrived at by eyeballing the {letter}-size.atr files and some fudging.
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("c", 1.25);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("m", 1.5);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("o", 1.25);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("p", 1.5);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("s", 1.25);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("u", 1.25);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("v", 1.22);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("w", 1.5);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("x", 1.25);
		LOWER_TO_UPPER_SIZE_BOUNDARY.put("z", 1.22);
	}
	
	public void process(Result result, Longan longan) {
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				for (Word word : line.words) {
					for (int i = 0; i < word.letters.size(); i++) {
						Letter l = word.letters.get(i);
						boolean first = i == 0;
						boolean last = i == word.letters.size() - 1;
						
						if (l.bestLetter().equals("M") && l.height > l.width * 2) {
							double bs = l.bestScore();
							l.possibleLetters.put("I", bs + NUDGE);
							l.possibleLetters.put("l", bs + NUDGE * 2);
							l.possibleLetters.put("M", bs * 0.8);
						}

						// l/I/f -> i
						if (l.bestLetter().matches("[lIf]") && l.components.size() >= 2) {
							l.possibleLetters.put("i", l.bestScore() + NUDGE);
						}

						// Zero identification
						if (l.bestLetter().matches("[o0]") &&
							(
								(!first && word.letters.get(i - 1).bestLetter().matches("[0-9]")) ||
								(!last && word.letters.get(i + 1).bestLetter().matches("[0-9]"))
							)
						)
						{
							l.possibleLetters.put("0", l.possibleLetters.get("0") + NUDGE);						
						} else {
							l.possibleLetters.put("0", l.possibleLetters.get("0") - NUDGE);
						}

						//for (String s : NNLetterIdentifier3.CASE_MERGED) {
						if (NNLetterIdentifier3.CASE_MERGED.contains(l.bestLetter().toLowerCase())) {
							String s = l.bestLetter().toLowerCase();
							if (l.relativeSize > LOWER_TO_UPPER_SIZE_BOUNDARY.get(s)) {
								l.possibleLetters.put(s.toUpperCase(), l.possibleLetters.get(s.toUpperCase()) + NUDGE);
							} else {
								l.possibleLetters.put(s, l.possibleLetters.get(s) + NUDGE);
							}
						}

						// Comma/single quote identification
						if (l.bestLetter().matches("[,']")) {
							l.possibleLetters.put(l.relativeLineOffset > 0 ? "," : "'",
									l.bestScore() + NUDGE);
						}
					}
				}
			}
		}
	}
}
