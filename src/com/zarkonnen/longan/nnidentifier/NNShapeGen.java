package com.zarkonnen.longan.nnidentifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class NNShapeGen {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream(new File(args[0]));
		new FastLoadingNetwork().initFromNetwork(new IdentifierNet(0).nw).saveShape(fos);
		fos.close();
		/*fos = new FileOutputStream(new File(args[1]));
		new FastLoadingNetwork().initFromNetwork(new DiscriminatorNet(0).nw).saveShape(fos);
		fos.close();*/
	}
}
