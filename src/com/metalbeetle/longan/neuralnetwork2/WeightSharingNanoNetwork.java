package com.metalbeetle.longan.neuralnetwork2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static com.metalbeetle.longan.neuralnetwork2.Util.*;

public class WeightSharingNanoNetwork {
	public Network nw;
	Random r = new Random();
	
	public WeightSharingNanoNetwork(int variant) {
		Layer input = new Layer("Input");
		for (int i = 0; i < 12 * 12 * DemoNet.kernels.length + 3; i++) {
			input.nodes.add(new Node("input " + i));
		}
		// Bias node!
		Node biasN = new Node("input bias");
		biasN.activation = 1.0;
		//input.nodes.add(biasN);*/
		
		Layer hidden = new Layer("Hidden");
		for (int i = 0; i < 16; i++) {
			hidden.nodes.add(new Node("hidden " + i));
		}
		
		// 2nd hidden layer
		Layer h2 = new Layer("Hidden 2");
		for (int i = 0; i < 7; i++) {
			h2.nodes.add(new Node("h2 " + i));
		}
		
		Layer output = new Layer("Output");
		output.nodes.add(new Node("output"));
		
		HashMap<String, Weight> offsetToWeight = new HashMap<String, Weight>();
		int iNum = 0;
		for (Node iN : input.nodes) {
			int inY = iNum / 12;
			int inX = iNum % 12;
			iNum++;
			int hNum = 0;
			for (Node hN : hidden.nodes) {
				int hY = (hNum / 4) * 2 + 1;
				int hX = (hNum % 4) * 2 + 1;
				hNum++;
				if (iNum / 144 != hNum / (2 + variant) && (iNum + hNum + variant) % 4 == 0) {
					String offset = null;
					if (iNum >= 12 * 12 * DemoNet.kernels.length) {
						offset = "special " + iNum;
					} else {
						offset = (inY - hY) + "/" + (inX - hX);
					}
					Weight w;
					if (offsetToWeight.containsKey(offset)) {
						w = offsetToWeight.get(offset);
					} else {
						w = new Weight(rnd(-0.2, 0.2));
						offsetToWeight.put(offset, w);
						input.weights.add(w);
					}
					new Connection(iN, hN, w);
				}
			}
		}
		for (Node hN : hidden.nodes) {
			Weight w = new Weight(rnd(-0.2, 0.2));
			input.weights.add(w);
			new Connection(biasN, hN, w);
		}
		
		for (Node hN : hidden.nodes) {
			for (Node h2N : h2.nodes) {
				Weight w = new Weight(rnd(-1.0, 1.0));
				hidden.weights.add(w);
				new Connection(hN, h2N, w);
			}
		}
		
		for (Node h2N : h2.nodes) {
			Weight w = new Weight(rnd(-1.0, 1.0));
			hidden.weights.add(w);
			new Connection(biasN, h2N, w);
		}
		
		for (Node h2N : h2.nodes) {
			Weight w = new Weight(rnd(-2.0, 2.0));
			h2.weights.add(w);
			new Connection(h2N, output.nodes.get(0), w);
		}
		
		ArrayList<Layer> layers = new ArrayList<Layer>();
		layers.add(input);
		layers.add(hidden);
		layers.add(h2);
		layers.add(output);
		
		nw = new Network(layers);
	}
	
	public void train(double[][] positives, double[][] negatives, double n, double m) {
		int to = Math.max(positives.length, negatives.length);
		for (int i = 0; i < to; i++) {
			nw.train(positives[i % positives.length], new double[] {1.0}, n, m);
			nw.train(negatives[i % negatives.length], new double[] {0.0}, n, m);
		}
	}
	
	public double run(double[] input) {
		return nw.run(input)[0];
	}
}
