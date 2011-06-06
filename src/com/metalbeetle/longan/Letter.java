package com.metalbeetle.longan;

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
