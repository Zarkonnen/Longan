package com.metalbeetle.longan.better;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.PostProcessor;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class HeuristicPostProcessor implements PostProcessor {
	static final double NO = 0.000001;
	static final double YES = 9.9999999999;
	
	static final String[] CAPS = {
		"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
	};
	
	static final String[] NUMBERS = {
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	static final String[] NOT_INSIDE_WORDS = {
		"!", "£", "$", "%", "(", ")", "'", ",", ":", ";", "/", "?", "-",
	};
	
	public void process(
			ArrayList<ArrayList<ArrayList<Letter>>> lines,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				for (int i = 0; i < word.size(); i++) {
					Letter l = word.get(i);
					boolean first = i == 0;
					boolean last = i == word.size() - 1;
					
					// Numbers in words
					if (l.bestLetter().matches("[0-9]") &&
						(
							(!first && !word.get(i - 1).bestLetter().matches("[0-9]")) ||
							(!last && !word.get(i + 1).bestLetter().matches("[0-9]"))
						)
					)
					{
						for (String n : NUMBERS) {
							l.possibleLetters.put(n, NO);
						}
					}
					
					// Capitals not at the start of words
					if (l.bestLetter().matches("[A-Z]") && !first) {
						for (String cl : CAPS) {
							l.possibleLetters.put(cl, NO);
						}
					}
					
					// Most symbols can't be inside words.
					if (!first && !last) {
						for (String n : NOT_INSIDE_WORDS) {
							l.possibleLetters.put(n, NO);
						}
					}
				}
			}
		}
	}
}
