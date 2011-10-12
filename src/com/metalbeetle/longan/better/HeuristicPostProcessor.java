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

public class HeuristicPostProcessor implements PostProcessor {
	static final double NO = 0.000001;
	static final double YES = 9.9999999999;
	static final double NUDGE = 0.001;
	
	static final String[] CAPS = {
		"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
	};
	
	static final String[] NUMBERS = {
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	static final String[] NOT_INSIDE_WORDS = {
		"!", "£", "$", "%", "(", ")", ",", ":", ";", "/", "?", "-",
	};
	
	public void process(Result result, Longan longan) {
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				for (Word word : line.words) {
					// Check for all-capsiness and numberiness.
					int caps = 0;
					int nums = 0;
					for (Letter l : word.letters) {
						if (l.bestLetter().matches("[A-Z]")) {
							caps++;
						}
						if (l.bestLetter().matches("[0-9£$%.-/:]")) {
							nums++;
						}
					}
					boolean allCaps = caps > word.letters.size() / 2;
					boolean allNums = nums > word.letters.size() / 2;
					for (int i = 0; i < word.letters.size(); i++) {
						Letter l = word.letters.get(i);
						boolean first = i == 0;
						boolean last = i == word.letters.size() - 1;

						if (!allNums) {
							if (l.bestLetter().matches("[0-9]")) {
								for (String cl : NUMBERS) {
									l.possibleLetters.put(cl, l.possibleLetters.get(cl) * 0.8);
								}
							}
						}
						// Numbers in words
						/*if (l.bestLetter().matches("[0-9]") &&
							(
								(!first && !word.get(i - 1).bestLetter().matches("[0-9]")) ||
								(!last && !word.get(i + 1).bestLetter().matches("[0-9]"))
							)
						)
						{
							for (String n : NUMBERS) {
								l.possibleLetters.put(n, NO);
							}
						}*/

						// Capitals not at the start of words
						if (!allCaps) {
							if (l.bestLetter().matches("[A-Z]") && !first) {
								for (String cl : CAPS) {
									l.possibleLetters.put(cl, l.possibleLetters.get(cl) * 0.8);
								}
							}
						}

						// Most symbols can't be inside words.
						if (!first && !last) {
							for (String n : NOT_INSIDE_WORDS) {
								l.possibleLetters.put(n, NO);
							}
						}
					}

					// I/l at start of line / after !.? / alone, but not before i.
					if (word.letters.size() > 0 && word.letters.get(0).bestLetter().equals("l")) {
						if (word.letters.size() == 1) {
							word.letters.get(0).possibleLetters.put("I", word.letters.get(0).bestScore() + NUDGE);
						} else {
							if (!word.letters.get(1).bestLetter().equals("i")) {
								if (line.words.indexOf(word) == 0) {
									word.letters.get(0).possibleLetters.put("I", word.letters.get(0).bestScore() + NUDGE);
								} else {
									Word prevW = line.words.get(line.words.indexOf(word) - 1);
									if (!prevW.letters.isEmpty()) {
										Letter prevL = prevW.letters.get(prevW.letters.size() - 1);
										if (prevL.bestLetter().matches("[!?.]")) {
											word.letters.get(0).possibleLetters.put("I", word.letters.get(0).bestScore() + NUDGE);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
