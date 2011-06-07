package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.LetterRect;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public interface LetterFinder {
	public ArrayList<LetterRect> find(BufferedImage img, HashMap<String, String> metadata);
}
