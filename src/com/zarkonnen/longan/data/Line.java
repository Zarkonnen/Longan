package com.zarkonnen.longan.data;

import java.awt.Rectangle;
import java.util.ArrayList;

public class Line {
	public ArrayList<Word> words = new ArrayList<Word>();
	public Rectangle boundingRect = null;
	public double avgLetterWidth = 0.0;
	public double avgLetterHeight = 0.0;

	public void add(Word w) {
		words.add(w);
		if (boundingRect == null) {
			boundingRect = new Rectangle(w.boundingRect);
		} else {
			boundingRect.add(w.boundingRect);
		}
		avgLetterWidth = avgLetterWidth();
		avgLetterHeight = avgLetterHeight();
	}

	public void regenBoundingRect() {
		boundingRect = null;
		for (Word w : words) {
			if (boundingRect == null) {
				boundingRect = new Rectangle(w.boundingRect);
			} else {
				boundingRect.add(w.boundingRect);
			}
		}
		avgLetterWidth = avgLetterWidth();
		avgLetterHeight = avgLetterHeight();
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

	private double avgLetterHeight() {
		double h = 0;
		int n = 0;
		for (Word w : words) {
			for (Letter l : w.letters) {
				h += l.height;
			}
			n += w.letters.size();
		}
		return h / n;
	}

	private double avgLetterWidth() {
		double w = 0;
		int n = 0;
		for (Word word : words) {
			for (Letter l : word.letters) {
				w += l.width;
			}
			n += word.letters.size();
		}
		return w / n;
	}
}
