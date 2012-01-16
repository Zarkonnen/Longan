package com.zarkonnen.longan.better;

import com.zarkonnen.longan.Histogram;
import com.zarkonnen.longan.stage.PreProcessor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

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

/**
 * Generates an intensity histogram of the input image to determine where the crossover point
 * between black and white is, and what shade of light gray is the default "white" of the image.
 */
public class IntensityHistogramPreProcessor implements PreProcessor {
	public static Histogram generate(BufferedImage img) {
		Histogram hg = new Histogram(256);
		
		BufferedImage img2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img2.createGraphics();
		g.drawImage(img, 0, 0, 100, 100, null);
		
		final int h = img2.getHeight();
		final int w = img2.getWidth();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				Color c = new Color(img2.getRGB(x, y));
				int intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
				hg.add(intensity);
			}
		}
		
		hg.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
		hg.convolve(new double[] { 1.0/49, 2.0/49, 3.0/49, 4.0/49, 5.0/49, 6.0/49, 7.0/49, 6.0/49, 5.0/49, 4.0/49, 3.0/49, 2.0/49, 1.0/49 });
		hg.convolve(new double[] { 3000.0 / img2.getWidth() / img2.getHeight() }); // Get rid of minor wobbles
		
		return hg;
	}
	
	public static int getBlackWhiteBoundary(Histogram hg) {
		return hg.blackWhiteBoundary();
	}
	
	public static int getStandardWhite(Histogram hg) {
		return hg.postIndexMean(getBlackWhiteBoundary(hg));
	}

	public BufferedImage process(BufferedImage img, HashMap<String, String> metadata) {
		Histogram hg = generate(img);
		/*try {
			ImageIO.write(hg.toImage(), "png", new File("/Users/zar/Desktop/hg.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		//System.exit(0);*/
		int bwb = getBlackWhiteBoundary(hg);
		if (!metadata.containsKey("blackWhiteBoundary")) {
			metadata.put("blackWhiteBoundary", "" + bwb);
		}
		if (!metadata.containsKey("standardWhite")) {
			metadata.put("standardWhite", "" + Math.min(255, hg.postIndexMean(bwb)));
		}
		return img;
	}
}
