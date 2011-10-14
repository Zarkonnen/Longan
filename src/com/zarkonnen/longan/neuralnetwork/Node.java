package com.zarkonnen.longan.neuralnetwork;

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

import static com.zarkonnen.longan.neuralnetwork.Util.*;
import java.util.ArrayList;

public class Node {
	public final String name;
	public double activation;
	public double delta;
	public ArrayList<Connection> incoming = new ArrayList<Connection>();
	public ArrayList<Connection> outgoing = new ArrayList<Connection>();

	public Node(String name) {
		this.name = name;
	}
	
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
	
	public void update() {
		if (frozen) {
			if (incomingA.length == 0) { return; }
			double sum = 0.0;
			for (int i = 0; i < incomingA.length; i++) {
				sum += incomingA[i].input.activation * incomingA[i].weight.value;
			}
			activation = Math.tanh(sum);
		} else {
			if (incoming.isEmpty()) { return; }
			double sum = 0.0;
			for (Connection in : incoming) {
				sum += in.input.activation * in.weight.value;
			}
			activation = Math.tanh(sum);
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
