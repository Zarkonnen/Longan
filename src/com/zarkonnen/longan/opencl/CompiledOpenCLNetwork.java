package com.zarkonnen.longan.opencl;

import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;
import com.zarkonnen.longan.profilegen.network.Node;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.zarkonnen.longan.profilegen.network.Layer;
import com.zarkonnen.longan.profilegen.network.Network;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import static java.lang.Math.*;
import static com.jogamp.opencl.CLMemory.Mem.*;

public class CompiledOpenCLNetwork {
	Network n;
	CLBuffer<FloatBuffer> input; 
	int outputSize;
	ArrayList<CompiledLayer> layers;
	CLCommandQueue queue;
	CLContext context;
	int localWorkSize;
	
	CLKernel nnK;
	CLKernel nn4InputsNoBiasK;
	
	public CompiledOpenCLNetwork(Network n) {
		this.n = n;
	}
	
	public void init() throws IOException {
		context = CLContext.create();
		CLDevice device = context.getMaxFlopsDevice();
		queue = device.createCommandQueue();
		localWorkSize = min(device.getMaxWorkGroupSize(), 256);
		CLProgram program = context.createProgram(CompiledOpenCLNetwork.class.getResourceAsStream("kernels.cl")).build();
		nnK              = program.createCLKernel("nn");
		nn4InputsNoBiasK = program.createCLKernel("nn4InputsNoBias");
		
		input = context.createFloatBuffer(n.layers.get(0).nodes.size(), READ_ONLY);
		outputSize = n.layers.get(n.layers.size() - 1).nodes.size();
		
		layers = new ArrayList<CompiledLayer>();
		for (int i = 1; i < n.layers.size(); i++) {
			layers.add(new CompiledLayer(n.layers.get(i), n.layers.get(i - 1), context, queue));
		}
	}
	
	public void close() {
		context.release();
		context = null;
		queue = null;
	}
	
	public void test() {
		run(new float[n.layers.get(0).nodes.size()]);
	}
	
	public float[] run(float[] inputArray) {
		input.getBuffer().put(inputArray).rewind();
		queue.putWriteBuffer(input, false);
		CLBuffer<FloatBuffer> currentLayer = input;
		for (CompiledLayer cl : layers) {
			currentLayer = cl.run(currentLayer);
		}
		float[] output = new float[outputSize];
		queue.putReadBuffer(currentLayer, true);
		currentLayer.getBuffer().get(output).rewind();
		return output;
	}
	
	final class CompiledLayer {
		int numNodes;
		boolean hasBias;
		int numInputs;
		int total;
		CLBuffer<FloatBuffer> weights;
		CLBuffer<FloatBuffer> biases;
		CLBuffer<FloatBuffer> nextLayer; 
		CLBuffer<IntBuffer>   connections; 
		
		CompiledLayer(Layer layer, Layer prevLayer, CLContext context, CLCommandQueue queue) {
			// Analyse the layer.
			numNodes = layer.nodes.size();
			hasBias = hasBias(layer);
			numInputs = 0;
			for (Node node : layer.nodes) { numInputs = Math.max(numInputs, node.incoming.size()); }
			if (hasBias) {
				numInputs--;
				// Assuming final input is bias!
			}
			total = numNodes * numInputs;
			int[]   connectionsA = new int  [total];
			float[] weightsA     = new float[total];
			float[] biasesA      = new float[numNodes];
			for (int node = 0; node < numNodes; node++) {
				int inputsForThisNode = layer.nodes.get(node).incoming.size();
				if (hasBias) { inputsForThisNode--; }
				for (int input = 0; input < numInputs; input++) {
					if (input < inputsForThisNode) {
						connectionsA[node * numInputs + input] = prevLayer.nodes.indexOf(
							layer.nodes.get(node).incoming.get(input).input);
						weightsA[node * numInputs + input] = (float) layer.nodes.get(node).incoming.get(input).weight.value;
					} else {
						connectionsA[node * numInputs + input] = 0;
						weightsA[node * numInputs + input] = 0.0f;
					}
				}
				if (hasBias) {
					biasesA[node] = (float) layer.nodes.get(node).incoming.get(inputsForThisNode).weight.value;
				}
			}
			
			// Upload layer data.
			weights = context.createFloatBuffer(weightsA.length, READ_ONLY);
				weights.getBuffer().put(weightsA).rewind();
				queue.putWriteBuffer(weights, false);
			biases = context.createFloatBuffer(biasesA.length, READ_ONLY);
				biases.getBuffer().put(biasesA).rewind();
				queue.putWriteBuffer(biases, false);
			connections = context.createIntBuffer(connectionsA.length, READ_ONLY);
				connections.getBuffer().put(connectionsA).rewind();
				queue.putWriteBuffer(connections, false);
			nextLayer = context.createFloatBuffer(numNodes, READ_WRITE);
		}
		
		CLBuffer<FloatBuffer> run(CLBuffer<FloatBuffer> input) {
			int globalWorkSize = roundUp(localWorkSize, numNodes);
			if (!hasBias && numInputs == 4) {
				nn4InputsNoBiasK.rewind().putArg(numNodes).putArgs(input, connections, weights, nextLayer);
				queue.put1DRangeKernel(nn4InputsNoBiasK, 0, globalWorkSize, localWorkSize);
				return nextLayer;
			} else {
				nnK.rewind().putArg(numNodes).putArg(numInputs).putArgs(input, connections, weights, biases, nextLayer);
				queue.put1DRangeKernel(nnK, 0, globalWorkSize, localWorkSize);
				return nextLayer;
			}
		}
	}
	
	// NB: Horrible.
	static boolean hasBias(Layer layer) {
		return layer.nodes.get(0).incoming.get(layer.nodes.get(0).incoming.size() - 1).input.name.toLowerCase().contains("bias");
	}
	
	static int roundUp(int upTo, int amount) {
		int r = amount % upTo;
		if (r == 0) {
			return amount;
		} else {
			return amount + upTo - r;
		}
	}
}
