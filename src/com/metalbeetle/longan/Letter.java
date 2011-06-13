package com.metalbeetle.longan;

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

import java.util.HashMap;
import java.util.Map;

public class Letter {
	public final LetterRect location;
	public final HashMap<String, Double> possibleLetters;

	public Letter(LetterRect location, HashMap<String, Double> possibleLetters) {
		this.location = location;
		this.possibleLetters = possibleLetters;
	}
	
	public String bestLetter() {
		String bestL = "";
		double bestP = 0.0;
		for (Map.Entry<String, Double> entry : possibleLetters.entrySet()) {
			if (entry.getValue() > bestP) {
				bestL = entry.getKey();
				bestP = entry.getValue();
			}
		}
		return bestL;
	}

	public double bestScore() {
		double bestP = 0.0;
		for (Map.Entry<String, Double> entry : possibleLetters.entrySet()) {
			if (entry.getValue() > bestP) {
				bestP = entry.getValue();
			}
		}
		return bestP;
	}
}
