package com.zarkonnen.longan.nnidentifier.network;

import com.zarkonnen.longan.data.Letter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
	
	public static float rnd(float from, float to, Random r) {
		return (to - from) * r.nextFloat() + from;
	}
	
	public static BufferedImage convertInputToImg(float[] in) {
		int sz = (int) Math.sqrt(in.length);
		int w = sz;
		int h = sz;
		if (sz * sz != in.length) {
			w = ((int) Math.sqrt(in.length / 2)) * 2;
			h = ((int) Math.sqrt(in.length / 2));
		}
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < h; y++) { for (int x = 0; x < w; x++) {
			int intensity = (int) ((in[y * w + x] + 1.0f) / 2.0f * 255.0f);
			intensity = Math.max(0, Math.min(255, intensity));
			Color c = new Color(intensity, intensity, intensity);
			img.setRGB(x, y, c.getRGB());
		} }
		BufferedImage img2 = new BufferedImage(w * 10, h * 10, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img2.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(img, 0, 0, w * 10, h * 10, null);
		return img2;
	}
	
	public static BufferedImage convertInputToImg(float[][] in) {
		BufferedImage img = new BufferedImage(in[0].length, in.length, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < in.length; y++) { for (int x = 0; x < in[0].length; x++) {
			int intensity = (int) ((in[y][x] + 1.0f) / 2.0f * 255.0f);
			intensity = Math.max(0, Math.min(255, intensity));
			Color c = new Color(intensity, intensity, intensity);
			img.setRGB(x, y, c.getRGB());
		} }
		BufferedImage img2 = new BufferedImage(in[0].length * 10, in.length * 10, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img2.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(img, 0, 0, in[0].length * 10, in.length * 10, null);
		return img2;
	}
	
	public static float[] convertImgToInput(BufferedImage src) {
		float[] result = new float[src.getWidth() * src.getHeight()];
		for (int y = 0; y < src.getHeight(); y++) { for (int x = 0; x < src.getWidth(); x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * src.getWidth() + x] = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0f / 1.5f - 1;
		} }
		return result;
	}
	
	public static float[] getTargetForNN(BufferedImage src, boolean proportional) {
		BufferedImage scaledSrc = new BufferedImage(16, 8, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = scaledSrc.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 16, 8);
		int width = 0;
		int xOffset = 0;
		int height = 0;
		int yOffset = 0;
		if (proportional) {
			if (src.getWidth() > src.getHeight()) {
				width = 16;
				height = 8 * src.getHeight() / src.getWidth();
				yOffset = (8 - height) / 2;
			} else {
				height = 8;
				width = 16 * src.getWidth() / src.getHeight();
				xOffset = (16 - width) / 2;
			}
		} else {
			width  = 16;
			height = 8;
			xOffset = 0;
			yOffset = 0;
		}
		g.drawImage(src, xOffset, yOffset, xOffset + width, yOffset + height, null);
		src = scaledSrc;
		float[] result = new float[16 * 8];
		for (int y = 0; y < 8; y++) { for (int x = 0; x < 16; x++) {
			Color c = new Color(src.getRGB(x, y));
			result[y * 16 + x] = (c.getRed() + c.getGreen() + c.getBlue()) / 255.0f / 1.5f - 1;
		} }
		return result;
	}
	
	public static float[] getInputForNN(BufferedImage src, boolean proportional) {
		BufferedImage scaledSrc = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = scaledSrc.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 28, 28);
		int width = 0;
		int xOffset = 0;
		int height = 0;
		int yOffset = 0;
		if (proportional) {
			if (src.getWidth() > src.getHeight()) {
				width = 16;
				height = 16 * src.getHeight() / src.getWidth();
				yOffset = (16 - height) / 2;
			} else {
				height = 16;
				width = 16 * src.getWidth() / src.getHeight();
				xOffset = (16 - width) / 2;
			}
		} else {
			width  = 16;
			height = 16;
			xOffset = 0;
			yOffset = 0;
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
	
	public static float[] getInputForNN(Letter r, BufferedImage src, int intensityAdjustment, boolean proportional) {
		// Masking
		BufferedImage maskedSrc = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = maskedSrc.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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
		g = scaledSrc.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 28, 28);
		int width = 0;
		int xOffset = 0;
		int height = 0;
		int yOffset = 0;
		if (proportional) {
			if (r.width > r.height) {
			width = 16;
			height = 16 * r.height / r.width;
			yOffset = (16 - height) / 2;
		} else {
			height = 16;
			width = 16 * r.width / r.height;
			xOffset = (16 - width) / 2;
		}
		} else {
			width  = 16;
			height = 16;
			xOffset = 0;
			yOffset = 0;
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
