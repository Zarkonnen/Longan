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

import com.metalbeetle.longan.data.Line;
import com.metalbeetle.longan.Histogram;
import com.metalbeetle.longan.data.Letter;
import com.metalbeetle.longan.stage.Chunker;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class BetterChunker2 implements Chunker {
	static final int MAX_SIZE_OUTLIER = 10;
	static final double MAX_PIECE_H_DEVIATION = 1.0;
	static final double MAX_PIECE_W_DEVIATION = 1.5;
	static final double MAX_WHOLE_X_DIST = 1.6;
	static final double MAX_WHOLE_Y_DIST = 0.1;

	public ArrayList<ArrayList<ArrayList<Letter>>> chunk(
			ArrayList<Letter> rects,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
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
		ArrayList<Letter> wholes = new ArrayList<Letter>();
		ArrayList<Letter> pieces = new ArrayList<Letter>();
		for (Letter r : rects) {
			double size = Math.sqrt(r.width * r.height);
			if (size <= avgSize * MAX_SIZE_OUTLIER && r.width <= avgSize * MAX_SIZE_OUTLIER && r.height <= avgSize * MAX_SIZE_OUTLIER) {
				if (size < avgSize * 0.5) {
					pieces.add(r);
					r.fragment = true;
				} else {
					wholes.add(r);
				}
			}
		}
		if (wholes.isEmpty()) {
			wholes.addAll(rects);
			pieces.clear();
		}
		// Arrange the wholes into lines.
		ArrayList<Line> lines = new ArrayList<Line>();
		lp: for (Letter lr : wholes) {
			for (Line l : lines) {
				if (l.xDist(lr) < MAX_WHOLE_X_DIST * Math.max(avgSize, l.avgWidth()) &&
					l.yDist(lr) < MAX_WHOLE_Y_DIST * Math.max(avgSize, l.avgHeight()))
				{
					l.add(lr);
					continue lp;
				}
			}
			Line l = new Line();
			l.add(lr);
			lines.add(l);
		}
		
		// Fuse the lines
		lp: while (true) {
			for (Line l1 : lines) {
				for (Line l2 : lines) {
					if (l1 == l2) { continue; }
					if (l1.xDist(l2.boundingRect) < MAX_WHOLE_X_DIST * Math.max(avgSize, l1.avgWidth()) &&
						l1.yDist(l2.boundingRect) < MAX_WHOLE_Y_DIST * Math.max(avgSize, l1.avgHeight()))
					{
						for (Letter lr : l2.rs) {
							l1.add(lr);
						}
						lines.remove(l2);
						continue lp;
					}
				}
			}
			break;
		}
		
		// Horizontally sort the lines.
		for (Line l : lines) {
			Collections.sort(l.rs, new XComparator());
		}
		
		// Use a histogram to determine the boundary between between-word and between-letter spaces.
		Histogram hg = new Histogram(500);
		for (Line l : lines) {
			for (int i = 0; i < l.rs.size() - 1; i++) {
				Rectangle r0 = l.rs.get(i);
				Rectangle r1 = l.rs.get(i + 1);
				hg.add(r1.x - (r0.x + r0.width));
			}
		}
		
		hg.convolve(new double[] { 0.05, 0.15, 0.2, 0.15, 0.05 });
		hg.convolve(new double[] { 100.0 / hg.count() });

		int letterToWordSpacingBoundary = hg.firstValleyEnd() + 2;
		
		// Now try to subdivide lines into columns. This may need to be done more cleverly in the future.
		/*ArrayList<Column> cols = new ArrayList<Column>();
		for (Line l : lines) {
			for (int i = 0; i < l.rs.size() - 1; i++) {
				Letter r0 = l.rs.get(i);
				Letter r1 = l.rs.get(i + 1);
				int dist = r1.x - (r0.x + r0.width);
				if (dist > letterToWordSpacingBoundary * 3) {
					// Snap the line in two.
					Line newL = new Line();
					while (l.rs.size() > i + 1) {
						newL.add(l.rs.get(i + 1));
						l.rs.remove(i + 1);
					}
					putLineIntoColumns(cols, l, letterToWordSpacingBoundary);
					l = newL;
					i = 0;
				}
			}
			
			putLineIntoColumns(cols, l, letterToWordSpacingBoundary);
		}
		
		// Now some graphical horrors. qqDPS TEMPORARY
		Graphics2D g = img.createGraphics();
		g.setStroke(new BasicStroke(3.0f));
		Random r = new Random();
		for (Column c : cols) {
			//Rectangle b = c.boundingBox();
			g.setColor(new Color(r.nextInt(200), r.nextInt(200), r.nextInt(200), 160));
			for (Line l : c.lines) {
				Rectangle b = null;
				for (Letter lr : l.rs) {
					if (b == null) {
						b = new Rectangle(lr);
					} else {
						b.add(lr);
					}
				}
				g.fillRect(b.x, b.y, b.width, b.height);
			}
		}
		
		lines.clear();
		for (Column col : cols) {
			lines.addAll(col.lines);
		}*/
		
		// Add the pieces into the lines.
		for (Letter piece : pieces) {
			double bestVDist = 100000;
			Line bestL = null;
			for (Line l : lines) {
				double vDist = Math.abs(piece.getCenterY() - l.boundingRect.getCenterY());
				
				Rectangle b = null;
				for (Letter lr : l.rs) {
					if (b == null) {
						b = new Rectangle(lr);
					} else {
						b.add(lr);
					}
				}
				double hDist = piece.getCenterX() < b.x
						? b.x - piece.getCenterX()
						: piece.getCenterX() > b.x + b.width
							? piece.getCenterX() - b.x - b.width
							: 0.0;
				
				// Don't add in pieces that are too far away from the line, just ignore them.
				if (vDist > l.avgHeight() * MAX_PIECE_H_DEVIATION || hDist > l.avgWidth() * MAX_PIECE_W_DEVIATION) { continue; }
				if (bestL == null || vDist < bestVDist) {
					bestVDist = vDist;
					bestL = l;
				}
			}
			if (bestL != null) {
				bestL.add(piece);
			}
		}
		
		for (Line l : lines) {
			Collections.sort(l.rs, new XComparator());
		}
		
		for (Line l : lines) {
			// Go over letter pairs and coalesce if needed.
			for (int i = 0; i < l.rs.size() - 1; i++) {
				Letter r0 = l.rs.get(i);
				Letter r1 = l.rs.get(i + 1);
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
				Letter newR = r0.add(r1);
				l.rs.add(i, newR);
				l.rs.remove(i + 1);
				l.rs.remove(i + 1);
				i--;
			}
		}
		
		// Next, fill in the relativeLineOffset / relativeSize values.
		for (Line l : lines) {
			for (Letter lr : l.rs) {
				lr.relativeLineOffset = (lr.getCenterY() - l.boundingRect.getCenterY()) / avgSize;
				lr.relativeSize = Math.sqrt(lr.width * lr.height) / avgSize;
			}
		}
		
		ArrayList<ArrayList<ArrayList<Letter>>> result = new ArrayList<ArrayList<ArrayList<Letter>>>();
		for (Line l : lines) {
			ArrayList<ArrayList<Letter>> rLine = new ArrayList<ArrayList<Letter>>();
			ArrayList<Letter> word = new ArrayList<Letter>();
			word.add(l.rs.get(0));
			for (int i = 0; i < l.rs.size() - 1; i++) {
				Letter r0 = l.rs.get(i);
				Letter r1 = l.rs.get(i + 1);
				int dist = r1.x - (r0.x + r0.width);
				if (dist > letterToWordSpacingBoundary) {
					rLine.add(word);
					word = new ArrayList<Letter>();
				}
				word.add(r1);
			}
			rLine.add(word);
			result.add(rLine);
		}
		
		return result;
	}
	
	void putLineIntoColumns(ArrayList<Column> cols, Line l, int letterToWordSpacingBoundary) {
		Column closestCol = null;
		int closestColDist = Integer.MAX_VALUE;
		Column insertCol = null;
		for (Column col : cols) {
			int xStart = col.xStart();
			int d = xStart - l.rs.get(0).x;
			if (Math.abs(d) < closestColDist) {
				closestCol = col;
				closestColDist = Math.abs(d);
			}
			if (d > 0) {
				insertCol = col;
			}
		}
		if (closestColDist < letterToWordSpacingBoundary * 4) {
			closestCol.lines.add(l);
		} else {
			Column newCol = new Column();
			newCol.lines.add(l);
			if (insertCol == null) {
				cols.add(newCol);
			} else {
				cols.add(cols.indexOf(insertCol), newCol);
			}
		}
	}
	
	class Column {
		ArrayList<Line> lines = new ArrayList<Line>();
		
		int xStart() {
			int xAcc = 0;
			for (Line l : lines) {
				xAcc += l.rs.get(0).x;
			}
			return xAcc / lines.size();
		}
		
		Rectangle boundingBox() {
			Rectangle bb = null;
			for (Line l : lines) {
				for (Letter r : l.rs) {
					if (bb == null) {
						bb = new Rectangle(r);
					} else {
						bb.add(r);
					}
				}
			}
			return bb;
		}
	}
}
