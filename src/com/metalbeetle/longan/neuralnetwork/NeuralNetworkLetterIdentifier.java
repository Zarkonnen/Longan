package com.metalbeetle.longan.neuralnetwork;

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
import com.metalbeetle.longan.LetterRect;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class NeuralNetworkLetterIdentifier implements LetterIdentifier {
	static final List<String> CASE_MERGED = Arrays.asList(new String[] {
		"c", "m", "o", "p", "s", "u", "v", "w", "x", "z"
	});
	
	final NeuralNetwork net;
	
	static final int OUTPUT_SIZE = 128;
	static final int REFERENCE_INTENSITY_BOUNDARY = 165;
	
	static ArrayList<Target> targets = new ArrayList<Target>();

	public NeuralNetworkLetterIdentifier() {
		net = new NeuralNetwork();
		InputStream is = NeuralNetworkLetterIdentifier.class.getResourceAsStream("data/networkweights");
		try {
			NetworkIO.input(net.nw, is);
			net.nw.freeze();
			is.close();
		} catch (Exception e) {
			e.printStackTrace(); // qqDPS
		}
		is = NeuralNetworkLetterIdentifier.class.getResourceAsStream("data/clusters");
		try {
			NetworkIO.input(targets, is, OUTPUT_SIZE);
			is.close();
		} catch (Exception e) {
			e.printStackTrace(); // qqDPS
		}
	}

	public Letter identify(LetterRect r, BufferedImage img, HashMap<String, String> metadata) {
		int intensityAdjustment = 0;
		if (metadata.containsKey("letterFinderIntensityBoundary")) {
			int letterFinderIntensityBoundary = Integer.parseInt(metadata.get("letterFinderIntensityBoundary"));
			intensityAdjustment = (REFERENCE_INTENSITY_BOUNDARY - letterFinderIntensityBoundary) * 3 / 4;
		}
		double[] data = prepare(r, img, intensityAdjustment);
		HashMap<String, Double> scores = new HashMap<String, Double>();
		double[] result = net.run(data);
		for (Target t : targets) {
			double error = 0.0;
			for (int j = 0; j < OUTPUT_SIZE; j++) {
				error += (result[j] - t.data[j]) * (result[j] - t.data[j]);
			}
			double score = (OUTPUT_SIZE - error) / OUTPUT_SIZE;
			if (scores.containsKey(t.letter)) {
				if (scores.get(t.letter) < score) {
					scores.put(t.letter, score);
				}
			} else {
				scores.put(t.letter, score);
			}
		}
		return new Letter(r, scores);
	}
	
	static int q = 1000;
	
	final BufferedImage scaledSrc = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
	
	final double[] prepare(LetterRect r, BufferedImage src, int intensityAdjustment) {
		// Masking
		BufferedImage maskedSrc = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
		Graphics g = maskedSrc.getGraphics();
		g.drawImage(
				src,
				0, 0,
				r.width, r.height,
				r.x, r.y,
				r.x + r.width, r.y + r.height,
				null);
		int white = Color.WHITE.getRGB();
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				boolean hasMask = false;
				for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
					int ny = y + dy;
					int nx = x + dx;
					if (ny >= 0 && ny < r.height && nx >= 0 && nx < r.width) {
						hasMask |= r.mask[ny][nx];
					}
				}}
				if (!hasMask) {
					maskedSrc.setRGB(x, y, white);
				}
			}
		}
		src = maskedSrc;
		/*try { ImageIO.write(src, "png", new File("/Users/zar/Desktop/drags/" + (q++) + ".png")); }
		catch (Exception e) {}*/
		g = scaledSrc.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 20, 20);
		int width = 0;
		int xOffset = 0;
		int height = 0;
		int yOffset = 0;
		if (r.width > r.height) {
			width = 12;
			height = 12 * r.height / r.width;
			yOffset = (12 - height) / 2;
		} else {
			height = 12;
			width = 12 * r.width / r.height;
			xOffset = (12 - width) / 2;
		}
		g.drawImage(
				src,
				4 + xOffset, 4 + yOffset,
				4 + xOffset + width, 4 + yOffset + height,
				0, 0,
				r.width, r.height,
				null);
		src = scaledSrc;
		double[] result = new double[20 * 20];
		for (int y = 0; y < 20; y++) { for (int x = 0; x < 20; x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * 20 + x] = (c.getRed() + c.getGreen() + c.getBlue() + intensityAdjustment * 3) / 255.0 / 1.5 - 1;
		} }
		return result;
	}
}
