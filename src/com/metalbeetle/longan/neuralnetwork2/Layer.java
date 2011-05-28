package com.metalbeetle.longan.neuralnetwork2;

import java.util.ArrayList;

public class Layer {
	public ArrayList<Node> nodes;
	public ArrayList<Weight> weights;
	public final String name;
	
	public Node[] nodesA;
	public Weight[] weightsA;
	
	boolean frozen = false;
	
	public void freeze() {
		if (frozen) { return; }
		for (Node n : nodes) { n.freeze(); }
		for (Weight w : weights) { w.freeze(); }
		nodesA = nodes.toArray(new Node[0]);
		weightsA = weights.toArray(new Weight[0]);
		frozen = true;
		nodes = null;
		weights = null;
	}

	public Layer(String name) {
		this.name = name;
		nodes = new ArrayList<Node>();
		weights = new ArrayList<Weight>();
	}

	public Layer(String name, ArrayList<Node> nodes, ArrayList<Weight> weights) {
		this.name = name;
		this.nodes = nodes;
		this.weights = weights;
	}
	
	public void update() {
		if (frozen) {
			for (int i = 0; i < nodesA.length; i++) {
				nodesA[i].update();
			}
		} else {
			for (Node n : nodes) { n.update(); }
		}
	}
	
	public void calculateDelta() {
		if (frozen) {
			for (int i = 0; i < nodesA.length; i++) {
				nodesA[i].calculateDelta();
			}
		} else {
			for (Node n : nodes) { n.calculateDelta(); }
		}
	}
	
	public void adjustWeights(double n, double m) {
		if (frozen) {
			for (int i = 0; i < weightsA.length; i++) {
				weightsA[i].adjust(n, m);
			}
		} else {
			for (Weight w : weights) { w.adjust(n, m); }
		}
	}
}
