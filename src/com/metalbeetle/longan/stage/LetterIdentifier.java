package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.Letter;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public interface LetterIdentifier {
	public Letter identify(Rectangle r, BufferedImage img);
}
