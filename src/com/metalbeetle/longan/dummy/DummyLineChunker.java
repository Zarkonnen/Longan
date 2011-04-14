package com.metalbeetle.longan.dummy;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.LineChunker;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class DummyLineChunker implements LineChunker {
	public ArrayList<ArrayList<Letter>> chunk(ArrayList<Letter> letters, BufferedImage img) {
		ArrayList<ArrayList<Letter>> lines = new ArrayList<ArrayList<Letter>>();
		ArrayList<Letter> line = new ArrayList<Letter>();
		for (Letter l : letters) {
			if (line.size() == 10) {
				lines.add(line);
				line = new ArrayList<Letter>();
			}
			line.add(l);
		}
		if (line.size() > 0) {
			lines.add(line);
		}
		return lines;
	}
}
