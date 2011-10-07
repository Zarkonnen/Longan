package com.metalbeetle.longan.better;

import com.metalbeetle.longan.LetterRect;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ColumnFinder {
	public ArrayList<Rectangle> find(ArrayList<LetterRect> rects,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
		// Create B&W map of letters.
		boolean[][] map = new boolean[img.getHeight()][img.getWidth()];
		for (LetterRect lr : rects) {
			for (int y = 0; y < lr.height; y++) { for (int x = 0; x < lr.width; x++) {
				map[lr.y + y][lr.x + x] |= lr.mask[y][x];
			}}
		}
		
		return null;
	}
	
	void find(boolean[][] map) {
		// Create pixel endpoint maps.
		int[][] leftEndpoints = new int[map.length][map[0].length];
		for (int y = 0; y < leftEndpoints.length; y++) {
			int lastEndPoint = 0;
			for (int x = 0; x < leftEndpoints[y].length; x++) {
				if (map[y][x]) {
					lastEndPoint = x + 1;
				} else {
					leftEndpoints[y][x] = lastEndPoint;
				}
			}
		}
		int[][] rightEndpoints = new int[map.length][map[0].length];
		for (int y = 0; y < rightEndpoints.length; y++) {
			int lastEndPoint = rightEndpoints[y].length;
			for (int x = rightEndpoints[y].length - 1; x >= 0; x--) {
				if (map[y][x]) {
					lastEndPoint = x;
				} else {
					rightEndpoints[y][x] = lastEndPoint;
				}
			}
		}
		int[][] topEndpoints = new int[map.length][map[0].length];
		for (int x = 0; x < topEndpoints[0].length; x++) {
			int lastEndPoint = 0;
			for (int y = 0; y < topEndpoints.length; y++) {
				if (map[y][x]) {
					lastEndPoint = y + 1;
				} else {
					topEndpoints[y][x] = lastEndPoint;
				}
			}
		}
		int[][] bottomEndpoints = new int[map.length][map[0].length];
		for (int x = 0; x < bottomEndpoints[0].length; x++) {
			int lastEndPoint = bottomEndpoints.length;
			for (int y = bottomEndpoints.length - 1; y >= 0; y--) {
				if (map[y][x]) {
					lastEndPoint = y;
				} else {
					bottomEndpoints[y][x] = lastEndPoint;
				}
			}
		}
		
		// Max/min l/r endpoints
		int[][] maxLeftEndpoints = new int[map.length][map[0].length];
		for (int x = 0; x < maxLeftEndpoints[0].length; x++) {
			int max = 0;
			for (int y = 0; y < maxLeftEndpoints.length; y++) {
				if (map[y][x]) {
					max = 0;
				} else {
					max = Math.max(max, leftEndpoints[y][x]);
					maxLeftEndpoints[y][x] = max;
				}
			}
		}
		int[][] minRightEndpoints = new int[map.length][map[0].length];
		for (int x = 0; x < minRightEndpoints[0].length; x++) {
			int min = minRightEndpoints[0].length;
			for (int y = 0; y < minRightEndpoints.length; y++) {
				if (map[y][x]) {
					min = minRightEndpoints[0].length;
				} else {
					min = Math.min(min, rightEndpoints[y][x]);
					minRightEndpoints[y][x] = min;
				}
			}
		}
		
		/*System.out.println("leftEndpoints");
		draw(leftEndpoints);
		
		System.out.println("rightEndpoints");
		draw(rightEndpoints);
		
		System.out.println("topEndpoints");
		draw(topEndpoints);
		
		System.out.println("bottomEndpoints");
		draw(bottomEndpoints);
		
		System.out.println("maxLeftEndpoints");
		draw(maxLeftEndpoints);
		
		System.out.println("minRightEndpoints");
		draw(minRightEndpoints);*/
		
		ArrayList<Rectangle> rs = new ArrayList<Rectangle>();
		for (int x = 0; x < map[0].length; x++) {
			Rectangle max = new Rectangle(0, 0, 0, 0);
			for (int y = 0; y < map.length; y++) {
				int ry = topEndpoints[y][x];
				int rx = maxLeftEndpoints[y][x];
				int w = minRightEndpoints[y][x] - rx;
				int h = y - ry + 1;
				if (w * h > max.width * max.height) {
					max.y = ry;
					max.x = rx;
					max.width = w;
					max.height = h;
				}
			}
			if (max.width * max.height > 0 && (max.width > 1 && max.height > 1) && (max.width > 2 || max.height > 2) && !rs.contains(max)) {
				rs.add(max);
			}
		}
		
		for (Rectangle r : rs) {
			System.out.println(r);
		}
		
		System.out.println();
		
		Collections.sort(rs, new Comparator<Rectangle>() {
			public int compare(Rectangle t, Rectangle t1) {
				return t1.width * t1.height - t.width * t.height;
			}
		});
		
		System.out.println(rs.get(0));
		
		System.out.println();
	}
	
	void draw(int[][] g) {
		for (int[] l : g) {
			for (int v : l) { System.out.print(v); }
			System.out.println();
		}
	}
	
	boolean contains(Rectangle r, boolean[][] map) {
		for (int y = 0; y < r.height; y++) { for (int x = 0; x < r.width; x++) {
			if (map[r.y + y][r.x + x]) { return false; }
		}}
		return true;
	}
	
	public static void main(String[] args) {
		boolean X = true;
		boolean O = false;
		boolean[][] grid = {
			{O,X,O,O,X,X,O,O,O,X},
			{O,O,O,X,X,O,O,O,O,X},
			{X,O,O,O,O,O,O,O,O,O},
			{X,O,X,O,O,O,O,O,O,X},
			{X,X,O,O,O,O,O,O,X,X},
			{X,O,X,O,O,O,O,X,O,O},
			{X,O,O,O,O,O,O,O,O,O},
			{X,O,O,O,O,X,O,O,O,X}
		};
		new ColumnFinder().find(grid);
	}
}
