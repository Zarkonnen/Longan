package com.metalbeetle.longan.neuralnetwork2;

import java.util.ArrayList;

public class Network {
	public final ArrayList<Layer> layers;
	
	boolean frozen = false;

	public Network(ArrayList<Layer> layers) {
		this.layers = layers;
	}
	
	public void freeze() {
		if (frozen) { return; }
		for (Layer l : layers) { l.freeze(); }
		frozen = true;
	}
	
	public void train(double[] input, double[] target, double n, double m) {
		setInput(input);
		update();
		setTargets(target);
		calculateDelta();
		adjustWeights(n, m);
	}
	
	public double[] run(double[] input) {
		setInput(input);
		update();
		double[] output;
		if (frozen) {
			output = new double[layers.get(layers.size() - 1).nodesA.length];
			for (int i = 0; i < output.length; i++) {
				output[i] = layers.get(layers.size() - 1).nodesA[i].activation;
			}
		} else {
			output = new double[layers.get(layers.size() - 1).nodes.size()];
			for (int i = 0; i < output.length; i++) {
				output[i] = layers.get(layers.size() - 1).nodes.get(i).activation;
			}
		}
		return output;
	}
	
	public void setInput(double[] inputs) {
		if (frozen) {
			Node[] iNodes = layers.get(0).nodesA;
			assert(inputs.length == iNodes.length);
			for (int i = 0; i < inputs.length; i++) {
				iNodes[i].activation = inputs[i];
			}
		} else {
			ArrayList<Node> iNodes = layers.get(0).nodes;
			assert(inputs.length == iNodes.size());
			for (int i = 0; i < inputs.length; i++) {
				iNodes.get(i).activation = inputs[i];
			}
		}
	}
	
	public void setTargets(double[] targets) {
		if (frozen) {
			Node[] outputs = layers.get(layers.size() - 1).nodesA;
			assert(outputs.length == targets.length);
			for (int i = 0; i < targets.length; i++) {
				outputs[i].delta = targets[i] - outputs[i].activation;
			}
		} else {
			ArrayList<Node> outputs = layers.get(layers.size() - 1).nodes;
			assert(targets.length == outputs.size());
			for (int i = 0; i < targets.length; i++) {
				outputs.get(i).delta = targets[i] - outputs.get(i).activation;
			}
		}
	}
	
	public void calculateDelta() {
		// Don't calc delta for output layer.
		for (int i = layers.size() - 2; i >= 0; i--) {
			layers.get(i).calculateDelta();
		}
	}
	
	public void adjustWeights(double n, double m) {
		for (int i = layers.size() - 1; i >= 0; i--) {
			layers.get(i).adjustWeights(n, m);
		}
	}
	
	public void update() {
		for (Layer l : layers) { l.update(); }
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
