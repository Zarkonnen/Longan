package com.metalbeetle.longan.neuralnetwork2;

import static com.metalbeetle.longan.neuralnetwork2.Util.*;
import java.util.ArrayList;

public final class Node {
	public final String name;
	public double activation;
	public double delta;
	public ArrayList<Connection> incoming = new ArrayList<Connection>();
	public ArrayList<Connection> outgoing = new ArrayList<Connection>();
	
	Connection[] incomingA;
	Connection[] outgoingA;
	
	boolean frozen;
	
	public void freeze() {
		if (frozen) { return; }
		incomingA = incoming.toArray(new Connection[0]);
		outgoingA = outgoing.toArray(new Connection[0]);
		incoming = null;
		outgoing = null;
		frozen = true;
	}

	public Node(String name) {
		this.name = name;
	}
	
	public void update() {
		if (frozen) {
			if (incomingA.length == 0) { return; }
			double sum = 0.0;
			for (int i = 0; i < incomingA.length; i++) {
				sum += incomingA[i].input.activation * incomingA[i].weight.value;
			}
			activation = sigmoid(sum);
		} else {
			if (incoming.isEmpty()) { return; }
			double sum = 0.0;
			for (Connection in : incoming) {
				sum += in.input.activation * in.weight.value;
			}
			activation = sigmoid(sum);
		}
	}

	void calculateDelta() {
		double error = 0.0;
		if (frozen) {
			for (int i = 0; i < outgoingA.length; i++) {
				error += outgoingA[i].output.delta * outgoingA[i].weight.value;
			}
		} else {
			for (Connection out : outgoing) {
				error += out.output.delta * out.weight.value;
			}
		}
		delta = dSigmoid(activation) * error;
	}
}
