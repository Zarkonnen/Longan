package com.zarkonnen.longan.nnidentifier;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

public class SVM {
	public static class Img<T> {
		float[] data;
		T tag;

		public Img(float[] data, T tag) {
			this.data = data;
			this.tag = tag;
		}
	}
	
	static float[] expand(float[] original) {
		float[] expanded = new float[5];
		expanded[0] = original[0];
		expanded[1] = original[1];
		expanded[2] = original[0] * original[0];
		expanded[3] = original[1] * original[1];
		expanded[4] = original[0] * original[1];
		return expanded;
	}
	
	static float dot(float[] a, float[] b) {
		a = expand(a);
		b = expand(b);
		float d = 0.0f;
		for (int i = 0; i < a.length; i++) {
			d += a[i] * b[i];
		}
		return d;
	}
	
	public static void main(String[] args) throws IOException {
		ArrayList<Img<Color>> els = new ArrayList<Img<Color>>();
		Random r = new Random();
		for (int i = 0; i < 100; i++) {
			float[] data = new float[] { r.nextFloat(), r.nextFloat() };
			// Make everything within 0.3 of { 0.8, 0.5 } RED.
			Color tag = (data[0] - 0.8) * (data[0] - 0.8) + (data[1] - 0.5) * (data[1] - 0.5) < 0.3 * 0.3
					? Color.RED : Color.BLUE;
			els.add(new Img<Color>(data, tag));
		}
		
		BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 400, 400);
		
		// Find best SV.
		int bestScore = -1;
		float[] bestV = null;
		float bestD = 0;
		Color aTag = null;
		Color bTag = null;
		for (Img<Color> a : els) {
			for (Img<Color> b : els) {
				if (a.tag.equals(b.tag)) { continue; }
				float[] vector = new float[a.data.length];
				float[] avg = new float[a.data.length];
				for (int i = 0; i < vector.length; i++) {
					vector[i] = a.data[i] - b.data[i];
					avg[i] = a.data[i] / 2 + b.data[i] / 2;
				}
				float d = dot(vector, avg);
				
				// How good is this?
				int score = 0;
				for (Img<Color> e : els) {
					float dist = d - dot(vector, e.data);
					Color tag = dist < 0 ? a.tag : b.tag;
					if (tag.equals(e.tag)) { score++; }
				}
				if (score > bestScore) {
					bestScore = score;
					bestV = vector;
					bestD = d;
					aTag = a.tag;
					bTag = b.tag;
				}
			}
		}
		
		// Draw support vector by checking each pixel of the display.
		g.setColor(Color.LIGHT_GRAY);
		for (int y = 0; y < 400; y++) { for (int x = 0; x < 400; x++) {
			float dist = bestD - dot(bestV, new float[] { x * 1.0f / 400, y * 1.0f / 400});
			if (Math.abs(dist) < 0.0002) {
				g.fillRect(x, y, 1, 1);
			}
		}}

		// Draw classes.
		for (Img<Color> e : els) {
			float dist = bestD - dot(bestV, e.data);
			g.setColor(dist < 0 ? aTag : bTag);
			g.drawRect((int) (400 * e.data[0]) - 2, (int) (400 * e.data[1]) - 2, 5, 5);
		}
		
		// Draw points with correct classes.
		for (Img<Color> e : els) {
			g.setColor(e.tag);
			g.fillRect((int) (400 * e.data[0]) - 1, (int) (400 * e.data[1]) - 1, 4, 4);
		}
		
		ImageIO.write(img, "png", new File("/Users/zar/Desktop/out.png"));
	}
}
