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

public class Layer {
	public final ArrayList<Node> nodes;
	public final ArrayList<Weight> weights;
	public final String name;

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
		for (Node n : nodes) { n.update(); }
	}
	
	public void calculateDelta() {
		for (Node n : nodes) { n.calculateDelta(); }
	}
	
	public double adjustWeights(double n, double m) {
		double total = 0.0;
		for (Weight w : weights) { total += Math.abs(w.adjust(n, m)); }
		return total / weights.size();
	}

	String getDetails() {
		String d = "";
		for (Node n : nodes) {
			d += n.name + " act=" + n.activation + " delta=" + n.delta + "\n";
		}
		return d;
	}
}
