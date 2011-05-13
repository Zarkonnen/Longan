package com.metalbeetle.longan.neuralnetwork;

import java.util.ArrayList;
import java.util.Random;

import static com.metalbeetle.longan.neuralnetwork.Util.*;

public class MicroNetwork {
	Network nw;
	Random r = new Random();
	
	public MicroNetwork() {
		Layer input = new Layer("Input");
		for (int i = 0; i < 12 * 12 * MicroNet.kernels.length; i++) {
			input.nodes.add(new Node("input " + i));
		}
		// Bias node!
		Node biasN = new Node("input bias");
		biasN.activation = 1.0;
		//input.nodes.add(biasN);*/
		
		Layer hidden = new Layer("Hidden");
		for (int i = 0; i < 44; i++) {
			hidden.nodes.add(new Node("hidden " + i));
		}
		
		// 2nd hidden layer
		Layer h2 = new Layer("Hidden 2");
		for (int i = 0; i < 9; i++) {
			h2.nodes.add(new Node("h2 " + i));
		}
		
		Layer output = new Layer("Output");
		output.nodes.add(new Node("output"));
		
		int iNum = 0;
		for (Node iN : input.nodes) {
			iNum++;
			int hNum = 0;
			for (Node hN : hidden.nodes) {
				hNum++;
				Weight w = new Weight(rnd(-0.2, 0.2));
				input.weights.add(w);
				ArrayList<Node> inputs = new ArrayList<Node>();
				inputs.add(iN);
				//if (r.nextInt(/*5*/5) == 0/*r.nextBoolean()*/) {
				//	new Connection(inputs, hN, w);
				//}
				if (iNum / 144 != hNum / 3 && (iNum + hNum) % 4 == 0) {
					new Connection(inputs, hN, w);
				}
			}
		}
		for (Node hN : hidden.nodes) {
			Weight w = new Weight(rnd(-0.2, 0.2));
			input.weights.add(w);
			ArrayList<Node> inputs = new ArrayList<Node>();
			inputs.add(biasN);
			new Connection(inputs, hN, w);
		}
		
		for (Node hN : hidden.nodes) {
			for (Node h2N : h2.nodes) {
				Weight w = new Weight(rnd(-1.0, 1.0));
				hidden.weights.add(w);
				ArrayList<Node> inputs = new ArrayList<Node>();
				inputs.add(hN);
				new Connection(inputs, h2N, w);
			}
		}
		
		for (Node h2N : h2.nodes) {
			Weight w = new Weight(rnd(-1.0, 1.0));
			hidden.weights.add(w);
			ArrayList<Node> inputs = new ArrayList<Node>();
			inputs.add(biasN);
			new Connection(inputs, h2N, w);
		}
		
		//for (Node hN : hidden.nodes) {
		for (Node h2N : h2.nodes) {
			Weight w = new Weight(rnd(-2.0, 2.0));
			//hidden.weights.add(w);
			h2.weights.add(w);
			ArrayList<Node> inputs = new ArrayList<Node>();
			inputs.add(h2N);
			new Connection(inputs, output.nodes.get(0), w);
		}
		
		ArrayList<Layer> layers = new ArrayList<Layer>();
		layers.add(input);
		layers.add(hidden);
		layers.add(h2);
		layers.add(output);
		
		nw = new Network(layers);
	}
	
	public void train(double[][] positives, double[][] negatives, double n, double m) {
		/*for (int i = 0; i < positives.length; i++) {
			nw.train(positives[i], new double[] {1.0}, n, m);
			nw.train(negatives[i], new double[] {0.0}, n, m);
		}*/
		int to = Math.max(positives.length, negatives.length);
		for (int i = 0; i < to; i++) {
			nw.train(positives[i % positives.length], new double[] {1.0}, n, m);
			nw.train(negatives[i % negatives.length], new double[] {0.0}, n, m);
		}
		/*
		if (positives.length > negatives.length) {
			int lastNeg = -1;
			for (int i = 0; i < positives.length; i++) {
				nw.train(positives[i], new double[] {1.0}, n, m);
				int neg = i * negatives.length / positives.length;
				if (neg != lastNeg) {
					nw.train(negatives[neg], new double[] {0.0}, n, m);
					lastNeg = neg;
				}
			}
		} else {
			int lastPos = 0;
			for (int i = 0; i < negatives.length; i++) {
				int pos = i * positives.length / negatives.length;
				if (pos != lastPos) {
					nw.train(positives[pos], new double[] {1.0}, n, m);
					lastPos = pos;
				}
				nw.train(negatives[i], new double[] {0.0}, n, m);
			}
		}*/
	}
	
	public double run(double[] input) {
		return nw.run(input)[0];
	}
}
