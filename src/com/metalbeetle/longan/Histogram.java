package com.metalbeetle.longan;

/*
 * Copyright 2011 David Stark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Histogram {
	private int[] hg;
	private int offset = 0;

	public Histogram(int range) {
		hg = new int[range];
	}
	
	public void add(int value) {
		if (value >= 0 && value < hg.length) {
			hg[value]++;
		}
	}
	
	public void set(int value, int amount) {
		if (value >= 0 && value < hg.length) {
			hg[value] = amount;
		}
	}
	
	public int count() {
		int c = 0;
		for (int b : hg) {
			c += b;
		}
		return c;
	}
	
	public void convolve(double[] kernel) {
		int[] newHg = new int[hg.length - kernel.length];
		//offset -= kernel.length - 1;
		offset -= kernel.length / 2;
		for (int i = 0; i < newHg.length; i++) {
			double value = 0.0;
			for (int j = 0; j < kernel.length; j++) {
				value += kernel[j] * hg[i + j];
			}
			newHg[i] = (int) value;
		}
		hg = newHg;
	}
	
	public int postFirstValleyMean() {
		int fwe = firstValleyEnd();
		long sum = 0;
		long n = 0;
		for (int i = fwe - offset; i < hg.length; i++) {
			sum += i * hg[i];
			n += hg[i];
		}
		int mean = (int) (sum / n);
		return mean + offset;
	}
	
	public int maxPeak() {
		int p = 0;
		for (int i = 0; i < hg.length; i++) {
			if (hg[i] > p) { p = hg[i]; }
		}
		return p;
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
				for (int j = i; j < Math.min(hg.length, i + hg.length / 10); j++) {
					if (hg[j] > bestValue * 4) {
						return valleyEnd + offset;
					}
				}
			}
			valleyEnd = i;
			bestValue = hg[i];
		}
				
		return valleyEnd + offset;
	}
	
	public double average() {
		long sum = 0;
		long count = 0;
		for (int i = 0; i < hg.length; i++) {
			sum += (i + offset) * hg[i];
			count += hg[i];
		}
		return ((double) sum) / ((double) count);
	}
	
	public double stdDev() {
		double avg = average();
		long deltas = 0;
		for (int i = 0; i < hg.length; i++) {
			deltas += hg[i] * ((i + offset) - avg) * ((i + offset) - avg);
		}
		return Math.sqrt(deltas);
	}
	
	public BufferedImage toImage() {
		int max = 0;
		for (int i = 0; i < hg.length; i++) {
			if (hg[i] > max) { max = hg[i]; }
		}
		
		int squash = 1;
		if (max > 400) {
			squash = max / 400 + 1;
		}
		BufferedImage img = new BufferedImage(hg.length, max / squash, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, hg.length, max / squash);
		g.setColor(Color.BLACK);
		int fwe = firstValleyEnd() - offset;
		for (int i = 0; i < hg.length; i++) {
			if (i == fwe) {
				g.setColor(Color.RED);
			} else {
				g.setColor(Color.BLACK);
			}
			g.fillRect(i, (max - hg[i]) / squash, 1, hg[i] / squash);
		}
		return img;
	}
}
