package com.metalbeetle.longan.better;

import com.metalbeetle.longan.dummy.DummyChunker;
import com.metalbeetle.longan.stage.Chunker;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

	public ArrayList<ArrayList<ArrayList<Rectangle>>> chunk(ArrayList<Rectangle> rects, BufferedImage img) {
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
		ArrayList<Rectangle> wholes = new ArrayList<Rectangle>();
		ArrayList<Rectangle> pieces = new ArrayList<Rectangle>();
		for (Rectangle r : rects) {
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
		rects: for (Rectangle r : wholes) {
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
		for (Rectangle r : pieces) {
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
				Rectangle r0 = l.rs.get(i);
				Rectangle r1 = l.rs.get(i + 1);
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
				l.rs.remove(i + 1);
				i--;
			}
		}
		
		// Now calculate the inter-quartile mean between-letter distances.
		ArrayList<Integer> distances = new ArrayList<Integer>();
		for (Line l : lines) {
			for (int i = 0; i < l.rs.size() - 1; i++) {
				Rectangle r0 = l.rs.get(i);
				Rectangle r1 = l.rs.get(i + 1);
				distances.add(r1.x - (r0.x + r0.width));
			}
		}
		long distSum = 0;
		for (int d : distances.subList(distances.size() / 4, distances.size() * 3 / 4)) {
			distSum += d;
		}
		double avgDist = ((double) distSum) / (distances.size() / 2);
		//System.out.println("Inter-quartile mean of letter distance: " + avgDist);
		
		ArrayList<ArrayList<ArrayList<Rectangle>>> result = new ArrayList<ArrayList<ArrayList<Rectangle>>>();
		for (Line l : lines) {
			ArrayList<ArrayList<Rectangle>> rLine = new ArrayList<ArrayList<Rectangle>>();
			ArrayList<Rectangle> word = new ArrayList<Rectangle>();
			word.add(l.rs.get(0));
			for (int i = 0; i < l.rs.size() - 1; i++) {
				Rectangle r0 = l.rs.get(i);
				Rectangle r1 = l.rs.get(i + 1);
				int dist = r1.x - (r0.x + r0.width);
				// Compare the letter distance to the average distance, somewhat modified by the
				// size of the first of the two letters. (Larger text: larger spacing.)
				if (dist > avgDist * (0.8 + 0.2 * Math.sqrt(r0.width * r0.height) / avgSize)) {
					rLine.add(word);
					word = new ArrayList<Rectangle>();
				}
				word.add(r1);
			}
			rLine.add(word);
			result.add(rLine);
		}
		
		return result;
	}
	
	static class XComparator implements Comparator<Rectangle> {
		public int compare(Rectangle r0, Rectangle r1) {
			return r0.x - r1.x;
		}
	}
	
	class Line {
		ArrayList<Rectangle> rs = new ArrayList<Rectangle>();
		int verticalCentre = 0;
		
		void add(Rectangle r) {
			rs.add(r);
			verticalCentre = 0;
			for (Rectangle r2 : rs) {
				verticalCentre += r2.getCenterY();
			}
			verticalCentre /= rs.size();
		}
	}
}
