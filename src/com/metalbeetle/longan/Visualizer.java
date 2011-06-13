package com.metalbeetle.longan;

/*
 * Copyright 2011 David Stark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Visualizer {
	public static void visualize(ArrayList<ArrayList<ArrayList<Letter>>> out, BufferedImage img) {
		Graphics2D g = img.createGraphics();

		float thickness = (img.getWidth() < img.getHeight() ? img.getWidth() : img.getHeight()) / 500f;
		if (thickness < 1.0f) { thickness = 1.0f; }
		g.setStroke(new BasicStroke(thickness));

		// Letter positions
		g.setColor(new Color(255, 0, 0, 191));
		for (ArrayList<ArrayList<Letter>> line : out) {
			for (ArrayList<Letter> word : line) {
				for (Letter letter : word) {
					g.drawRect(letter.location.x, letter.location.y,
							letter.location.width, letter.location.height);
				}
			}
		}

		// Lines
		g.setColor(new Color(0, 127, 0, 191));
		for (ArrayList<ArrayList<Letter>> line : out) {
			Letter prevLetter = null;
			for (ArrayList<Letter> word : line) {
				for (Letter letter : word) {
					if (prevLetter != null) {
						g.drawLine(
							prevLetter.location.x + prevLetter.location.width / 2,
							prevLetter.location.y + prevLetter.location.height / 2,
							letter.location.x + letter.location.width / 2,
							letter.location.y + letter.location.height / 2
						);
					}
					prevLetter = letter;
				}
			}
		}
		
		// Words
		g.setColor(new Color(0, 0, 255, 191));
		for (ArrayList<ArrayList<Letter>> line : out) {
			for (ArrayList<Letter> word : line) {
				Rectangle wr = null;
				for (Letter letter : word) {
					if (wr == null) {
						wr = new Rectangle(letter.location);
					} else {
						wr.add(letter.location);
					}
				}
				g.drawRect(wr.x - (int) thickness * 2, wr.y - (int) thickness * 2, wr.width + (int) thickness * 4, wr.height + (int) thickness * 4);
			}
		}

		g.dispose();
	}
}
