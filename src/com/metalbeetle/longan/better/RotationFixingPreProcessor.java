package com.metalbeetle.longan.better;

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

import com.metalbeetle.longan.Histogram;
import com.metalbeetle.longan.stage.PreProcessor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class RotationFixingPreProcessor implements PreProcessor {
	static final int TILT_RANGE = 40;
	static final double TILT_DELTA = 0.1 / 180 * Math.PI;
	static final double MIN_ADJUST = 0.4 / 180 * Math.PI;
	static final int IMAGE_H = 400;
	
	public BufferedImage process(BufferedImage img, HashMap<String, String> metadata) {
		double rotation = determineRotation(img, 0.0);
		if (Math.abs(rotation) > MIN_ADJUST) {
			BufferedImage img2 = new BufferedImage(img.getWidth() * 110 / 100, img.getHeight() * 110 / 100, img.getType());
			Graphics2D g2 = img2.createGraphics();
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, img2.getWidth(), img2.getHeight());
			g2.translate(img.getWidth() * 5 / 100, img.getHeight() * 5 / 100);
			g2.rotate(rotation);
			g2.drawImage(img, 0, 0, null);
			img = img2;
		}
		return img;
	}
	
	double determineRotation(BufferedImage img, double initialRotation) {
		BufferedImage out1 = new BufferedImage(IMAGE_H, IMAGE_H, BufferedImage.TYPE_INT_RGB);
		int intensityBoundary = -1;
		double bestRotation = initialRotation;
		double bestStdDev = 0.0;
		for (int t = 0; t < TILT_RANGE * 2; t++) {
			double tilt = (t - TILT_RANGE) * TILT_DELTA;
			Graphics2D g1 = out1.createGraphics();
			g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g1.setColor(Color.WHITE);
			g1.fillRect(0, 0, IMAGE_H, IMAGE_H);
			g1.translate(IMAGE_H / 2, IMAGE_H / 2);
			g1.rotate(tilt + initialRotation);
			g1.translate(-IMAGE_H / 2, -IMAGE_H / 2);
			g1.scale(((double) IMAGE_H) / img.getWidth(), ((double) IMAGE_H) / img.getHeight());
			g1.drawImage(img, 0, 0, null);
			
			if (intensityBoundary == -1) {
				Histogram hg = new Histogram(256);
				for (int y = 0; y < out1.getHeight(); y++) {
					for (int x = 0; x < out1.getWidth(); x++) {
						Color c = new Color(out1.getRGB(x, y));
						int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
						hg.add(intensity);
					}
				}

				hg.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
				hg.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
				hg.convolve(new double[] { 3000.0 / out1.getWidth() / out1.getHeight() }); // Get rid of minor wobbles

				intensityBoundary = hg.firstValleyEnd();
			}
			
			Histogram ch = new Histogram(IMAGE_H / 2);
			for (int y = 0; y < IMAGE_H * 3 / 4; y++) {
				int contactsHorizontal = 0;
				for (int x = IMAGE_H / 4; x < IMAGE_H * 3 / 4; x++) {
					Color c = new Color(out1.getRGB(x, y));
					int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
					if (intensity < intensityBoundary) {
						contactsHorizontal++;
					}
				}
				ch.add(contactsHorizontal);
			}
						
			double stdDev = ch.stdDev();
			if (stdDev > bestStdDev) {
				bestRotation = initialRotation + tilt;
				bestStdDev = stdDev;
			}
		}
		
		return bestRotation;
	}
}
