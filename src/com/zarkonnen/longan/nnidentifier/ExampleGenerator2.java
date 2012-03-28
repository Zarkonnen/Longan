package com.zarkonnen.longan.nnidentifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Random;

public class ExampleGenerator2 {
	public static BufferedImage makeLetterImage(String l, Config.FontType font, Random r) {
		int intensity = 0;
		int size = 50 + r.nextInt(50);
		return getLetter(l, font.font, font.italic, 1, size,
				intensity, 0,
				intensity + 200 + r.nextInt(40),
				r.nextInt(30) + r.nextInt(20) + r.nextInt(20),
				new int[] {1, 1, 1, 1}, r);
	}
	
	public static BufferedImage makeCorrectlyVariableLetterImage(String l, Config.FontType font, Random r) {
		int intensity = r.nextInt(30) + r.nextInt(30) + r.nextInt(50);
		int size = 30 + r.nextInt(40);
		return getLetter(l, font.font, font.italic, r.nextInt(2), size,
				intensity, -Math.PI / 60 + r.nextDouble() * Math.PI / 30,
				Math.min(255, intensity + 160 + r.nextInt(50)),
				30 + r.nextInt(30),
				new int[] {r.nextInt(2), r.nextInt(2), r.nextInt(2), r.nextInt(2)}, r);
	}
	
	public static BufferedImage makeSemiVariableLetterImage(String l, Config.FontType font, Random r) {
		int intensity = r.nextInt(30) + r.nextInt(30) + r.nextInt(80);
		int size = 30 + r.nextInt(40);
		return getLetter(l, font.font, font.italic, r.nextInt(5), size,
				intensity, -Math.PI / 20 + r.nextDouble() * Math.PI / 10,
				intensity + 160 + r.nextInt(50),
				r.nextInt(30) + r.nextInt(20) + r.nextInt(20),
				new int[] {r.nextInt(2), r.nextInt(2), r.nextInt(2), r.nextInt(2)}, r);
	}
	
	static BufferedImage getLetter(String l, String font, boolean italic, int blurIterations, int size, int color, double rot, int cropBoundary,
			int noise, int[] widen, Random r) {
		BufferedImage img = new BufferedImage(size * 4, size * 4, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, size * 4, size * 4);
		g.setFont(new Font(font, italic ? Font.ITALIC : Font.PLAIN, size));
		g.setColor(new Color(color, color, color));
		g.rotate(rot);
		g.drawString(l, size * 2, size * 2);
		for (int i = 0; i < blurIterations; i++) {
			img = noise(img, noise, r);
			img = blur(img);
		}
		img = crop(img, cropBoundary, widen);
		if (img == null) {
			System.out.println("fish");
		}
		return img == null ? getLetter(l, font, italic, blurIterations == 0 ? 0 : 1, size * 2, 0, rot, cropBoundary, noise, widen, r) : img;
	}
	
	static BufferedImage getSimpleLetter(String l, String font, boolean italic) {
		BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 100, 100);
		g.setFont(new Font(font, italic ? Font.ITALIC : Font.PLAIN, 40));
		g.setColor(Color.BLACK);
		g.drawString(l, 50, 50);
		return crop(img, 127, new int[] { 0, 1, 0, 1 });
	}
	
	static BufferedImage weaksauculate(BufferedImage img, int color) {
		for (int y = 0; y < img.getHeight(); y++) { for (int x = 0; x < img.getWidth(); x++) {
			Color c = new Color(img.getRGB(x, y));
			int val = Math.min(255, (c.getRed() + c.getGreen() + c.getBlue() + color * 8) / 3);
			img.setRGB(x, y, new Color(val, val, val).getRGB());
		}}
		return img;
	}

	static BufferedImage noise(BufferedImage img, int amt, Random r) {
		for (int y = 0; y < img.getHeight(); y++) { for (int x = 0; x < img.getWidth(); x++) {
			Color c = new Color(img.getRGB(x, y));
			int val = Math.min(255, (c.getRed() + c.getGreen() + c.getBlue() + r.nextInt(amt * 3 + 1)) / 3);
			img.setRGB(x, y, new Color(val, val, val).getRGB());
		}}
		return img;
	}
	
	public static BufferedImage blur(BufferedImage img) {
		BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, new float[] {
			1 / 9f, 1 / 9f, 1 / 9f,
			1 / 9f, 1 / 9f, 1 / 9f,
			1 / 9f, 1 / 9f, 1 / 9f
		}));
		img = op.filter(img, null);
		// Get rid of stupid border
		Graphics2D g = img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, img.getWidth(), 1);
		g.fillRect(0, img.getHeight() - 1, img.getWidth(), 1);
		g.fillRect(0, 0, 1, img.getHeight());
		g.fillRect(img.getWidth() - 1, 0, 1, img.getHeight());
		return img;
	}
	
	public static BufferedImage crop(BufferedImage img, int bound, int[] widen) {
		int top = 0;
		lp: while (top < img.getHeight()) {
			for (int x = 0; x < img.getWidth(); x++) {
				Color c = new Color(img.getRGB(x, top));
				if (c.getRed() + c.getGreen() + c.getBlue() < bound * 3) {
					break lp;
				}
			}
			top++;
		}
		int bottom = img.getHeight() - 1;
		lp: while (bottom > top) {
			for (int x = 0; x < img.getWidth(); x++) {
				Color c = new Color(img.getRGB(x, bottom));
				if (c.getRed() + c.getGreen() + c.getBlue() < bound * 3) {
					break lp;
				}
			}
			bottom--;
		}
		int left = 0;
		lp: while (left < img.getWidth()) {
			for (int y = 0; y < img.getHeight(); y++) {
				Color c = new Color(img.getRGB(left, y));
				if (c.getRed() + c.getGreen() + c.getBlue() < bound * 3) {
					break lp;
				}
			}
			left++;
		}
		int right = img.getWidth() - 1;
		lp: while (right > left) {
			for (int y = 0; y < img.getHeight(); y++) {
				Color c = new Color(img.getRGB(right, y));
				if (c.getRed() + c.getGreen() + c.getBlue() < bound * 3) {
					break lp;
				}
			}
			right--;
		}
		
		if (right - left <= 0 || bottom - top <= 0) {
			if (bound == 254) { return null; }
			return crop(img, 254, widen);
		}
		
		left = Math.max(0, left - widen[0]);
		right = Math.min(img.getWidth() - 1, right + widen[1]);
		top = Math.max(0, top - widen[2]);
		bottom = Math.min(img.getHeight() - 1, bottom + widen[3]);
		
		BufferedImage img2 = new BufferedImage(right - left, bottom - top, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img2.createGraphics();
		g.drawImage(img, 0, 0, right - left, bottom - top, left, top, right, bottom, null);
		
		return img2;
	}
}
