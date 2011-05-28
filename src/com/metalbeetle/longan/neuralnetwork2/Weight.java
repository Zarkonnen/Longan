package com.metalbeetle.longan.neuralnetwork2;

import java.util.ArrayList;

public final class Weight {
	public double value;
	public double lastAdjustment;
	public ArrayList<Connection> connections = new ArrayList<Connection>();
	
	Connection[] connectionsA;
	
	boolean frozen;
	
	void freeze() {
		if (frozen) { return; }
		connectionsA = connections.toArray(new Connection[0]);
		frozen = true;
	}

	public Weight(double value) {
		this.value = value;
	}

	void adjust(double n, double m) {
		double change = 0.0;
		if (frozen) {
			for (int i = 0; i < connectionsA.length; i++) {
				change += connectionsA[i].output.delta * connectionsA[i].input.activation;
			}
		} else {
			for (Connection c : connections) {
				change += c.output.delta * c.input.activation;
			}
		}
		value += n * change + m * lastAdjustment;
		lastAdjustment = change;
	}
}
