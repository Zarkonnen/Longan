package com.metalbeetle.longan.dummy;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.WordChunker;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class DummyWordChunker implements WordChunker {
	public ArrayList<ArrayList<ArrayList<Letter>>> chunk(ArrayList<ArrayList<Letter>> lines, BufferedImage img) {
		ArrayList<ArrayList<ArrayList<Letter>>> chunked = new ArrayList<ArrayList<ArrayList<Letter>>>();
		for (ArrayList<Letter> line : lines) {
			ArrayList<ArrayList<Letter>> chunkedLine = new ArrayList<ArrayList<Letter>>();
			ArrayList<Letter> word = new ArrayList<Letter>();
			for (Letter l : line) {
				if (word.size() == 2) {
					chunkedLine.add(word);
					word = new ArrayList<Letter>();
				}
				word.add(l);
			}
			if (word.size() > 0) {
				chunkedLine.add(word);
			}
			chunked.add(chunkedLine);
		}
		return chunked;
	}
}
