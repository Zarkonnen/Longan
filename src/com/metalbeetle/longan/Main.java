package com.metalbeetle.longan;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage: java -jar longan.jar path-to-image-file");
			return;
		}

		System.out.print(Longan.getDummyImplementation().recognize(ImageIO.read(new File(args[0]))));
	}
}
