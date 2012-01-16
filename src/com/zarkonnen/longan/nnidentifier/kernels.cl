kernel void nn(
    int numNodes,
    int numInputs,
    global const float* prevLayer,
    global const int*   connections,
    global const float* weights,
    global const float* biases,
    global       float* nextLayer)
{
  int n = get_global_id(0);
  if (n >= numNodes) { return; }
  float x = biases[n];
  for (int i = 0; i < numInputs; i += 1) {
    x += weights[n * numInputs + i]
         *
         prevLayer[connections[n * numInputs + i]];
  }
  nextLayer[n] = tanh(x);
}

kernel void nn4InputsNoBias(
    int numNodes,
    global const float* prevLayer,
    global const int*   connections,
    global const float* weights,
    global       float* nextLayer)
{
  int n = get_global_id(0);
  if (n >= numNodes) { return; }
  nextLayer[n] = tanh(
    weights[n * 4    ] * prevLayer[connections[n * 4    ]] +
    weights[n * 4 + 1] * prevLayer[connections[n * 4 + 1]] +
    weights[n * 4 + 2] * prevLayer[connections[n * 4 + 2]] +
    weights[n * 4 + 3] * prevLayer[connections[n * 4 + 3]]
  );
}