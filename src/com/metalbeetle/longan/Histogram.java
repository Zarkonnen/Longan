package com.metalbeetle.longan;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Histogram {
	private final int[] hg;

	public Histogram(int range) {
		hg = new int[range + 1];
	}
	
	public void add(int value) {
		if (value >= 0 && value < hg.length) {
			hg[value]++;
		}
	}
	
	public int firstValleyEnd() {
		// Find the first peak.
		int bestValue = -1;
		int firstPeak = 0;
		for (int i = 0; i < hg.length; i++) {
			if (hg[i] < bestValue) {
				break;
			} else {
				firstPeak = i;
				bestValue = hg[i];
			}
		}
		
		// Find the end of the valley.
		int valleyEnd = firstPeak;
		for (int i = firstPeak + 1; i < hg.length; i++) {
			if (hg[i] > bestValue) {
				break;
			} else {
				valleyEnd = i;
				bestValue = hg[i];
			}
		}
		
		return valleyEnd;
	}
	
	public BufferedImage toImage() {
		int max = 0;
		for (int i = 0; i < hg.length; i++) {
			if (hg[i] > max) { max = hg[i]; }
		}
		
		BufferedImage img = new BufferedImage(hg.length, max + 1, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, hg.length, max + 1);
		int squash = 1;
		if (max > 400) {
			squash = max / 400 + 1;
		}
		g.setColor(Color.BLACK);
		for (int i = 0; i < hg.length; i++) {
			g.fillRect(i, (max - hg[i]) / squash, 1, hg[i] / squash);
		}
		return img;
	}
}
