package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.Letter;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface LetterIdentifier {
	public ArrayList<Letter> identify(ArrayList<Rectangle> possibleLetters, BufferedImage img);
}
