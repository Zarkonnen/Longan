package com.zarkonnen.longan.nnidentifier;

import java.awt.image.BufferedImage;

public class Example {
	public String letter;
	public float[] input;
	public float[] target;
	public BufferedImage original;
	public String path;

	public Example(String letter, float[] input, float[] target) {
		this.letter = letter;
		this.input = input;
		this.target = target;
	}
	
	float inputDistanceTo(Example e2) {
		float d = 0.0f;
		for (int i = 0; i < input.length; i++) {
			d += (input[i] - e2.input[i]) * (input[i] - e2.input[i]);
		}
		return (float) Math.sqrt(d);
	}
}
