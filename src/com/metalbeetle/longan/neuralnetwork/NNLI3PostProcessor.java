package com.metalbeetle.longan.neuralnetwork;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.PostProcessor;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** Post-processor to fix the systemic errors NNLetterIdentifier3 makes. */
public class NNLI3PostProcessor implements PostProcessor {
	static final double NUDGE = 0.05;
	
	public void process(ArrayList<ArrayList<ArrayList<Letter>>> lines, BufferedImage img) {
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				for (int i = 0; i < word.size(); i++) {
					Letter l = word.get(i);
					boolean first = i == 0;
					boolean last = i == word.size() - 1;
					
					// Zero identification
					if (l.bestLetter().equals("o") &&
						(
							(!first && !word.get(i - 1).bestLetter().matches("[0-9]")) ||
							(!last && !word.get(i + 1).bestLetter().matches("[0-9]"))
						)
					)
					{
						l.possibleLetters.put("0", l.bestScore() + NUDGE);
					}
					
					for (String s : NNLetterIdentifier3.CASE_MERGED) {
						l.possibleLetters.put(s.toUpperCase(), l.possibleLetters.get(s.toUpperCase()) - NUDGE);
					}
					
					// Comma/single quote identification
					if (l.bestLetter().matches("[,']")) {
						l.possibleLetters.put(l.location.relativeLineOffset > 0 ? "," : "'",
								l.bestScore() + NUDGE);
					}
				}
			}
		}
	}
}
