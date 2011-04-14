package com.metalbeetle.longan.simple;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleLetterIdentifier implements LetterIdentifier {
	public ArrayList<Letter> identify(ArrayList<Rectangle> possibleLetters, BufferedImage img) {
		ArrayList<Letter> ls = new ArrayList<Letter>();
		for (Rectangle r : possibleLetters) {
			ls.add(new Letter(r, identifyLetter(r, img)));
		}
		return ls;
	}

	HashMap<String, Double> identifyLetter(Rectangle r, BufferedImage img) {
		HashMap<String, Double> hm = new HashMap<String, Double>();
		return hm;
	}
}
