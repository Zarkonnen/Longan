package com.metalbeetle.longan.simple;

import com.metalbeetle.longan.stage.LetterFinder;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;

public class SimpleLetterFinder implements LetterFinder {
	static final int INTENSITY_BOUNDARY = 133;
	static final int MIN_AREA = 4;

	public ArrayList<Rectangle> find(BufferedImage img) {
		int[][] scan = new int[img.getHeight()][img.getWidth()];
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				Color c = new Color(img.getRGB(x, y));
				int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
				scan[y][x] = intensity > INTENSITY_BOUNDARY ? 0 : 1;
			}
		}

		ArrayList<Rectangle> rs = new ArrayList<Rectangle>();
		int floodID = 2;
		for (int searchY = 0; searchY < img.getHeight(); searchY++) {
			for (int searchX = 0; searchX < img.getWidth(); searchX++) {
				if (scan[searchY][searchX] == 1) {
					Rectangle r = new Rectangle(searchX, searchY, 1, 1);
					LinkedList<Point> floodQueue = new LinkedList<Point>();
					floodQueue.add(new Point(searchX, searchY));
					floodFill(scan, floodQueue, r, floodID++);
					if (r.getWidth() * r.getHeight() >= MIN_AREA) {
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
		}
		
		return rs;
	}

	private void floodFill(int[][] scan, LinkedList<Point> floodQueue, Rectangle r, int floodID) {
		while (floodQueue.size() > 0) {
			Point p = floodQueue.poll();
			int y = p.y;
			int x = p.x;
			//System.out.println(floodID + ": " + x + " / " + y);
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
