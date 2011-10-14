package com.zarkonnen.longan.better;

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

import com.zarkonnen.longan.Histogram;
import com.zarkonnen.longan.stage.PreProcessor;
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
	static final int MAX_DIM = 280;
	
	public BufferedImage process(BufferedImage img, HashMap<String, String> metadata) {
		if (!metadata.containsKey("standardWhite") || !metadata.containsKey("blackWhiteBoundary")) {
			new IntensityHistogramPreProcessor().process(img, metadata);
		}
		int standardWhite = Integer.parseInt(metadata.get("standardWhite"));
		int blackWhiteBoundary = Integer.parseInt(metadata.get("blackWhiteBoundary"));
		double rotation = determineRotation(img, 0.0, blackWhiteBoundary);
		if (Math.abs(rotation) > MIN_ADJUST) {
			BufferedImage img2 = new BufferedImage(
					(int) (img.getWidth() * (1.0 + Math.sin(Math.abs(rotation)))),
					(int) (img.getHeight() * (1.0 + Math.sin(Math.abs(rotation)))),
					img.getType()
			);
			Graphics2D g2 = img2.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setColor(new Color(standardWhite, standardWhite, standardWhite));
			g2.fillRect(0, 0, img2.getWidth(), img2.getHeight());
			g2.translate(img2.getWidth() / 2, img2.getHeight() / 2);
			g2.rotate(rotation);
			g2.translate(-img.getWidth() / 2, -img.getHeight() / 2);
			g2.drawImage(img, 0, 0, null);
			img = img2;
		}
		return img;
	}
	
	double determineRotation(BufferedImage img, double initialRotation, int blackWhiteBoundary) {
		int w = 0;
		int h = 0;
		if (img.getWidth() > img.getHeight()) {
			w = MAX_DIM;
			h = MAX_DIM * img.getHeight() / img.getWidth();
		} else {
			w = MAX_DIM * img.getWidth() / img.getHeight();
			h = MAX_DIM;
		}
		BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D sg = scaled.createGraphics();
		sg.drawImage(img, 0, 0, w, h, null);
		BufferedImage out1 = new BufferedImage(IMAGE_H, IMAGE_H, BufferedImage.TYPE_INT_RGB);
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
			g1.translate(-w / 2, -h / 2);
			g1.drawImage(scaled, 0, 0, null);
			
			Histogram ch = new Histogram(IMAGE_H / 2);
			for (int y = 0; y < IMAGE_H * 3 / 4; y++) {
				int contactsHorizontal = 0;
				for (int x = IMAGE_H / 4; x < IMAGE_H * 3 / 4; x++) {
					Color c = new Color(out1.getRGB(x, y));
					int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
					if (intensity < blackWhiteBoundary) {
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
