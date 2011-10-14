package com.zarkonnen.longan;

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

import com.zarkonnen.longan.data.Letter;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Util {
	static BufferedImage cropMaskAndAdjust(BufferedImage src, Letter r, int intensityAdjustment) {
		BufferedImage maskedSrc = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
		Graphics g = maskedSrc.getGraphics();
		g.drawImage(
				src,
				0, 0,
				r.width, r.height,
				r.x, r.y,
				r.x + r.width, r.y + r.height,
				null);
		int white = Color.WHITE.getRGB();
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				boolean hasMask = false;
				for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
					int ny = y + dy;
					int nx = x + dx;
					if (ny >= 0 && ny < r.height && nx >= 0 && nx < r.width) {
						hasMask |= r.mask[ny][nx];
					}
				}}
				if (!hasMask) {
					maskedSrc.setRGB(x, y, white);
				} else {
					Color rgb = new Color(maskedSrc.getRGB(x, y));
					rgb = new Color(
						Math.min(255, Math.max(0, rgb.getRed() + intensityAdjustment)),
						Math.min(255, Math.max(0, rgb.getGreen() + intensityAdjustment)),
						Math.min(255, Math.max(0, rgb.getBlue() + intensityAdjustment))
					);
					maskedSrc.setRGB(x, y, rgb.getRGB());
				}
			}
		}
		return maskedSrc;
	}
}
