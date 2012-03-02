package com.zarkonnen.longan.nnidentifier.network;

import java.util.ArrayList;

public class Network {
	public final ArrayList<Layer> layers;

	public Network(ArrayList<Layer> layers) {
		this.layers = layers;
	}
	
	public double cutBelowThreshold(double t) {
		int nCut = 0;
		double nConns = 0;
		for (Layer l : layers) {
			nConns += l.weights.size();
			for (Weight w : l.weights) {
				if (Math.abs(w.value) < t) {
					w.value = 0;
					nCut++;
				}
			}
		}
		return nCut / nConns;
	}
	
	public void train(float[] input, float[] target, float[] n, float[] m) {
		setInput(input);
		update();
		setTargets(target);
		calculateDelta();
		adjustWeights(n, m);
	}
	
	public void train(float[] input, float[] target, float n, float m) {
		setInput(input);
		update();
		setTargets(target);
		calculateDelta();
		adjustWeights(n, m);
	}
	
	public float[] run(float[] input) {
		setInput(input);
		update();
		float[] output = new float[layers.get(layers.size() - 1).nodes.size()];
		for (int i = 0; i < output.length; i++) {
			output[i] = layers.get(layers.size() - 1).nodes.get(i).activation;
		}
		return output;
	}
	
	public void setInput(float[] inputs) {
		ArrayList<Node> iNodes = layers.get(0).nodes;
		assert(inputs.length == iNodes.size());
		for (int i = 0; i < inputs.length; i++) {
			iNodes.get(i).activation = inputs[i];
		}
	}
	
	public void setTargets(float[] targets) {
		ArrayList<Node> outputs = layers.get(layers.size() - 1).nodes;
		assert(targets.length == outputs.size());
		for (int i = 0; i < targets.length; i++) {
			outputs.get(i).delta = targets[i] - outputs.get(i).activation;
		}
	}
	
	public void calculateDelta() {
		// Don't calc delta for output layer.
		for (int i = layers.size() - 2; i >= 0; i--) {
			layers.get(i).calculateDelta();
		}
	}
	
	public void adjustWeights(float n, float m) {
		for (int i = layers.size() - 1; i >= 0; i--) {
			layers.get(i).adjustWeights(n, m);
		}
	}
	
	public void adjustWeights(float[] n, float[] m) {
		for (int i = layers.size() - 1; i >= 0; i--) {
			layers.get(i).adjustWeights(n[i], m[i]);
		}
	}
	
	public void update() {
		for (Layer l : layers) { l.update(); }
	}
	
	public String getDetails() {
		String d = "";
		for (Layer l : layers) {
			d += l.getDetails() + "\n";
		}
		return d;
	}
	
	public int numWeights() {
		int n = 0;
		for (Layer l : layers) {
			n += l.weights.size();
		}
		return n;
	}
	
	public int numNodes() {
		int n = 0;
		for (Layer l : layers) {
			n += l.nodes.size();
		}
		return n;
	}
}
