package com.zarkonnen.longan.data;

import java.awt.Rectangle;

public class Picture {
	public Rectangle location;
	public boolean[][] mask;

	public Picture(Rectangle location, boolean[][] mask) {
		this.location = location;
		this.mask = mask;
	}
}
