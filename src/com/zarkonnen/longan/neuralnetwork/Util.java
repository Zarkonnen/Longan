package com.zarkonnen.longan.neuralnetwork;

/*
 * Copyright 2011 David Stark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Random;

public class Util {
	private Util() {}
	
	static double sigmoid(double x) {
		return Math.tanh(x);
	}

	static double dSigmoid(double y) {
		return 1.0 - y * y;
	}
	
	static final Random RANDOM = new Random();

	static double rnd(double from, double to) { return (to - from) * RANDOM.nextDouble() + from; }
}
