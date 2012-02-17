package com.zarkonnen.longan.data;

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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Letter extends Rectangle {
	public final HashMap<ArrayList<String>, java.lang.Double> possibleLetters =
			new HashMap<ArrayList<String>, java.lang.Double>();
	public double relativeLineOffset = 0.0;
	public double relativeSize = 1.0;
	public boolean[][] mask;
	public boolean fragment;
	public ArrayList<Letter> components = new ArrayList<Letter>();

	public Letter(int x, int y, int width, int height) {
		super(x, y, width, height);
	}
	
	public void setScores(HashMap<ArrayList<String>, java.lang.Double> sc) {
		possibleLetters.clear();
		possibleLetters.putAll(sc);
	}
	
	public Letter add(Letter lr2) {
		Rectangle newR = new Rectangle(this);
		newR.add(lr2);
		boolean[][] newMask = new boolean[newR.height][newR.width];
		for (int my = 0; my < newR.height; my++) {
			for (int mx = 0; mx < newR.width; mx++) {
				int thisY = newR.y + my - y;
				int thisX = newR.x + mx - x;
				if (thisY >= 0 && thisY < height && thisX >= 0 && thisX < width) {
					newMask[my][mx] |= mask[thisY][thisX];
				}
				int lr2Y = newR.y + my - lr2.y;
				int lr2X = newR.x + mx - lr2.x;
				if (lr2Y >= 0 && lr2Y < lr2.height && lr2X >= 0 && lr2X < lr2.width) {
					try {
						newMask[my][mx] |= lr2.mask[lr2Y][lr2X];
					} catch (Exception e) {
						System.out.println("JAM");
					}
				}
			}
		}
		
		Letter newLR = new Letter(newR.x, newR.y, newR.width, newR.height);
		newLR.mask = newMask;
		if (components.isEmpty()) {
			newLR.components.add(this);
		} else {
			newLR.components.addAll(components);
		}
		if (lr2.components.isEmpty()) {
			newLR.components.add(lr2);
		} else {
			newLR.components.addAll(components);
		}
		newLR.relativeSize = Math.sqrt(relativeSize * relativeSize + lr2.relativeSize * lr2.relativeSize);
		newLR.relativeLineOffset = (relativeLineOffset + lr2.relativeLineOffset) / 2;
		return newLR;
	}

	public ArrayList<Letter> splitAlongXAxis(int xSplit, int splitW) {
		ArrayList<Letter> lrs = new ArrayList<Letter>();
		// Left side
		boolean[][] newMask = new boolean[height][xSplit];
		for (int my = 0; my < height; my++) {
			System.arraycopy(mask[my], 0, newMask[my], 0, xSplit);
		}
		Letter left = new Letter(x, y, xSplit, height);
		left.mask = newMask;
		left.relativeLineOffset = relativeLineOffset;
		left.relativeSize = Math.sqrt(relativeSize * xSplit / width);
		left.fragment = fragment;
		left.cropVertically();
		lrs.add(left);
		// Right side
		newMask = new boolean[height][width - xSplit - splitW];
		for (int my = 0; my < height; my++) {
			System.arraycopy(mask[my], xSplit + splitW, newMask[my], 0, width - xSplit - splitW);
		}
		Letter right = new Letter(x + xSplit + splitW, y, width - xSplit - splitW, height);
		right.mask = newMask;
		right.relativeLineOffset = relativeLineOffset;
		right.relativeSize = Math.sqrt(relativeSize * (width - xSplit - splitW) / width);
		right.fragment = fragment;
		right.cropVertically();
		lrs.add(right);
		return lrs;
	}
	
	void cropVertically() {
		int oldWidth = width;
		int oldHeight = height;
		int topCrop = 0;
		crop: while (topCrop < height) {
			for (int mx = 0; mx < width; mx++) {
				if (mask[topCrop][mx]) {
					break crop;
				}
			}
			topCrop++;
		}
		int bottomCrop = 0;
		crop: while (bottomCrop < height - topCrop) {
			for (int mx = 0; mx < width; mx++) {
				if (mask[height - bottomCrop - 1][mx]) {
					break crop;
				}
			}
			bottomCrop++;
		}
		if (topCrop != 0 || bottomCrop != 0) {
			boolean[][] newMask = new boolean[height - topCrop - bottomCrop][width];
			System.arraycopy(mask, topCrop, newMask, 0, height - topCrop - bottomCrop);
			mask = newMask;
			y += topCrop;
			height -= topCrop + bottomCrop;
			relativeLineOffset = 0; // qqDPS
			relativeSize = relativeSize * Math.sqrt(width * height) / Math.sqrt(oldWidth * oldHeight);
		}
	}
	
	public ArrayList<String> bestLetterClass() {
		ArrayList<String> bestL = null;
		double bestP = -100000000;
		for (Map.Entry<ArrayList<String>, java.lang.Double> entry : possibleLetters.entrySet()) {
			if (entry.getValue() > bestP) {
				bestL = entry.getKey();
				bestP = entry.getValue();
			}
		}
		return bestL;
	}
	
	public String bestLetter() {
		return bestLetterClass().get(0);
	}

	public double bestScore() {
		double bestScore = 0.0;
		for (double score : possibleLetters.values()) {
			if (score > bestScore) {
				bestScore = score;
			}
		}
		return bestScore;
	}
}
