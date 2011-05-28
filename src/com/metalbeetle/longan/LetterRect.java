package com.metalbeetle.longan;

import java.awt.Rectangle;

public class LetterRect extends Rectangle {
	public double relativeLineOffset = 0.0;
	public double relativeSize = 1.0;

	public LetterRect(int x, int y, int width, int height) {
		super(x, y, width, height);
	}
}
