package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.LetterRect;
import java.awt.image.BufferedImage;

public interface LetterIdentifier {
	public Letter identify(LetterRect r, BufferedImage img);
}
