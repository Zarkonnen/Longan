package com.zarkonnen.longan.better;

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

import com.zarkonnen.longan.data.Line;
import com.zarkonnen.longan.Histogram;
import com.zarkonnen.longan.data.Column;
import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.data.Word;
import com.zarkonnen.longan.stage.Chunker;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.imageio.ImageIO;

public class BetterChunker2 implements Chunker {
	static final int MAX_SIZE_OUTLIER = 10;
	static final double MAX_PIECE_H_DEVIATION = 1.0;
	static final double MAX_PIECE_W_DEVIATION = 1.5;
	static final double MAX_WHOLE_X_DIST = 1.6;
	static final double MAX_WHOLE_Y_DIST = 0.1;

	public Result chunk(
			ArrayList<Letter> letters,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
		Histogram sizeHistogram = new Histogram(100);
		for (Rectangle r : letters) {
			sizeHistogram.add((int) Math.sqrt(r.width * r.height));
		}
		
		sizeHistogram.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
		int avgSize = sizeHistogram.secondPeakOrFirstIfUnavailable();
		ArrayList<Letter> wholes = new ArrayList<Letter>();
		ArrayList<Letter> pieces = new ArrayList<Letter>();
		for (Letter r : letters) {
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
			wholes.addAll(letters);
			pieces.clear();
		}
		// Arrange the wholes into lines.
		ArrayList<Line> lines = new ArrayList<Line>();
		lp: for (Letter letter : wholes) {
			for (Line l : lines) {
				if (l.xDist(letter) < MAX_WHOLE_X_DIST * Math.max(avgSize, l.avgLetterWidth) &&
					l.yDist(letter) < MAX_WHOLE_Y_DIST * Math.max(avgSize, l.avgLetterHeight))
				{
					l.add(new Word(letter));
					continue lp;
				}
			}
			Line l = new Line();
			l.add(new Word(letter));
			lines.add(l);
		}
		
		// Fuse the lines
		lp: while (true) {
			for (Line l1 : lines) {
				for (Line l2 : lines) {
					if (l1 == l2) { continue; }
					if (l1.xDist(l2.boundingRect) < MAX_WHOLE_X_DIST * Math.max(avgSize, l1.avgLetterWidth) &&
						l1.yDist(l2.boundingRect) < MAX_WHOLE_Y_DIST * Math.max(avgSize, l1.avgLetterHeight))
					{
						for (Word word : l2.words) {
							l1.add(word);
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
			Collections.sort(l.words, new WordXComparator());
		}
		
		// Use a histogram to determine the boundary between between-word and between-letter spaces.
		Histogram hg = new Histogram(500);
		for (Line l : lines) {
			for (int i = 0; i < l.words.size() - 1; i++) {
				Rectangle r0 = l.words.get(i).letters.get(0);
				Rectangle r1 = l.words.get(i + 1).letters.get(0);
				hg.add(r1.x - (r0.x + r0.width));
			}
		}
		
		/*try {
			ImageIO.write(hg.toImage(), "png", new File("/Users/zar/Desktop/letdist.png"));
		} catch (Exception e) {
			
		}*/
		
		if (hg.count() > 100 && hg.average() > 3) {
			hg.convolve(new double[] { 0.05, 0.15, 0.2, 0.15, 0.05 });
			hg.convolve(new double[] { 100.0 / hg.count() });
		}
		
		int letterToWordSpacingBoundary = hg.firstValleyEnd() - 3; // qqDPS FUDGE
		
		metadata.put("letterToWordSpacingBoundary", "" + letterToWordSpacingBoundary); 
		
		// Now arrange lines into columns.
		Result result = new Result();
		result.img = img;
		result.metadata = metadata;
		for (Line l : lines) {
			putLineIntoColumns(result.columns, l, letterToWordSpacingBoundary);
		}
		
		/*
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
		
		*/
		
		// Add the pieces into the lines.
		for (Letter piece : pieces) {
			double bestVDist = 100000;
			Line bestL = null;
			for (Line l : lines) {
				double vDist = Math.abs(piece.getCenterY() - l.boundingRect.getCenterY());
				
				Rectangle b = l.boundingRect;
				double hDist = piece.getCenterX() < b.x
						? b.x - piece.getCenterX()
						: piece.getCenterX() > b.x + b.width
							? piece.getCenterX() - b.x - b.width
							: 0.0;
				
				// Don't add in pieces that are too far away from the line, just ignore them.
				if (vDist > l.avgLetterHeight * MAX_PIECE_H_DEVIATION || hDist > l.avgLetterWidth * MAX_PIECE_W_DEVIATION) { continue; }
				if (bestL == null || vDist < bestVDist) {
					bestVDist = vDist;
					bestL = l;
				}
			}
			if (bestL != null) {
				bestL.add(new Word(piece));
			}
		}
		
		for (Line l : lines) {
			Collections.sort(l.words, new WordXComparator());
		}
		
		for (Line l : lines) {
			// Go over letter pairs and coalesce if needed.
			for (int i = 0; i < l.words.size() - 1; i++) {
				Letter r0 = l.words.get(i).letters.get(0);
				Letter r1 = l.words.get(i + 1).letters.get(0);
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
				l.words.add(i, new Word(newR));
				l.words.remove(i + 1);
				l.words.remove(i + 1);
				i--;
			}
		}
		
		// Next, fill in the relativeLineOffset / relativeSize values.
		for (Line l : lines) {
			for (Word w : l.words) {
				Letter letter = w.letters.get(0);
				letter.relativeLineOffset = (letter.getCenterY() - l.boundingRect.getCenterY()) / avgSize;
				letter.relativeSize = Math.sqrt(letter.width * letter.height) / avgSize;
			}
		}
		
		// Agglutinate letters into words.
		for (Column col : result.columns) {
			for (Line line : col.lines) {
				ArrayList<Word> newWords = new ArrayList<Word>();
				Word currentWord = new Word(line.words.get(0).letters.get(0));
				for (int i = 0; i < line.words.size() - 1; i++) {
					Letter l0 = line.words.get(i).letters.get(0);
					Letter l1 = line.words.get(i + 1).letters.get(0);
					int dist = l1.x - (l0.x + l0.width);
					if (dist > letterToWordSpacingBoundary) {
						newWords.add(currentWord);
						currentWord = new Word(l1);
					} else {
						currentWord.add(l1);
					}
				}
				newWords.add(currentWord);
				line.words.clear();
				line.words.addAll(newWords);
			}
		}
		
		return result;
	}
	
	void putLineIntoColumns(ArrayList<Column> cols, Line l, int letterToWordSpacingBoundary) {
		Column closestCol = null;
		int closestColDist = Integer.MAX_VALUE;
		Column insertCol = null;
		for (Column col : cols) {
			int xStart = col.averageXStart();
			int d = xStart - l.words.get(0).letters.get(0).x;
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
}
