package com.metalbeetle.longan.better;

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

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.LetterRect;
import com.metalbeetle.longan.Longan;
import com.metalbeetle.longan.stage.PostProcessor;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Post-processor that takes low-scoring letters and checks if they're meant to be two letters.
 */
public class LetterSplittingPostProcessor implements PostProcessor {
	public void process(
			ArrayList<ArrayList<ArrayList<Letter>>> lines,
			BufferedImage img,
			HashMap<String, String> metadata,
			Longan longan)
	{
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				lp: for (int i = 0; i < word.size(); i++) {
					Letter l = word.get(i);
					if (l.bestScore() < 0.8 && l.location.numRegions == 1) {
						ArrayList<LetterRect> resegmented = resegment(l.location, img, 
								Integer.parseInt(metadata.get("letterFinderIntensityBoundary")));
						if (resegmented.size() < 2) { continue lp; }
						Collections.sort(resegmented, new XComparator());
						ArrayList<Letter> ls = new ArrayList<Letter>();
						for (LetterRect lr : resegmented) {
							Letter newL = longan.letterIdentifier.identify(lr, img, metadata);
							if (newL.bestScore() < 0.9 || newL.bestLetter().equals(l.bestLetter())) {
								continue lp;
							}
							ls.add(newL);
						}
						/*System.out.print("Resegmented " + l.bestLetter() + " as");
						for (Letter nl : ls) { System.out.print(" " + nl.bestLetter()); }
						System.out.println();*/
						word.remove(i);
						word.addAll(i, ls);
						i += ls.size() - 1;
					}
				}
			}
		}
	}
	
	private ArrayList<LetterRect> resegment(LetterRect r, BufferedImage img, int boundary) {
		ArrayList<LetterRect> rs = null;
		do {
			boundary -= 10;
			rs = segment(r, img, boundary);
		} while (rs.size() < 2 && boundary > 0);
		return rs;
	}
	
	private ArrayList<LetterRect> segment(LetterRect srcR, BufferedImage img, int boundary) {
		int[][] scan = new int[srcR.height][srcR.width];
		for (int y = 0; y < srcR.height; y++) {
			for (int x = 0; x < srcR.width; x++) {
				Color c = new Color(img.getRGB(srcR.x + x, srcR.y + y));
				int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
				scan[y][x] = intensity > boundary ? 0 : 1;
			}
		}
		
		ArrayList<LetterRect> rs = new ArrayList<LetterRect>();
		int floodID = 2;
		for (int searchY = 0; searchY < srcR.height; searchY++) {
			for (int searchX = 0; searchX < srcR.width; searchX++) {
				if (scan[searchY][searchX] == 1) {
					LetterRect r = new LetterRect(searchX, searchY, 1, 1);
					LinkedList<Point> floodQueue = new LinkedList<Point>();
					floodQueue.add(new Point(searchX, searchY));
					floodFill(scan, floodQueue, r, floodID++);
					/*
					if (r.x > 0) {
						r.x--;
						r.width++;
					}
					if (r.y > 0) {
						r.y--;
						r.height++;
					}
					if (r.x + r.width < srcR.width) {
						r.width++;
					}
					if (r.y + r.height < srcR.height) {
						r.height++;
					}
					r.x += srcR.x;
					r.y += srcR.y;
					 * 
					 */
					rs.add(r);
				}
			}
		}
		
		return rs;
	}
	
	private void floodFill(int[][] scan, LinkedList<Point> floodQueue, LetterRect r, int floodID) {
		while (floodQueue.size() > 0) {
			Point p = floodQueue.poll();
			int y = p.y;
			int x = p.x;
			scan[y][x] = floodID;
			r.add(x, y);
			for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
				int y2 = y + dy;
				int x2 = x + dx;
				if (y2 >= 0 && y2 < scan.length && x2 >= 0 && x2 < scan[0].length && scan[y2][x2] == 1) {
					Point p2 = new Point(x2, y2);
					if (!floodQueue.contains(p2)) { floodQueue.add(p2); }
				}
			}}
		}
		
		// Fill in mask.
		if (r.x > 0) {
			r.x--;
			r.width++;
		}
		if (r.y > 0) {
			r.y--;
			r.height++;
		}
		if (r.x + r.width < scan[0].length) {
			r.width++;
		}
		if (r.y + r.height < scan.length) {
			r.height++;
		}
		r.mask = new boolean[r.height][r.width];
		for (int my = 0; my < r.height; my++) {
			for (int mx = 0; mx < r.width; mx++) {
				r.mask[my][mx] = scan[r.y + my][r.x + mx] == floodID;
			}
		}
	}
}
