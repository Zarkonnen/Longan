package com.metalbeetle.longan.neuralnetwork;

import java.util.Random;

public class Util {
	private Util() {}
	
	static double sigmoid(double x) {
		return Math.tanh(x);
		//return 1.7159 * Math.tanh(x * 2 / 3);
	}

	static double dSigmoid(double y) {
		//return 1.7159 * 2 / 3 * (1 - (y / 1.7159) * (y / 1.7159));
		return 1.0 - y * y;
	}
	
	static final Random random = new Random();

	static double rnd(double from, double to) { return (to - from) * random.nextDouble() + from; }
}
