package com.metalbeetle.longan.neuralnetwork;

import static com.metalbeetle.longan.neuralnetwork.Util.*;
import java.util.ArrayList;

public class Node {
	public final String name;
	public double activation;
	public double delta;
	public final ArrayList<Connection> incoming = new ArrayList<Connection>();
	public final ArrayList<Connection> outgoing = new ArrayList<Connection>();

	public Node(String name) {
		this.name = name;
	}
	
	public void update() {
		if (incoming.isEmpty()) { return; }
		double sum = 0.0;
		for (Connection in : incoming) {
			for (Node input : in.inputs) {
				sum += input.activation * in.weight.value / in.inputs.size();
			}
		}
		activation = sigmoid(sum);
	}

	void calculateDelta() {
		double error = 0.0;
		for (Connection out : outgoing) {
			error += out.output.delta * out.weight.value; // qqDPS Scale by size?
		}
		delta = dSigmoid(activation) * error;
	}
}
