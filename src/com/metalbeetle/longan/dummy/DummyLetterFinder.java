package com.metalbeetle.longan.dummy;

import com.metalbeetle.longan.stage.LetterFinder;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class DummyLetterFinder implements LetterFinder {
	public ArrayList<Rectangle> find(BufferedImage img) {
		ArrayList<Rectangle> rs = new ArrayList<Rectangle>(100);
		for (int y = 0; y < 10; y++) { for (int x = 0; x < 10; x++) {
			rs.add(new Rectangle(
					img.getWidth() / 10 * x,
					img.getHeight() / 10 * y,
					img.getWidth() / 10,
					img.getHeight() / 10
			));
		}}
		return rs;
	}
}
