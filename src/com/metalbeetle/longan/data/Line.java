package com.metalbeetle.longan.data;

import com.metalbeetle.longan.data.Letter;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Line {
	public ArrayList<Letter> rs = new ArrayList<Letter>();
	public Rectangle boundingRect = null;

	public void add(Letter r) {
		rs.add(r);
		if (boundingRect == null) {
			boundingRect = new Rectangle(r);
		} else {
			boundingRect.add(r);
		}
	}
	
	public void remove(Letter r) {
		rs.remove(r);
		regenBoundingRect();
	}

	public void regenBoundingRect() {
		boundingRect = null;
		for (Letter lr : rs) {
			if (boundingRect == null) {
				boundingRect = new Rectangle(lr);
			} else {
				boundingRect.add(lr);
			}
		}
	}

	public int xDist(Rectangle r2) {
		if (boundingRect.x + boundingRect.width < r2.x) {
			return r2.x - boundingRect.x - boundingRect.width;
		}
		if (r2.x + r2.width < boundingRect.x) {
			return boundingRect.x - r2.x - r2.width;
		}
		return 0;
	}

	public int yDist(Rectangle r2) {
		if (boundingRect.y + boundingRect.height < r2.y) {
			return r2.y - boundingRect.y - boundingRect.height;
		}
		if (r2.y + r2.height < boundingRect.y) {
			return boundingRect.y - r2.y - r2.height;
		}
		return 0;
	}

	public double avgHeight() {
		double h = 0;
		for (Rectangle r2 : rs) {
			h += r2.height;
		}
		return h / rs.size();
	}

	public double avgWidth() {
		double w = 0;
		for (Rectangle r2 : rs) {
			w += r2.width;
		}
		return w / rs.size();
	}
}
