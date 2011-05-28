package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.LetterRect;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface LetterFinder {
	public ArrayList<LetterRect> find(BufferedImage img);
}
