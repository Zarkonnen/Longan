package com.metalbeetle.longan;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Histogram {
	private final HashMap<Integer, Integer> hg = new HashMap<Integer, Integer>();
	
	public void add(int value) {
		if (!hg.containsKey(value)) { hg.put(value, 0); }
		hg.put(value, hg.get(value) + 1);
	}
	
	public int firstValleyEnd() {
		int max = 0;
		for (Map.Entry<Integer, Integer> e : hg.entrySet()) {
			if (e.getKey() > max) {
				max = e.getKey();
			}
		}
		
		// Find the first peak.
		int bestValue = -1;
		int firstPeak = 0;
		for (int i = 0; i <= max; i++) {
			int value = hg.containsKey(i) ? hg.get(i) : 0;
			if (value < bestValue) {
				break;
			} else {
				firstPeak = i;
				bestValue = value;
			}
		}
		
		// Find the end of the valley.
		int valleyEnd = firstPeak;
		for (int i = firstPeak + 1; i <= max; i++) {
			int value = hg.containsKey(i) ? hg.get(i) : 0;
			if (value > bestValue) {
				break;
			} else {
				valleyEnd = i;
				bestValue = value;
			}
		}
		
		return valleyEnd;
	}
	
	public BufferedImage toImage() {
		int maxKey = 0;
		int maxValue = 0;
		for (Map.Entry<Integer, Integer> e : hg.entrySet()) {
			if (e.getKey() > maxKey) {
				maxKey = e.getKey();
			}
			if (e.getValue() > maxValue) {
				maxValue = e.getValue();
			}
		}
		
		BufferedImage img = new BufferedImage(maxKey + 1, maxValue + 1, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, maxKey + 1, maxValue + 1);
		int squash = 1;
		if (maxValue > 400) {
			squash = maxValue / 400 + 1;
		}
		g.setColor(Color.BLACK);
		for (Map.Entry<Integer, Integer> e : hg.entrySet()) {
			g.fillRect(e.getKey(), maxValue - e.getValue() / squash, 1, e.getValue() / squash);
		}
		return img;
	}
}
