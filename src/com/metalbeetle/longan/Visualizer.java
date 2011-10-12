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

import com.metalbeetle.longan.data.Column;
import com.metalbeetle.longan.data.Letter;
import com.metalbeetle.longan.data.Line;
import com.metalbeetle.longan.data.Result;
import com.metalbeetle.longan.data.Word;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Visualizer {
	public static void visualize(Result result) {
		Graphics2D g = result.img.createGraphics();

		float thickness = (result.img.getWidth() < result.img.getHeight() ? result.img.getWidth() : result.img.getHeight()) / 500f;
		if (thickness < 1.0f) { thickness = 1.0f; }
		g.setStroke(new BasicStroke(thickness));

		// Letter positions
		g.setColor(new Color(255, 0, 0, 191));
		for (Column c : result.columns) {
			for (Line l : c.lines) {
				for (Word w : l.words) {
					for (Letter letter : w.letters) {
						g.drawRect(letter.x, letter.y,
							letter.width, letter.height);
					}
				}
			}
		}

		// Lines
		g.setColor(new Color(0, 127, 0, 191));
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				Letter prevLetter = null;
				for (Word word : line.words) {
					for (Letter letter : word.letters) {
						if (prevLetter != null) {
							g.drawLine(
								prevLetter.x + prevLetter.width / 2,
								prevLetter.y + prevLetter.height / 2,
								letter.x + letter.width / 2,
								letter.y + letter.height / 2
							);
						}
						prevLetter = letter;
					}
				}
			}
		}
		
		// Words
		g.setColor(new Color(0, 0, 255, 191));
		for (Column c : result.columns) {
			for (Line line : c.lines) {
				for (Word word : line.words) {
					Rectangle wr = word.boundingRect;
					g.drawRect(wr.x - (int) thickness * 2, wr.y - (int) thickness * 2, wr.width + (int) thickness * 4, wr.height + (int) thickness * 4);
				}
			}
		}

		g.dispose();
	}
}
