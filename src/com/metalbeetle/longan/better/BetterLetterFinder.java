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
import com.metalbeetle.longan.stage.LetterFinder;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class BetterLetterFinder implements LetterFinder {
	public ArrayList<LetterRect> find(BufferedImage img, HashMap<String, String> metadata) {
		Histogram hg = new Histogram(256);
		
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				Color c = new Color(img.getRGB(x, y));
				int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
				hg.add(intensity);
			}
		}
		
		hg.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
		hg.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
		hg.convolve(new double[] { 3000.0 / img.getWidth() / img.getHeight() }); // Get rid of minor wobbles
		
		int intensityBoundary = hg.firstValleyEnd();
		metadata.put("letterFinderIntensityBoundary", "" + intensityBoundary); 
				
		int[][] scan = new int[img.getHeight()][img.getWidth()];
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				Color c = new Color(img.getRGB(x, y));
				int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
				scan[y][x] = intensity > intensityBoundary ? 0 : 1;
			}
		}

		ArrayList<LetterRect> rs = new ArrayList<LetterRect>();
		int floodID = 2;
		for (int searchY = 0; searchY < img.getHeight(); searchY++) {
			for (int searchX = 0; searchX < img.getWidth(); searchX++) {
				if (scan[searchY][searchX] == 1) {
					LetterRect r = new LetterRect(searchX, searchY, 1, 1);
					LinkedList<Point> floodQueue = new LinkedList<Point>();
					floodQueue.add(new Point(searchX, searchY));
					floodFill(scan, floodQueue, r, floodID++);
					if (r.x > 0) {
						r.x--;
						r.width++;
					}
					if (r.y > 0) {
						r.y--;
						r.height++;
					}
					if (r.x + r.width < img.getWidth()) {
						r.width++;
					}
					if (r.y + r.height < img.getHeight()) {
						r.height++;
					}
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
	}
}
