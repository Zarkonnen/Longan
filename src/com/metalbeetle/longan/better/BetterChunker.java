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

import com.metalbeetle.longan.Histogram;
import com.metalbeetle.longan.LetterRect;
import com.metalbeetle.longan.stage.Chunker;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class BetterChunker implements Chunker {
	/*
	 * Algorithm:
	 * - determine average sqrt(rect size) and std deviation
	 * - put all rects that are within 2 (?) std devs into a list
	 * - choose a rectangle and put it into a line
	 * - pick another rectangle: if it overlaps vertically with at least 10% of a line, add it to
	 *   that line, otherwise create a new line with it
	 * - once all such rects are divided up, add the outliers to whichever line is vertically
	 *   closest
	 * - sort lines to go from left to right
	 * - go over each line: if a pair of adjacent letters overlaps horizontally by at least 30%,
	 *   combine the letters into one
	 * - now calculate the average horizontal letter distance
	 * - go over each line and put all letters that are closer than average together into words
	 */

	public ArrayList<ArrayList<ArrayList<LetterRect>>> chunk(
			ArrayList<LetterRect> rects,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
		// Calculate the inter-quartile mean of sizes.
		ArrayList<Integer> sizes = new ArrayList<Integer>();
		for (Rectangle r : rects) {
			sizes.add((int) Math.sqrt(r.width * r.height));
		}
		Collections.sort(sizes);
		long sizeSum = 0;
		for (int sz : sizes.subList(sizes.size() / 4, sizes.size() * 3 / 4)) {
			sizeSum += sz;
		}
		int avgSize = (int) (sizeSum / (rects.size() / 2));
		//System.out.println("Inter-quartile mean of letter size: " + avgSize);
		ArrayList<LetterRect> wholes = new ArrayList<LetterRect>();
		ArrayList<LetterRect> pieces = new ArrayList<LetterRect>();
		for (LetterRect r : rects) {
			double size = Math.sqrt(r.width * r.height);
			if (size <= avgSize * 4.0 && r.width <= avgSize * 3.0 && r.height <= avgSize * 3.0) {
				if (size < avgSize * 0.5) {
					pieces.add(r);
				} else {
					wholes.add(r);
				}
			}
		}
		if (wholes.isEmpty()) {
			wholes.addAll(rects);
			pieces.clear();
		}
		ArrayList<Line> lines = new ArrayList<Line>();
		rects: for (LetterRect r : wholes) {
			for (Line l : lines) {
				if (l.verticalCentre >= r.y - avgSize * 0.4 &&
					l.verticalCentre <= r.y + r.height + avgSize * 0.4)
				{
					l.add(r);
					continue rects;
				}
			}
			Line l = new Line();
			l.add(r);
			lines.add(l);
		}
		for (LetterRect r : pieces) {
			double bestVDist = 100000;
			Line bestL = null;
			for (Line l : lines) {
				double vDist = Math.abs(r.getCenterY() - l.verticalCentre);
				if (bestL == null || vDist < bestVDist) {
					bestVDist = vDist;
					bestL = l;
				}
			}
			bestL.add(r);
		}
		for (Line l : lines) {
			Collections.sort(l.rs, new XComparator());
		}
		
		for (Line l : lines) {
			// Go over letter pairs and coalesce if needed.
			for (int i = 0; i < l.rs.size() - 1; i++) {
				LetterRect r0 = l.rs.get(i);
				LetterRect r1 = l.rs.get(i + 1);
				if (r0.x + r0.width < r1.x + r1.width) {
					// r0 ends before r1 does, so the two may overlap, but not completely
					int overlapPx = r0.x + r0.width - r1.x;
					// if overlapPx is negative, they don't overlap at all
					if (overlapPx < Math.min(r0.width, r1.width) * 0.2) {
						continue;
					}
				} else {
					// r0 ends after r1, so r1 is entirely within r0
				}
				// They overlap enough: coalesce.
				//System.out.println("coalescing");
				r0.add(r1);
				r0.relativeSize = Math.max(r0.relativeSize, r1.relativeSize);
				r0.numRegions = r0.numRegions + r1.numRegions;
				l.rs.remove(i + 1);
				i--;
			}
		}
		
		// Next, fill in the relativeLineOffset / relativeSize values.
		for (Line l : lines) {
			for (LetterRect r : l.rs) {
				r.relativeLineOffset = (r.getCenterY() - l.verticalCentre) / avgSize;
				r.relativeSize = Math.sqrt(r.width * r.height) / avgSize;
			}
		}
		
		// Now calculate the inter-quartile mean between-letter distances.
		Histogram hg = new Histogram(500);
		for (Line l : lines) {
			for (int i = 0; i < l.rs.size() - 1; i++) {
				Rectangle r0 = l.rs.get(i);
				Rectangle r1 = l.rs.get(i + 1);
				hg.add(r1.x - (r0.x + r0.width));
			}
		}
		//System.out.println("Histogrammed letter distance divider: " + hg.firstValleyEnd());
		int letterToWordSpacingBoundary = hg.firstValleyEnd();
		
		ArrayList<ArrayList<ArrayList<LetterRect>>> result = new ArrayList<ArrayList<ArrayList<LetterRect>>>();
		for (Line l : lines) {
			ArrayList<ArrayList<LetterRect>> rLine = new ArrayList<ArrayList<LetterRect>>();
			ArrayList<LetterRect> word = new ArrayList<LetterRect>();
			word.add(l.rs.get(0));
			for (int i = 0; i < l.rs.size() - 1; i++) {
				LetterRect r0 = l.rs.get(i);
				LetterRect r1 = l.rs.get(i + 1);
				int dist = r1.x - (r0.x + r0.width);
				if (dist > letterToWordSpacingBoundary) {
					rLine.add(word);
					word = new ArrayList<LetterRect>();
				}
				word.add(r1);
			}
			rLine.add(word);
			result.add(rLine);
		}
		
		return result;
	}
	
	static class XComparator implements Comparator<LetterRect> {
		public int compare(LetterRect r0, LetterRect r1) {
			return r0.x - r1.x;
		}
	}
	
	class Line {
		ArrayList<LetterRect> rs = new ArrayList<LetterRect>();
		int verticalCentre = 0;
		
		void add(LetterRect r) {
			rs.add(r);
			verticalCentre = 0;
			for (Rectangle r2 : rs) {
				verticalCentre += r2.getCenterY();
			}
			verticalCentre /= rs.size();
		}
		
		double tilt() {
			if (rs.size() < 2) { return 0.0; }
			/*return (rs.get(rs.size() - 1).getCenterY() - rs.get(0).getCenterY()) /
					(rs.get(rs.size() - 1).getCenterX() - rs.get(0).getCenterX() + 0.001);*/
			double dYSum = 0;
			for (int i = 0; i < rs.size() - 1; i++) {
				dYSum += (rs.get(i + 1).getCenterY() - rs.get(i).getCenterY()) /
					(rs.get(i + 1).getCenterX() - rs.get(i).getCenterX() + 1);
			}
			return dYSum / (rs.size() - 1);
		}
	}
}
