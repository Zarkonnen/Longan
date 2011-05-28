package com.metalbeetle.longan.simple;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.PlaintextConverter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class SimpleWordPlaintextConverter implements PlaintextConverter {
	public String convert(ArrayList<ArrayList<ArrayList<Letter>>> lines, BufferedImage img) {
		StringBuilder sb = new StringBuilder();
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				for (Letter letter : word) {
					sb.append(letter.bestLetter());
				}
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
