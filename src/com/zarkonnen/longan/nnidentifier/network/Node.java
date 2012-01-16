package com.zarkonnen.longan.nnidentifier.network;

import static com.zarkonnen.longan.nnidentifier.network.Util.*;
import java.util.ArrayList;

public class Node {
	public final String name;
	public float activation;
	public float delta;
	public final ArrayList<Connection> incoming = new ArrayList<Connection>();
	public final ArrayList<Connection> outgoing = new ArrayList<Connection>();

	public Node(String name) {
		this.name = name;
	}
	
	public void update() {
		if (incoming.isEmpty()) { return; }
		float sum = 0.0f;
		for (Connection in : incoming) {
			sum += in.input.activation * in.weight.value;
		}
		activation = (float) Math.tanh(sum);
	}

	void calculateDelta() {
		float error = 0.0f;
		for (Connection out : outgoing) {
			error += out.output.delta * out.weight.value;
		}
		delta = dSigmoid(activation) * error;
	}
}
