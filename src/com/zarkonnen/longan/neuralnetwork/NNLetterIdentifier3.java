package com.zarkonnen.longan.neuralnetwork;

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

import com.zarkonnen.longan.data.Letter;
import com.zarkonnen.longan.data.Result;
import com.zarkonnen.longan.stage.LetterIdentifier;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class NNLetterIdentifier3 implements LetterIdentifier {
	final Lenet4eNet net;
	final HashMap<String, HashMap<String, DeciderNet>> deciders = new HashMap<String, HashMap<String, DeciderNet>>();
	
	static final int OUTPUT_SIZE = 128;
	static final int REFERENCE_INTENSITY_BOUNDARY = 165;
	
	static final String[] LETTERS = {
		"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
		"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
		"!", "@", "£", "$", "%", "&", "(", ")", "'", ".", ",", ":", ";", "/", "?", "+", "-",
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	static final List<String> CASE_MERGED = Arrays.asList(new String[] {
		"c", "m", "o", "p", "s", "u", "v", "w", "x", "z"
	});
	
	static final String[][] DECIDERS = {
		{ "i", "1", "i1" },
		{ "Z", "2", "Z2" },
		{ "E", "£", "Epound" },
		{ "M", "l", "Ml" }
	};
	
	static final double[][] LETTER_TARGETS = new double[LETTERS.length][OUTPUT_SIZE];
	static {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			for (int l = 0; l < LETTERS.length; l++) {
				String s = LETTERS[l];
				byte[] digest;
				if (CASE_MERGED.contains(s.toLowerCase())) {
					 digest = md.digest(s.toLowerCase().getBytes("UTF-8"));
				} else {
					 digest = md.digest(s.getBytes("UTF-8"));
				}
				if (s.equals("0")) {
					digest = md.digest("o".getBytes("UTF-8"));
				}
				for (int i = 0; i < 16; i++) {
					for (int j = 0; j < 8; j++) {
						LETTER_TARGETS[l][i * 8 + j] = (digest[i] >>> j) & 1;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public NNLetterIdentifier3() {
		net = new Lenet4eNet();
		InputStream is = NNLetterIdentifier3.class.getResourceAsStream("data/lenet4enndata");
		try {
			NetworkIO.input(net.nw, is);
			net.nw.freeze();
			is.close();
		} catch (Exception e) {
			e.printStackTrace(); // qqDPS
			System.exit(1);
		}
		for (String[] dec : DECIDERS) {
			if (!deciders.containsKey(dec[0])) {
				deciders.put(dec[0], new HashMap<String, DeciderNet>());
			}
			DeciderNet dn = new DeciderNet();
			is = NNLetterIdentifier3.class.getResourceAsStream("data/" + dec[2]);
			try {
				NetworkIO.input(dn.nw, is);
				dn.nw.freeze();
				is.close();
			} catch (Exception e) {
				e.printStackTrace(); // qqDPS
				System.exit(1);
			}
			deciders.get(dec[0]).put(dec[1], dn);
		}
	}

	public Letter identify(Letter letter, Result result) {
		int intensityAdjustment = 0;
		if (result.metadata.containsKey("blackWhiteBoundary")) {
			int blackWhiteBoundary = Integer.parseInt(result.metadata.get("blackWhiteBoundary"));
			intensityAdjustment = (REFERENCE_INTENSITY_BOUNDARY - blackWhiteBoundary) * 3 / 4;
		}
		double[] data = prepare(letter, result.img, intensityAdjustment);
		HashMap<String, Double> scores = new HashMap<String, Double>();
		double[] output = net.run(data);
		for (int l = 0; l < LETTERS.length; l++) {
			double[] target = LETTER_TARGETS[l];
			double error = 0.0;
			for (int j = 0; j < OUTPUT_SIZE; j++) {
				error += (output[j] - target[j]) * (output[j] - target[j]);
			}
			double score = (OUTPUT_SIZE - error) / OUTPUT_SIZE;
			scores.put(LETTERS[l], score);
		}
		letter.possibleLetters.putAll(scores);
		//System.out.print(l.bestLetter());
		String bl = letter.bestLetter();
		if (deciders.containsKey(bl)) {
			//System.out.print(" <");
			double bestScore = 0.0;
			String bestAltLetter = null;
			for (String altLetter : deciders.get(bl).keySet()) {
				double res = deciders.get(bl).get(altLetter).run(data)[0];
				if (res > bestScore) {
					bestScore = res;
					bestAltLetter = altLetter;
				}
			}
			if (bestScore > 0.5) {
				double bestLScore = letter.bestScore();
				letter.possibleLetters.put(bestAltLetter, bestLScore);
				letter.possibleLetters.put(bl, bestLScore * 0.8);
			}
			/*try {
				//ImageIO.write(masked(r, img, intensityAdjustment), "png", new File("/Users/zar/Desktop/lets/" +
				//		(zzz++) + " " + bestScore + ".png"));
			} catch (Exception e) {
				e.printStackTrace();
			}*/
		}
		//System.out.println();
		return letter;
	}
	
	int zzz = 0;
	
	static int q = 1000;
	
	BufferedImage masked(Letter r, BufferedImage src, int intensityAdjustment) {
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
		return maskedSrc;
	}
	
	static double[] prepare(Letter r, BufferedImage src, int intensityAdjustment) {
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
		BufferedImage scaledSrc = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
		g = scaledSrc.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 28, 28);
		int width = 0;
		int xOffset = 0;
		int height = 0;
		int yOffset = 0;
		if (r.width > r.height) {
			width = 16;
			height = 16 * r.height / r.width;
			yOffset = (16 - height) / 2;
		} else {
			height = 16;
			width = 16 * r.width / r.height;
			xOffset = (16 - width) / 2;
		}
		g.drawImage(
				src,
				6 + xOffset, 6 + yOffset,
				6 + xOffset + width, 6 + yOffset + height,
				0, 0,
				r.width, r.height,
				null);
		/*
		g.drawImage(
				src,
				6 + xOffset, 6 + yOffset,
				6 + xOffset + width, 6 + yOffset + height,
				r.x, r.y,
				r.x + r.width, r.y + r.height,
				null);*/
		src = scaledSrc;
		double[] result = new double[28 * 28];
		for (int y = 0; y < 28; y++) { for (int x = 0; x < 28; x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * 28 + x] = (c.getRed() + c.getGreen() + c.getBlue() + intensityAdjustment * 3) / 255.0 / 1.5 - 1;
		} }
		return result;
	}
}
