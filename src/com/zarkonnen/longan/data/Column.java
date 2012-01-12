package com.zarkonnen.longan.data;

import com.zarkonnen.longan.Metadata;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Column {
	public final Metadata metadata = new Metadata();
	public ArrayList<Line> lines = new ArrayList<Line>();
	public Rectangle boundingRect = null;
	
	public int averageXStart() {
		int x = 0;
		for (Line l : lines) {
			x += l.boundingRect.x;
		}
		return x / lines.size();
	}
	
	public void add(Line l) {
		lines.add(l);
		if (boundingRect == null) {
			boundingRect = new Rectangle(l.boundingRect);
		} else {
			boundingRect.add(l.boundingRect);
		}
	}

	public void regenBoundingRect() {
		boundingRect = null;
		for (Line l : lines) {
			if (boundingRect == null) {
				boundingRect = new Rectangle(l.boundingRect);
			} else {
				boundingRect.add(l.boundingRect);
			}
		}
	}
}
