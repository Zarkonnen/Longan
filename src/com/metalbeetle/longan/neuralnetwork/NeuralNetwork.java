package com.metalbeetle.longan.neuralnetwork;

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

import java.util.ArrayList;
import java.util.Random;
import static com.metalbeetle.longan.neuralnetwork.Util.*;

public class NeuralNetwork {
	Network nw;
	Random r = new Random();
	
	public NeuralNetwork() {
		Layer input = new Layer("Input");
		for (int y = 0; y < 20; y++) { for (int x = 0; x < 20; x++) {
			input.nodes.add(new Node("input " + y + "/" + x));
		}}
		// Bias node!
		Node biasN = new Node("input bias");
		biasN.activation = 1.0;
		
		Layer h1 = new Layer("H1 (conv)");
		for (int m = 0; m < 6; m++) {
			for (int y = 0; y < 16; y++) { for (int x = 0; x < 16; x++) {
				h1.nodes.add(new Node("H1." + m + " " + y + "/" + x));
			}}
		}
		
		Layer h2 = new Layer("H2 (subsampling)");
		for (int m = 0; m < 5; m++) {
			for (int y = 0; y < 8; y++) { for (int x = 0; x < 8; x++) {
				h2.nodes.add(new Node("H2." + m + " " + y + "/" + x));
			}}
		}
		
		Layer h3 = new Layer("H3 (conv)");
		for (int m = 0; m < 12; m++) {
			for (int y = 0; y < 4; y++) { for (int x = 0; x < 4; x++) {
				h3.nodes.add(new Node("H3." + m + " " + y + "/" + x));
			}}
		}
		
		Layer h4 = new Layer("H4 (subsampling)");
		for (int m = 0; m < 12; m++) {
			for (int y = 0; y < 2; y++) { for (int x = 0; x < 2; x++) {
				h4.nodes.add(new Node("H4." + m + " " + y + "/" + x));
			}}
		}
		
		Layer output = new Layer("Output");
		for (int i = 0; i < NeuralNetworkLetterIdentifier.OUTPUT_SIZE; i++) {
			output.nodes.add(new Node("Output " + i));
		}
		
		// Connect input to h1
		for (int m = 0; m < 5; m++) {
			for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
				Weight w = new Weight(rnd(-2.0 / 26, 2.0 / 26));
				input.weights.add(w);
				for (int y = 0; y < 16; y++) { for (int x = 0; x < 16; x++) {
					new Connection(
						input.nodes.get(
							(y + wY) * 20 +
							(x + wX)
						),
						h1.nodes.get(
							m * 16 * 16 +
							y * 16 +
							x
						),
						w
					);
				}}
			}}
			
			// Bias
			Weight w = new Weight(rnd(-2.0 / 26, 2.0 / 26));
			input.weights.add(w);
			for (int y = 0; y < 16; y++) { for (int x = 0; x < 16; x++) {
				new Connection(
					biasN,
					h1.nodes.get(
						m * 16 * 16 +
						y * 16 +
						x
					),
					w
				);
			}}
		}
		
		// Connect h1 to h2
		for (int m = 0; m < 5; m++) {
			Weight w = new Weight(0.25);
			h1.weights.add(w);
			for (int y = 0; y < 8; y++) { for (int x = 0; x < 8; x++) {
				for (int dy = 0; dy < 2; dy++) { for (int dx = 0; dx < 2; dx++) {
					new Connection(
						h1.nodes.get(
							m * 16 * 16 +
							(y * 2 + dy) * 16 +
							(x * 2 + dx)
						),
						h2.nodes.get(
							m * 8 * 8 +
							y * 8 +
							x
						),
						w
					);
				}}
			}}
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
		
		for (int m1 = 0; m1 < 5; m1++) {
			for (int m3 = 0; m3 < 12; m3++) {
				if (!table[m1][m3]) { continue; }
				for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
					Weight w = new Weight(rnd(-2.0 / 26, 2.0 / 26));
					h2.weights.add(w);
					for (int y = 0; y < 4; y++) { for (int x = 0; x < 4; x++) {
						new Connection(
							h2.nodes.get(
								m1 * 8 * 8 +
								(y + wY) * 8 +
								(x + wX)
							),
							h3.nodes.get(
								m3 * 4 * 4 +
								y * 4 +
								x
							),
							w
						);
					}}
				}}
				
				// Add bias
				Weight w = new Weight(rnd(-2.0 / 26, 2.0 / 26));
				h2.weights.add(w);
				for (int y = 0; y < 4; y++) { for (int x = 0; x < 4; x++) {
					new Connection(
						biasN,
						h3.nodes.get(
							m3 * 4 * 4 +
							y * 4 +
							x
						),
						w
					);
				}}
			}
		}
		
		// Connect h3 to h4
		for (int m3 = 0; m3 < 12; m3++) {
			Weight w = new Weight(0.25);
			h3.weights.add(w);
			for (int y = 0; y < 2; y++) { for (int x = 0; x < 2; x++) {
				for (int dy = 0; dy < 2; dy++) { for (int dx = 0; dx < 2; dx++) {
					new Connection(
						h3.nodes.get(
							m3 * 4 * 4 +
							(y * 2 + dy) * 4 +
							(x * 2 + dx)
						),
						h4.nodes.get(
							m3 * 2 * 2 +
							y * 2 +
							x
						),
						w
					);
				}}
			}}
		}
		
		// Connect h4 to output (randomly semifull connection)
		int x = 0;
		for (Node h4N : h4.nodes) {
			for (Node oN : output.nodes) {
				if (Integer.bitCount(x++) % 2 == 0) { continue; }
				//if (r.nextBoolean()) { continue; }
				Weight w = new Weight(rnd(-2.0 / 49, 2.0 / 49));
				h4.weights.add(w);
				new Connection(h4N, oN, w);
			}
		}
		// Add biases to output.
		for (Node oN : output.nodes) {
			Weight w = new Weight(rnd(-2.0 / 49, 2.0 / 49));
			h4.weights.add(w);
			new Connection(biasN, oN, w);
		}
		
		ArrayList<Layer> layers = new ArrayList<Layer>();
		layers.add(input);
		layers.add(h1);
		layers.add(h2);
		layers.add(h3);
		layers.add(h4);
		layers.add(output);
		
		nw = new Network(layers);
	}
	
	public double[] run(double[] input) {
		return nw.run(input);
	}
}
