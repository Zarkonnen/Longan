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

import java.util.ArrayList;

public class Layer {
	public ArrayList<Node> nodes;
	public ArrayList<Weight> weights;
	public final String name;
	
	public Node[] nodesA;
	public Weight[] weightsA;
	
	boolean frozen = false;
	
	public void freeze() {
		if (frozen) { return; }
		for (Node n : nodes) { n.freeze(); }
		for (Weight w : weights) { w.freeze(); }
		nodesA = nodes.toArray(new Node[0]);
		weightsA = weights.toArray(new Weight[0]);
		frozen = true;
		nodes = null;
		weights = null;
	}

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
		if (frozen) {
			for (int i = 0; i < nodesA.length; i++) {
				//nodesA[i].update();
				// qqDPS
				Node n = nodesA[i];
				if (n.incomingA.length == 0) { continue; }
				double sum = 0.0;
				for (int j = 0; j < n.incomingA.length; j++) {
					sum += n.incomingA[j].input.activation * n.incomingA[j].weight.value;
				}
				n.activation = Math.tanh(sum);
			}
		} else {
			for (Node n : nodes) { n.update(); }
		}
	}
	
	public void calculateDelta() {
		if (frozen) {
			for (int i = 0; i < nodesA.length; i++) {
				nodesA[i].calculateDelta();
			}
		} else {
			for (Node n : nodes) { n.calculateDelta(); }
		}
	}
	
	public void adjustWeights(double n, double m) {
		if (frozen) {
			for (int i = 0; i < weightsA.length; i++) {
				weightsA[i].adjust(n, m);
			}
		} else {
			for (Weight w : weights) { w.adjust(n, m); }
		}
	}
}
