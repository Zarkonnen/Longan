package com.metalbeetle.longan.neuralnetwork;

/*
 * Copyright 2011 David Stark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;

public class Connection {
	public final ArrayList<Node> inputs;
	public final Node output;
	public final Weight weight;
	
	public Connection(Node input, Node output, Weight weight) {
		inputs = new ArrayList<Node>();
		inputs.add(input);
		this.output = output;
		this.weight = weight;
		weight.connections.add(this);
		input.outgoing.add(this);
		output.incoming.add(this);
	}

	public Connection(ArrayList<Node> inputs, Node output, Weight weight) {
		this.inputs = inputs;
		this.output = output;
		this.weight = weight;
		weight.connections.add(this);
		for (Node input : inputs) { 
			input.outgoing.add(this);
		}
		output.incoming.add(this);
	}
}
