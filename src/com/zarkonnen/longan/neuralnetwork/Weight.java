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