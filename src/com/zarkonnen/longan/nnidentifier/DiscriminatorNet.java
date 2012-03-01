package com.zarkonnen.longan.nnidentifier;

import com.zarkonnen.longan.nnidentifier.network.Connection;
import com.zarkonnen.longan.nnidentifier.network.Network;
import com.zarkonnen.longan.nnidentifier.network.Weight;
import com.zarkonnen.longan.nnidentifier.network.Layer;
import com.zarkonnen.longan.nnidentifier.network.Node;
import java.util.ArrayList;
import java.util.Random;

import static com.zarkonnen.longan.nnidentifier.network.Util.*;

public class DiscriminatorNet {	
	Network nw;
	Random r;
	
	public DiscriminatorNet(long seed) {
		r = new Random(seed);
		Layer input = new Layer("Input");
		for (int y = 0; y < 28; y++) { for (int x = 0; x < 28; x++) {
			input.nodes.add(new Node("input " + y + "/" + x));
		}}
		// Bias node!
		Node biasN = new Node("input bias");
		biasN.activation = 1.0f;
		
		Layer h1 = new Layer("H1 (conv)");
		for (int m = 0; m < 6; m++) {
			for (int y = 0; y < 24; y++) { for (int x = 0; x < 24; x++) {
				h1.nodes.add(new Node("H1." + m + " " + y + "/" + x));
			}}
		}
		
		Layer h2 = new Layer("H2 (subsampling)");
		for (int m = 0; m < 6; m++) {
			for (int y = 0; y < 12; y++) { for (int x = 0; x < 12; x++) {
				h2.nodes.add(new Node("H2." + m + " " + y + "/" + x));
			}}
		}
		
		Layer h3 = new Layer("H3 (conv)");
		for (int m = 0; m < 16; m++) {
			for (int y = 0; y < 8; y++) { for (int x = 0; x < 8; x++) {
				h3.nodes.add(new Node("H3." + m + " " + y + "/" + x));
			}}
		}
		
		Layer h4 = new Layer("H4 (subsampling)");
		for (int m = 0; m < 16; m++) {
			for (int y = 0; y < 4; y++) { for (int x = 0; x < 4; x++) {
				h4.nodes.add(new Node("H4." + m + " " + y + "/" + x));
			}}
		}
		
		Layer output = new Layer("Output");
		output.nodes.add(new Node("Output"));
		
		// Connect input to h1
		for (int m = 0; m < 6; m++) {
			for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
				Weight w = new Weight(rnd(-2.0f / 26, 2.0f / 26, r));
				input.weights.add(w);
				for (int y = 0; y < 24; y++) { for (int x = 0; x < 24; x++) {
					new Connection(
						input.nodes.get(
							(y + wY) * 28 +
							(x + wX)
						),
						h1.nodes.get(
							m * 24 * 24 +
							y * 24 +
							x
						),
						w
					);
				}}
			}}
			
			// Bias
			Weight w = new Weight(rnd(-2.0f / 26, 2.0f / 26, r));
			input.weights.add(w);
			for (int y = 0; y < 24; y++) { for (int x = 0; x < 24; x++) {
				new Connection(
					biasN,
					h1.nodes.get(
						m * 24 * 24 +
						y * 24 +
						x
					),
					w
				);
			}}
		}
		
		// Connect h1 to h2
		for (int m = 0; m < 6; m++) {
			Weight w = new Weight(0.25f);
			h1.weights.add(w);
			for (int y = 0; y < 12; y++) { for (int x = 0; x < 12; x++) {
				for (int dy = 0; dy < 2; dy++) { for (int dx = 0; dx < 2; dx++) {
					new Connection(
						h1.nodes.get(
							m * 24 * 24 +
							(y * 2 + dy) * 24 +
							(x * 2 + dx)
						),
						h2.nodes.get(
							m * 12 * 12 +
							y * 12 +
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
		
		for (int m1 = 0; m1 < 6; m1++) {
			for (int m3 = 0; m3 < 16; m3++) {
				if (!table[m1][m3]) { continue; }
				for (int wY = 0; wY < 5; wY++) { for (int wX = 0; wX < 5; wX++) {
					Weight w = new Weight(rnd(-2.0f / 76, 2.0f / 76, r));
					h2.weights.add(w);
					for (int y = 0; y < 8; y++) { for (int x = 0; x < 8; x++) {
						new Connection(
							h2.nodes.get(
								m1 * 12 * 12 +
								(y + wY) * 12 +
								(x + wX)
							),
							h3.nodes.get(
								m3 * 8 * 8 +
								y * 8 +
								x
							),
							w
						);
					}}
				}}
			}
		}
		
		// Add biases
		for (int m3 = 0; m3 < 16; m3++) {
			// Add bias
			Weight w = new Weight(rnd(-2.0f / 76, 2.0f / 76, r));
			h2.weights.add(w);
			for (int y = 0; y < 8; y++) { for (int x = 0; x < 8; x++) {
				new Connection(
					biasN,
					h3.nodes.get(
						m3 * 8 * 8 +
						y * 8 +
						x
					),
					w
				);
			}}
		}
		
		// Connect h3 to h4
		for (int m3 = 0; m3 < 16; m3++) {
			Weight w = new Weight(0.25f);
			h3.weights.add(w);
			for (int y = 0; y < 4; y++) { for (int x = 0; x < 4; x++) {
				for (int dy = 0; dy < 2; dy++) { for (int dx = 0; dx < 2; dx++) {
					new Connection(
						h3.nodes.get(
							m3 * 8 * 8 +
							(y * 2 + dy) * 8 +
							(x * 2 + dx)
						),
						h4.nodes.get(
							m3 * 4 * 4 +
							y * 4 +
							x
						),
						w
					);
				}}
			}}
		}
		
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
	
	public void train(Example ex, float n, float m) {
		nw.train(ex.input, ex.target, n, m);
	}
	
	public float[] run(float[] input) {
		return nw.run(input);
	}
}
