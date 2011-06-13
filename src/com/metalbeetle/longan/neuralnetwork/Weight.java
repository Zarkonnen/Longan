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
