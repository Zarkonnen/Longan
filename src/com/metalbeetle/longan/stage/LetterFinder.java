package com.metalbeetle.longan.stage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface LetterFinder {
	public ArrayList<Rectangle> find(BufferedImage img);
}
