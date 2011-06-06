package com.metalbeetle.longan.neuralnetwork;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.LetterRect;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class NNLetterIdentifier3 implements LetterIdentifier {
	final Lenet4eNet net;
	
	static final int OUTPUT_SIZE = 128;
	
	static final String[] LETTERS = {
		"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
		"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
		"!", "@", "£", "$", "%", "&", "(", ")", "'", ".", ",", ":", ";", "/", "?", "+", "-",
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	static final List<String> CASE_MERGED = Arrays.asList(new String[] {
		"c", "m", "o", "p", "s", "u", "v", "w", "x", "z"
	});
	
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
		InputStream is = NeuralNetworkLetterIdentifier2.class.getResourceAsStream("data/l4edata3");
		try {
			NetworkIO.input(net.nw, is);
			is.close();
		} catch (Exception e) {
			e.printStackTrace(); // qqDPS
		}
	}

	public Letter identify(LetterRect r, BufferedImage img) {
		double[] data = prepare(r, img);
		HashMap<String, Double> scores = new HashMap<String, Double>();
		double[] result = net.run(data);
		for (int l = 0; l < LETTERS.length; l++) {
			double[] target = LETTER_TARGETS[l];
			double error = 0.0;
			for (int j = 0; j < OUTPUT_SIZE; j++) {
				error += (result[j] - target[j]) * (result[j] - target[j]);
			}
			double score = (OUTPUT_SIZE - error) / OUTPUT_SIZE;
			scores.put(LETTERS[l], score);
		}
		return new Letter(r, scores);
	}
	
	static double[] prepare(Rectangle r, BufferedImage src) {
		BufferedImage scaledSrc = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledSrc.getGraphics();
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
				r.x, r.y,
				r.x + r.width, r.y + r.height,
				null);
		src = scaledSrc;
		double[] result = new double[28 * 28];
		for (int y = 0; y < 28; y++) { for (int x = 0; x < 28; x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * 28 + x] = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0 / 1.5 - 1;
		} }
		return result;
	}
}
