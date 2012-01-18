package com.zarkonnen.longan.nnidentifier.network;

import java.util.ArrayList;

public class Weight {
	public float value;
	public float lastAdjustment;
	public final ArrayList<Connection> connections = new ArrayList<Connection>();	
	
	public Weight(float value) {
		this.value = value;
	}

	double adjust(float n, float m) {
		float change = 0.0f;
		for (Connection c : connections) {
			change += c.output.delta * c.input.activation;
		}
		value += n * change + m * lastAdjustment;
		lastAdjustment = change;
		return change;
	}
}
