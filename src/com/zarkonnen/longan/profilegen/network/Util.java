package com.zarkonnen.longan.profilegen.network;

import com.zarkonnen.longan.data.Letter;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Util {
	private Util() {}
	
	public static double sigmoid(double x) {
		return Math.tanh(x);
	}

	public static float dSigmoid(float y) {
		return 1.0f - y * y;
	}
	
	public static final Random random = new Random();

	public static float rnd(float from, float to) { return (to - from) * random.nextFloat() + from; }
	
	public static float[] getInputForNN(BufferedImage src) {
		BufferedImage scaledSrc = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledSrc.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 28, 28);
		int width = 0;
		int xOffset = 0;
		int height = 0;
		int yOffset = 0;
		if (src.getWidth() > src.getHeight()) {
			width = 16;
			height = 16 * src.getHeight() / src.getWidth();
			yOffset = (16 - height) / 2;
		} else {
			height = 16;
			width = 16 * src.getWidth() / src.getHeight();
			xOffset = (16 - width) / 2;
		}
		g.drawImage(src, 6 + xOffset, 6 + yOffset, 6 + xOffset + width, 6 + yOffset + height, 0, 0, src.getWidth(), src.getHeight(), null);
		src = scaledSrc;
		float[] result = new float[28 * 28];
		for (int y = 0; y < 28; y++) { for (int x = 0; x < 28; x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * 28 + x] = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0f / 1.5f - 1;
		} }
		return result;
	}
	
	public static float[] getInputForNN(Letter r, BufferedImage src, int intensityAdjustment) {
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
		src = scaledSrc;
		float[] result = new float[28 * 28];
		for (int y = 0; y < 28; y++) { for (int x = 0; x < 28; x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * 28 + x] = (c.getRed() + c.getGreen() + c.getBlue() + intensityAdjustment * 3) / 255.0f / 1.5f - 1;
		} }
		return result;
	}
}
