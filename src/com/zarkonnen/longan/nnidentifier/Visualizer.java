package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.nnidentifier.network.Network;
import com.zarkonnen.longan.nnidentifier.network.Weight;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Visualizer {
	static final int W = 1000;
	static final int H = 600;
	
	static final Color CONN = new Color(220, 220, 240);
		
	static class VisFrame extends JFrame {
		Canvas c;
		
		public VisFrame() {
			super("Neural Network Training");
			c = new Canvas();
			
			getContentPane().add(c);
			setSize(W, H);
			setVisible(true);
			c.createBufferStrategy(2);
		}
		
		public void update(Network in, int pass) {
			visualize(c.getBufferStrategy().getDrawGraphics(), in, pass);
			c.getBufferStrategy().show();
		}
	}
	
	static void weight(Graphics g, int x, int y, int gridSize, Weight w) {
		int intensity = (int) ((w.value + 1.0f) / 2.0f * 250.0f);
		if (intensity < 0) { intensity = 0; }
		if (intensity > 250) { intensity = 250; }
		/*int r = Math.max(0, 125 - intensity) * 2;
		int green = Math.max(0, intensity - 125) * 2;
		int b = Math.max(0, Math.abs(intensity - 125) - 50) * 2;
		g.setColor(new Color(r, green, b));*/
		g.setColor(new Color(intensity, intensity, intensity));
		g.fillRect(x, y, gridSize, gridSize);
		/*g.setColor(Color.BLUE);
		g.drawRect(x * GR_SZ, y * GR_SZ, GR_SZ, GR_SZ);*/
	}
	
	public static void saveFrame(Network in, int pass) {
		/*BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		visualize(img.getGraphics(), in, pass);
		try {
			ImageIO.write(img, "png", new File("/Users/zar/Desktop/film/" + pass + ".png"));
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
	public static void visualize(Graphics g, Network in, int pass) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, W, H);
		g.translate(30, 30);
		
		// so we have 6 blocks of 25+1 in the first layah
		int weightIndex = 0;
		for (int m = 0; m < 6; m++) {
			for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
				weight(g,
						(m * 12 + wX) * 10,
						wY * 10,
						10,
						in.layers.get(0).weights.get(weightIndex++)
				);
			}}
			weight(g,
					(m * 12 + 5) * 10,
					0,
					10,
					in.layers.get(0).weights.get(weightIndex++));
		}
		
		// Draw connections
		g.setColor(CONN);
		for (int m = 0; m < 6; m++) {
			g.drawLine(m * 12 * 10, 5 * 10 + 2, m * 12 * 10, 8 * 10 - 2);
		}
		
		// In the second layah we have 6 individual scaling factors with no bias
		weightIndex = 0;
		for (int m = 0; m < 6; m++) {
			weight(g,
					m * 12 * 10,
					8 * 10,
					10,
					in.layers.get(1).weights.get(weightIndex++)
			);
		}
		
				
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
		
		// Draw connections
		int[] m3Sizes = new int[16];
		for (int m1 = 0; m1 < 6; m1++) {
			for (int m3 = 0; m3 < 16; m3++) {
				if (!table[m1][m3]) { continue; }
				m3Sizes[m3]++;
			}
		}
		int[] m3Offsets = new int[16];
		for (int m3 = 1; m3 < 16; m3++) {
			m3Offsets[m3] = m3Sizes[m3 - 1] + m3Offsets[m3 - 1];
		}
		
		g.setColor(CONN);
		int[] m3Indices = new int[16];
		for (int m1 = 0; m1 < 6; m1++) {
			for (int m3 = 0; m3 < 16; m3++) {
				if (!table[m1][m3]) { continue; }
				g.drawLine(m1 * 12 * 10, 9 * 10 + 2, (m3Indices[m3] * 6 + m3Offsets[m3] * 6 + m3 * 8) * 2, 248);
				m3Indices[m3]++;
			}
		}
		
		// The next one is where it gets to the scary bit
		weightIndex = 0;
		m3Indices = new int[16];
		for (int m1 = 0; m1 < 6; m1++) {
			for (int m3 = 0; m3 < 16; m3++) {
				if (!table[m1][m3]) { continue; }
				for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
					weight(g,
							(m3Indices[m3] * 6 + m3Offsets[m3] * 6 + m3 * 8 + wX) * 2,
							250 + wY * 2,
							2,
							in.layers.get(2).weights.get(weightIndex++)
					);
				}}
				m3Indices[m3]++;
			}
		}
		
		// Biases!
		for (int m3 = 0; m3 < 16; m3++) {
			weight(g,
					(m3Indices[m3] * 6 + m3Offsets[m3] * 6 + m3 * 8) * 2,
					250,
					2,
					in.layers.get(2).weights.get(weightIndex++)
			);
		}
		
		// Boxes
		g.setColor(CONN);
		for (int m3 = 0; m3 < 16; m3++) {
			g.drawRect(
					m3Offsets[m3] * 12 + m3 * 16 - 2,
					248,
					m3Sizes[m3] * 12 + 5,
					13);
			g.drawLine(
					m3Offsets[m3] * 12 + m3 * 16,
					261,
					m3Offsets[m3] * 12 + m3 * 16,
					278);
		}
		
		// h3 -> h4, a scaling layer
		weightIndex = 0;
		for (int m3 = 0; m3 < 16; m3++) {
			weight(g,
					(m3Offsets[m3] * 6 + m3 * 8) * 2,
					280,
					5,
					in.layers.get(3).weights.get(weightIndex++)
			);
		}
		
		// h4 to output!
		weightIndex = 0;
		for (int m3 = 0; m3 < 16; m3++) {
			for (int out = 0; out < ProfileGen.OUTPUT_SIZE; out++) {
				weight(g,
						(m3Offsets[m3] * 6 + m3 * 8) * 2 + (out % 8) * 4,
						320 + (out / 8) * 4,
						4,
						in.layers.get(4).weights.get(weightIndex++)
				);
			}
		}
		
		for (int out = 0; out < ProfileGen.OUTPUT_SIZE; out++) {
			weight(g,
					(out % 8) * 4,
					420 + (out / 8) * 4,
					4,
					in.layers.get(4).weights.get(weightIndex++)
			);
		}
		
		g.setColor(CONN);
		g.drawLine(0, 286, 922, 315);
		g.drawLine(922, 286, 0, 315);
		g.drawRect(
				-3,
				317,
				925,
				169
		);
		
		/*
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
		
		g.setColor(Color.BLACK);
		g.drawString("Pass " + pass, 0, -12);
	}
}
