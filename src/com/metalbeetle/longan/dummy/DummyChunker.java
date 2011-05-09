package com.metalbeetle.longan.dummy;

import com.metalbeetle.longan.stage.Chunker;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class DummyChunker implements Chunker {
	public ArrayList<ArrayList<ArrayList<Rectangle>>> chunk(ArrayList<Rectangle> rects, BufferedImage img) {
		ArrayList<ArrayList<ArrayList<Rectangle>>> lines = new ArrayList<ArrayList<ArrayList<Rectangle>>>();
		for (Rectangle r : rects) {
			ArrayList<ArrayList<Rectangle>> line = new ArrayList<ArrayList<Rectangle>>();
			ArrayList<Rectangle> word = new ArrayList<Rectangle>();
			word.add(r);
			line.add(word);
			lines.add(line);
		}
		return lines;
	}
}
