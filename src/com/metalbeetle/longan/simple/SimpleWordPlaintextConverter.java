package com.metalbeetle.longan.simple;

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

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.stage.PlaintextConverter;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class SimpleWordPlaintextConverter implements PlaintextConverter {
	public String convert(
			ArrayList<ArrayList<ArrayList<Letter>>> lines,
			BufferedImage img,
			HashMap<String, String> metadata)
	{
		StringBuilder sb = new StringBuilder();
		/*int bwi = 1000;
		try {
			ImageIO.write(img, "png", new File("/Users/zar/Desktop/re/img.png"));
		} catch (Exception e) {}*/
		for (ArrayList<ArrayList<Letter>> line : lines) {
			for (ArrayList<Letter> word : line) {
				for (Letter letter : word) {
					sb.append(letter.bestLetter());
					/*if (letter.bestScore() < 0.85) {
						sb.append("[" + bwi + "]");
						System.out.println(letter.bestLetter() + "[" + bwi + "]");
						for (Map.Entry<String, Double> e : letter.possibleLetters.entrySet()) {
							System.out.println(e.getKey() + " " + e.getValue());
						}
						System.out.println("----------------------------------------");
						BufferedImage img2 = new BufferedImage(letter.location.width,
								letter.location.height, BufferedImage.TYPE_INT_RGB);
						Graphics g = img2.getGraphics();
						g.drawImage(
							img,
							0, 0,
							letter.location.width, letter.location.height,
							letter.location.x, letter.location.y,
							letter.location.x + letter.location.width, letter.location.y + letter.location.height,
							null);
						try {
							ImageIO.write(img2, "png", new File("/Users/zar/Desktop/re/" + bwi + ".png"));
						} catch (Exception e) {}
						bwi++;
					}*/
				}
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
