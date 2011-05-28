package com.metalbeetle.longan.neuralnetwork;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.LetterRect;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class NeuralNetworkLetterIdentifier implements LetterIdentifier {
	final HashMap<String, MicroNetwork> networks = new HashMap<String, MicroNetwork>();
	
	static final double[][][] kernels = {
		// Identity
		{
			{ 0,  0,  0 },
			{ 0,  1,  0 },
			{ 0,  0,  0 }
		},
		// c/o detector (vgap)
		{
			{-1,  4, -1 },
			{-1, -2, -1 },
			{-1,  4, -1 }
		},
		// weird-ass kernel
		{
			{-2,  4,  2 },
			{-4,  0,  4 },
			{ 2, -4, -2 }
		},
	};
	
	static final String[] LETTERS = {
		"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
		"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
		"!", "@", "£", "$", "%", "&", "(", ")", "'", ".", ",", ":", ";", "/", "?", "+", "-",
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	static String letterToFilename(String l) {
		return
				l.equals(".")
				? "period"
				: l.equals(":")
				? "colon"
				: l.equals("/")
				? "slash"
				: l.toLowerCase().equals(l)
				? l
				: l.toLowerCase() + "-uc";
	}

	public NeuralNetworkLetterIdentifier() {
		for (String l : LETTERS) {
			MicroNetwork mn = new MicroNetwork();
			InputStream is = NeuralNetworkLetterIdentifier.class.getResourceAsStream("data/" +
					letterToFilename(l));
			try {
				NetworkIO.input(mn.nw, is);
				is.close();
			} catch (Exception e) {
				e.printStackTrace(); // qqDPS
			}
			networks.put(l, mn);
		}
	}

	public Letter identify(LetterRect r, BufferedImage img) {
		double[] data = convolve(r, img);
		HashMap<String, Double> scores = new HashMap<String, Double>();
		for (Map.Entry<String, MicroNetwork> e : networks.entrySet()) {
			scores.put(e.getKey(), e.getValue().run(data));
		}
		return new Letter(r, scores);
	}
	
	static double[] convolve(Rectangle r, BufferedImage src) {
		BufferedImage scaledSrc = new BufferedImage(14, 14, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledSrc.getGraphics();
		g.setColor(Color.WHITE);
		g.drawImage(src, 2, 2, 12, 12, r.x, r.y, r.x + r.width, r.y + r.height, null);
		src = scaledSrc;
		double[] result = new double[kernels.length * 12 * 12];
		for (int y = 0; y < 12; y++) { for (int x = 0; x < 12; x++) {
			for (int kdy = 0; kdy < 3; kdy++) { for (int kdx = 0; kdx < 3; kdx++) {
				Color c = new Color(src.getRGB(x + kdx, y + kdy));
				double intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0 / 3.0;
				for (int k = 0; k < kernels.length; k++) {
					result[k * 144 + y * 12 + x] += intensity * kernels[k][kdy][kdx];
				}
			} }
		} }
		return result;
	}
}
