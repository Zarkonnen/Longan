package com.metalbeetle.longan.simple;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.PlaintextConverter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;

public class SimpleWordPlaintextConverter implements PlaintextConverter {
	public String convert(ArrayList<ArrayList<ArrayList<Letter>>> lines, BufferedImage img) {
		StringBuilder sb = new StringBuilder();
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				for (Letter letter : word) {
					String bestL = null;
					double bestP = 0.0;
					for (Map.Entry<String, Double> entry : letter.possibleLetters.entrySet()) {
						if (entry.getValue() > bestP) {
							bestL = entry.getKey();
							bestP = entry.getValue();
						}
					}
					if (bestL != null) {
						sb.append(bestL);
					}
				}
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
