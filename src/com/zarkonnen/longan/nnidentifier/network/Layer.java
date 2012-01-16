package com.zarkonnen.longan.nnidentifier.network;

import java.util.ArrayList;

public class Layer {
	public final ArrayList<Node> nodes;
	public final ArrayList<Weight> weights;
	public final String name;

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
		for (Node n : nodes) { n.update(); }
	}
	
	public void calculateDelta() {
		for (Node n : nodes) { n.calculateDelta(); }
	}
	
	public float adjustWeights(float n, float m) {
		float total = 0.0f;
		for (Weight w : weights) { total += Math.abs(w.adjust(n, m)); }
		return total / weights.size();
	}

	String getDetails() {
		String d = "";
		for (Node n : nodes) {
			d += n.name + " act=" + n.activation + " delta=" + n.delta + "\n";
			/*for (Connection c : n.incoming) {
				d += c.weight.value + "\n";
			}*/
		}
		return d;
	}
}
