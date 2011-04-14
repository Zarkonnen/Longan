package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.Letter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface WordChunker {
	public ArrayList<ArrayList<ArrayList<Letter>>> chunk(ArrayList<ArrayList<Letter>> lines,
			BufferedImage img);
}
