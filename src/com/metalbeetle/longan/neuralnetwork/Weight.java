package com.metalbeetle.longan.neuralnetwork;

import java.util.ArrayList;

public class Weight {
	public double value;
	public double lastAdjustment;
	public final ArrayList<Connection> connections = new ArrayList<Connection>();

	public Weight(double value) {
		this.value = value;
	}

	double adjust(double n, double m) {
		double change = 0.0;
		for (Connection c : connections) {
			for (Node input : c.inputs) {
				change += c.output.delta * input.activation;
			}
		}
		value += n * change + m * lastAdjustment;
		lastAdjustment = change;
		return change;
	}
}
