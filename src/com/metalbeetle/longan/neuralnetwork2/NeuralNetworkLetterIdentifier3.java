package com.metalbeetle.longan.neuralnetwork2;

import com.metalbeetle.longan.Letter;
import com.metalbeetle.longan.LetterRect;
import com.metalbeetle.longan.neuralnetwork2.WeightSharingNanoNetwork;
import com.metalbeetle.longan.stage.LetterIdentifier;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NeuralNetworkLetterIdentifier3 implements LetterIdentifier {
	static final int NUM_NETWORKS = 7;
	final HashMap<String, ArrayList<WeightSharingNanoNetwork>> networks = new HashMap<String, ArrayList<WeightSharingNanoNetwork>>();
	
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

	public NeuralNetworkLetterIdentifier3() {
		for (String l : LETTERS) {
			ArrayList<WeightSharingNanoNetwork> nws = new ArrayList<WeightSharingNanoNetwork>();
			for (int nn = 0; nn < NUM_NETWORKS; nn++) {
				WeightSharingNanoNetwork nw = new WeightSharingNanoNetwork(nn);
				nws.add(nw);
				InputStream is = NeuralNetworkLetterIdentifier3.class.getResourceAsStream("networks/" +
					letterToFilename(l) + "-" + nn);
				try {
					NetworkIO.input(nw.nw, is);
					is.close();
				} catch (Exception e) {
					e.printStackTrace(); // qqDPS
				}
			}
			networks.put(l, nws);
		}
	}

	public Letter identify(LetterRect r, BufferedImage img) {
		HashMap<String, Double> scores = new HashMap<String, Double>();
		double[] data = getInputForNN(r, img);
		double[] results = new double[NUM_NETWORKS];
		for (Map.Entry<String, ArrayList<WeightSharingNanoNetwork>> e : networks.entrySet()) {
			for (int nn = 0; nn < NUM_NETWORKS; nn++) {
				results[nn] = e.getValue().get(nn).run(data);
			}
			Arrays.sort(results);
			double score = results[NUM_NETWORKS / 2];
			scores.put(e.getKey(), score);
		}
		return new Letter(r, scores);
	}
	
	static double[] getInputForNN(LetterRect lr, BufferedImage src) {
		BufferedImage scaledSrc = new BufferedImage(14, 14, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledSrc.getGraphics();
		g.setColor(Color.WHITE);
		g.drawImage(src, 2, 2, 12, 12,
				lr.x, lr.y, lr.x + lr.width, lr.y + lr.height, null);
		src = scaledSrc;
		double[] result = new double[kernels.length * 12 * 12 + 3];
		for (int y = 0; y < 12; y++) { for (int x = 0; x < 12; x++) {
			for (int kdy = 0; kdy < 3; kdy++) { for (int kdx = 0; kdx < 3; kdx++) {
				Color c = new Color(src.getRGB(x + kdx, y + kdy));
				double intensity = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0 / 3.0;
				for (int k = 0; k < kernels.length; k++) {
					result[k * 144 + y * 12 + x] += intensity * kernels[k][kdy][kdx];
				}
			} }
		} }
		result[result.length - 3] = Math.log(lr.width / ((double) lr.height)) * 2;
		result[result.length - 2] = Math.log(lr.relativeSize) * 2;
		result[result.length - 1] = lr.relativeLineOffset * 5;
		return result;
	}
}
