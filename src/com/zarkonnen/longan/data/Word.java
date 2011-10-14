package com.zarkonnen.longan.data;

import java.awt.Rectangle;
import java.util.ArrayList;

public class Word {
	public ArrayList<Letter> letters = new ArrayList<Letter>();
	public Rectangle boundingRect = null;

	public Word() {}

	public Word(Letter l) {
		add(l);
	}
	
	public void add(Letter l) {
		letters.add(l);
		if (boundingRect == null) {
			boundingRect = new Rectangle(l);
		} else {
			boundingRect.add(l);
		}
	}

	public void regenBoundingRect() {
		boundingRect = null;
		for (Letter l : letters) {
			if (boundingRect == null) {
				boundingRect = new Rectangle(l);
			} else {
				boundingRect.add(l);
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Letter l : letters) { sb.append(l.bestLetter() == null ? "?" : l.bestLetter()); }
		return sb.toString();
	}
}
