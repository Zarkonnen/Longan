package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.LetterRect;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface Chunker {
	public ArrayList<ArrayList<ArrayList<LetterRect>>> chunk(ArrayList<LetterRect> rects, BufferedImage img);
}
