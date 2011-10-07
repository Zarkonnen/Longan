package com.metalbeetle.longan.better;

import com.metalbeetle.longan.LetterRect;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ColumnFinder {
	public ArrayList<Rectangle> find(ArrayList<LetterRect> rects,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
		ArrayList<LetterRect> rs2 = new ArrayList<LetterRect>(rects);
		Collections.sort(rs2, new Comparator<LetterRect>() {
			public int compare(LetterRect t, LetterRect t1) {
				return t1.width * t1.height - t.width * t.height;
			}
		});
		List<LetterRect> iq = rs2.subList(rs2.size() / 4, rs2.size() * 3 / 4);
		int w = 0;
		int h = 0;
		for (LetterRect lr : iq) {
			w += lr.width;
			h += lr.height;
		}
		w /= iq.size();
		h /= iq.size();
		
		// Create B&W map of letters.
		int scale = Math.min(w, h);
		System.out.println(scale);
		boolean[][] map = new boolean[img.getHeight() / scale][img.getWidth() / scale];
		for (LetterRect lr : rects) {
			if (lr.width > w * 4 || lr.height > h * 4) { continue; }
			for (int y = 0; y < lr.height; y++) { for (int x = 0; x < lr.width; x++) {
				//map[lr.y + y][lr.x + x] |= lr.mask[y][x];
				if ((lr.y + y) / scale >= map.length || (lr.x + x) / scale >= map[0].length) {
					continue;
				}
				map[(lr.y + y) / scale][(lr.x + x) / scale] = true;
			}}
		}
		
		return upscale(find(map, 3, 6), scale);
	}
	
	ArrayList<Rectangle> upscale(ArrayList<Rectangle> rs, int scale) {
		for (Rectangle r : rs) {
			r.x *= scale;
			r.y *= scale;
			r.width *= scale;
			r.height *= scale;
		}
		return rs;
	}
	
	ArrayList<Rectangle> find(boolean[][] map, int minWidth, int minHeight) {
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
		
		ArrayList<Rectangle> rs = new ArrayList<Rectangle>();
		for (int x = 0; x < map[0].length; x++) {
			ArrayList<Rectangle> subRs = new ArrayList<Rectangle>();
			lp: for (int y = 0; y < map.length; y++) {
				int ry = topEndpoints[y][x];
				int rx = maxLeftEndpoints[y][x];
				int w = minRightEndpoints[y][x] - rx;
				int h = y - ry + 1;
				if (w >= minWidth && h >= minHeight) {
					for (Rectangle r : subRs) {
						if (Math.abs(r.y - ry) < minHeight / 2 && Math.abs(r.x - rx) < minWidth / 2 && Math.abs(r.width - w) < minWidth / 2) {
							r.height = h;
							continue lp;
						}
					}
					subRs.add(new Rectangle(rx, ry, w, h));
				}
			}
			rs.addAll(subRs);
		}
		
		return rs;
	}
}
