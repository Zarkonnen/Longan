package com.metalbeetle.longan.dummy;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class DummyLetterIdentifier implements LetterIdentifier {
	public ArrayList<Letter> identify(ArrayList<Rectangle> possibleLetters, BufferedImage img) {
		ArrayList<Letter> ls = new ArrayList<Letter>();
		for (Rectangle r : possibleLetters) {
			HashMap<String, Double> ps = new HashMap<String, Double>();
			ps.put("a", 1.0);
			ls.add(new Letter(r, ps));
		}
		return ls;
	}
}
