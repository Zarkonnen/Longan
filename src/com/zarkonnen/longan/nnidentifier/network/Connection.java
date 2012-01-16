package com.zarkonnen.longan.nnidentifier.network;

public class Connection {
	public final Node input;
	public final Node output;
	public final Weight weight;

	public Connection(Node input, Node output, Weight weight) {
		this.input = input;
		this.output = output;
		this.weight = weight;
		weight.connections.add(this);
		input.outgoing.add(this);
		output.incoming.add(this);
	}
}
