package com.metalbeetle.longan.dummy;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class DummyLetterIdentifier implements LetterIdentifier {
	public Letter identify(Rectangle r, BufferedImage img) {
		HashMap<String, Double> ps = new HashMap<String, Double>();
		ps.put("a", 1.0);
		return new Letter(r, ps);
	}
}
