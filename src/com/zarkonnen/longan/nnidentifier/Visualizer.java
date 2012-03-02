package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.nnidentifier.network.Weight;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Visualizer {
	static final int W = 1000;
	static final int H = 1000;
	
	static final int GR_SZ = 5;
	
	static void weight(Graphics2D g, int x, int y, Weight w) {
		int intensity = (int) ((w.value + 1.0f) / 2.0f * 255.0f);
		if (intensity < 0) { intensity = 0; }
		if (intensity > 255) { intensity = 255; }
		g.setColor(new Color(intensity, intensity, intensity));
		g.fillRect(x * GR_SZ, y * GR_SZ, GR_SZ, GR_SZ);
		g.setColor(Color.BLUE);
		g.drawRect(x * GR_SZ, y * GR_SZ, GR_SZ, GR_SZ);
	}
	
	public static void visualize(BufferedImage img, IdentifierNet in) {
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, W, H);
		
		// so we have 6 blocks of 25+1 in the first layah
		int weightIndex = 0;
		for (int m = 0; m < 6; m++) {
			for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
				weight(g,
						m * 7 + wX,
						wY,
						in.nw.layers.get(0).weights.get(weightIndex++)
				);
			}}
			weight(g,
					m * 7 + 5,
					0,
					in.nw.layers.get(0).weights.get(weightIndex++));
		}
		
		// Draw connections
		for (int m = 0; m < 6; m++) {
			g.setColor(Color.DARK_GRAY);
			g.drawLine(m * 7 * GR_SZ, 5 * GR_SZ, m * 7 * GR_SZ, 7 * GR_SZ);
		}
		
		// In the second layah we have 6 individual scaling factors with no bias
		weightIndex = 0;
		for (int m = 0; m < 6; m++) {
			weight(g,
					m * 7,
					8,
					in.nw.layers.get(1).weights.get(weightIndex++)
			);
		}
		
		// The next one is where it gets to the scary bit
		
		// Connect h2 to h3
		boolean X = true;
		boolean O = false;
		boolean[][] table = {
			{X, O, O, O, X, X, X, O, O, X, X, X, X, O, X, X},
			{X, X, O, O, O, X, X, X, O, O, X, X, X, X, O, X},
			{X, X, X, O, O, O, X, X, X, O, O, X, O, X, X, X},
			{O, X, X, X, O, O, X, X, X, X, O, O, X, O, X, X},
			{O, O, X, X, X, O, O, X, X, X, X, O, X, X, O, X},
			{O, O, O, X, X, X, O, O, X, X, X, X, O, X, X, X}
		};
		/*
		for (int m1 = 0; m1 < 6; m1++) {
			for (int m3 = 0; m3 < 16; m3++) {
				if (!table[m1][m3]) { continue; }
				for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
					Weight w = new Weight(rnd(-2.0f / 76, 2.0f / 76, r));
					h2.weights.add(w);
					for (int y = 0; y < 8; y++) { for (int x = 0; x < 8; x++) {
						new Connection(
							h2.nodes.get(
								m1 * 12 * 12 +
								(y + wY) * 12 +
								(x + wX)
							),
							h3.nodes.get(
								m3 * 8 * 8 +
								y * 8 +
								x
							),
							w
						);
					}}
				}}
			}
		}
		
		// Add biases
		for (int m3 = 0; m3 < 16; m3++) {
			// Add bias
			Weight w = new Weight(rnd(-2.0f / 76, 2.0f / 76, r));
			h2.weights.add(w);
			for (int y = 0; y < 8; y++) { for (int x = 0; x < 8; x++) {
				new Connection(
					biasN,
					h3.nodes.get(
						m3 * 8 * 8 +
						y * 8 +
						x
					),
					w
				);
			}}
		}
		
		// Connect h3 to h4
		for (int m3 = 0; m3 < 16; m3++) {
			Weight w = new Weight(0.25f);
			h3.weights.add(w);
			for (int y = 0; y < 4; y++) { for (int x = 0; x < 4; x++) {
				for (int dy = 0; dy < 2; dy++) { for (int dx = 0; dx < 2; dx++) {
					new Connection(
						h3.nodes.get(
							m3 * 8 * 8 +
							(y * 2 + dy) * 8 +
							(x * 2 + dx)
						),
						h4.nodes.get(
							m3 * 4 * 4 +
							y * 4 +
							x
						),
						w
					);
				}}
			}}
		}
		
		// Connect h4 to output (full connection)
		for (Node h4N : h4.nodes) {
			for (Node oN : output.nodes) {
				Weight w = new Weight(rnd(-2.0f / 193, 2.0f / 193, r));
				h4.weights.add(w);
				new Connection(h4N, oN, w);
			}
		}
		// Add biases to output.
		for (Node oN : output.nodes) {
			Weight w = new Weight(rnd(-2.0f / 193, 2.0f / 193, r));
			h4.weights.add(w);
			new Connection(biasN, oN, w);
		}*/
	}
}
