package com.metalbeetle.longan.neuralnetwork2;

import java.util.Random;

public class Util {
	private Util() {}
	
	static double sigmoid(double x) {
		return Math.tanh(x);
	}

	static double dSigmoid(double y) {
		return 1.0 - y * y;
	}
	
	static final Random random = new Random();

	static double rnd(double from, double to) { return (to - from) * random.nextDouble() + from; }
}
