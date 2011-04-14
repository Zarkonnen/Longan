package com.metalbeetle.longan.stage;

import com.metalbeetle.longan.Letter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface LineChunker {
	public ArrayList<ArrayList<Letter>> chunk(ArrayList<Letter> letters, BufferedImage img);
}
