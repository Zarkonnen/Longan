package com.zarkonnen.longan;

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
		/*int[] newHg = new int[hg.length + kernel.length];
		offset -= kernel.length / 2;
		for (int i = 0; i < newHg.length; i++) {
			double value = 0.0;
			for (int j = 0; j < kernel.length; j++) {
				value += kernel[j] * (i + j < hg.length ? hg[i + j] : 0);
			}
			newHg[i] = (int) value;
		}
		hg = newHg;
		 * 
		 */
		int[] newHg = new int[hg.length + kernel.length * 2];
		offset -= kernel.length / 2;
		for (int i = 0; i < newHg.length; i++) {
			double value = 0.0;
			for (int j = 0; j < kernel.length; j++) {
				int p = i + j - kernel.length;
				value += kernel[j] * (p < 0 || p >= hg.length ? 0 : hg[p]);
			}
			newHg[i] = (int) value;
		}
		hg = newHg;
	}
	
	public int postIndexMean(int index) {
		long sum = 0;
		long n = 0;
		for (int i = index - offset; i < hg.length; i++) {
			//n += hg[i];
			//sum += i;
			sum += i * hg[i];
			n += hg[i];
		}
		if (n == 0) {
			return index;
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
	
	public int maxPeakPos() {
		int p = 0;
		int pos = 0;
		for (int i = 0; i < hg.length; i++) {
			if (hg[i] > p) { p = hg[i]; pos = i; }
		}
		return pos + offset;
	}
	
	public int blackWhiteBoundary() {
		return firstValleyEnd(127);
	}
	
	public int altBlackWhiteBoundary() {
		int fwe = firstValleyEnd() - offset;
		
		// Find the last peak.
		int bestValue = -1;
		int lastPeak = hg.length - 1;
		for (int i = hg.length - 1; i >= 0; i--) {
			if (hg[i] < bestValue) {
				break;
			} else {
				lastPeak = i;
				bestValue = hg[i];
			}
		}
				
		// Now go to the left until we reach somewhere that's at least mult smaller than that peak.
		int blackWhitePoint = lastPeak - 1;
		for (; blackWhitePoint >= fwe; blackWhitePoint--) {
			if (hg[blackWhitePoint] < bestValue * 0.2) {
				break;
			}
		}
		
		// Now finally slide down from there until we hit our first increase, or until we hit
		// firstValleyEnd.
		int cmpVal = hg[blackWhitePoint];
		for (; blackWhitePoint >= fwe; blackWhitePoint--) {
			if (hg[blackWhitePoint] > cmpVal) {
				return blackWhitePoint + offset;
			} else {
				cmpVal = hg[blackWhitePoint];
			}
		}
		
		return fwe + offset;
	}
	
	public int secondPeakOrFirstIfUnavailable() {
		int fpPtr = 0;
		int best = 0;
		for (int i = 0; i < hg.length; i++) {
			if (hg[i] < best) {
				break;
			} else {
				fpPtr = i;
				best = hg[i];
			}
		}
		
		int vallPtr = fpPtr;
		for (int i = fpPtr; i < hg.length + 1; i++) {
			if (i == hg.length) { return fpPtr + offset; }
			if (hg[i] > best) {
				break;
			} else {
				vallPtr = i;
				best = hg[i];
			}
		}
		
		int spPtr = vallPtr;
		for (int i = vallPtr; i < hg.length + 1; i++) {
			if (i == hg.length) { return fpPtr + offset; }
			if (hg[i] < best) {
				break;
			} else {
				spPtr = i;
				best = hg[i];
			}
		}
		
		return spPtr + offset;
	}
	
	public int firstPeak() {
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
		return firstPeak;
	}
	
	public int firstValleyEnd() {
		return firstValleyEnd(offset);
	}
	
	public int firstValleyEnd(int atLeast) {
		atLeast -= offset;
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
			if (hg[i] > bestValue && i >= atLeast) {
				return valleyEnd + offset;
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
		int bwp = altBlackWhiteBoundary() - offset;
		int fwe = firstValleyEnd() - offset;
		int fwe127 = blackWhiteBoundary() - offset;
		int p2 = secondPeakOrFirstIfUnavailable() - offset;
		for (int i = 0; i < hg.length; i++) {
			if (i == bwp) {
				g.setColor(Color.RED);
				g.fillRect(i, 0, 1, max / squash);
			} else if (i == fwe) {
				g.setColor(Color.GREEN);
				g.fillRect(i, 0, 1, max / squash);
			} else if (i == fwe127) {
				g.setColor(Color.BLUE);
				g.fillRect(i, 0, 1, max / squash);
			}
			if (i == fwe && i == bwp) {
				g.setColor(Color.ORANGE);
				g.fillRect(i, 0, 1, max / squash);
			}
			if (i == fwe127 && i == fwe) {
				g.setColor(Color.CYAN);
				g.fillRect(i, 0, 1, max / squash);
			}
			if (i == fwe127 && i == bwp) {
				g.setColor(Color.MAGENTA);
				g.fillRect(i, 0, 1, max / squash);
			}
			g.setColor(Color.BLACK);
			g.fillRect(i, (max - hg[i]) / squash, 1, hg[i] / squash);
			if (i == p2) {
				g.setColor(new Color(127, 127, 127, 63));
				g.fillRect(i, 0, 1, max / squash);
			}
			if (i == -offset) {
				g.setColor(Color.RED);
				g.fillRect(i, 0, 1, 1);
			}
		}
		return img;
	}
}
