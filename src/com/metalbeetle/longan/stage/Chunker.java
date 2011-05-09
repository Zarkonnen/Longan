package com.metalbeetle.longan.stage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface Chunker {
	public ArrayList<ArrayList<ArrayList<Rectangle>>> chunk(ArrayList<Rectangle> rects, BufferedImage img);
}
