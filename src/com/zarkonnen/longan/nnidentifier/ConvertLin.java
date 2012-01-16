package com.zarkonnen.longan.nnidentifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConvertLin {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		for (File f : new File("/Users/zar/Desktop/new_new_recog_nns/source/").listFiles()) {
			if (!f.getName().endsWith(".ldn")) { continue; }
			System.out.println(f.getName());
			FileInputStream fis = new FileInputStream(f);
			DiscriminatorNet net = new DiscriminatorNet();
			NetworkIO.input(net.nw, fis);
			fis.close();
			String newP = f.getAbsolutePath();
			newP = newP.substring(0, newP.length() - 1) + "f";
			FileOutputStream fos = new FileOutputStream(new File(newP));
			new FastLoadingNetwork().initFromNetwork(net.nw).saveWeights(fos);
			fos.close();
			fos = new FileOutputStream(new File("/Users/zar/Desktop/new_new_recog_nns/discriminator.lns"));
			new FastLoadingNetwork().initFromNetwork(net.nw).saveShape(fos);
			fos.close();
		}
	}
}
