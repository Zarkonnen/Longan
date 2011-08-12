package com.metalbeetle.longan;

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

import java.awt.Rectangle;

public class LetterRect extends Rectangle {
	public double relativeLineOffset = 0.0;
	public double relativeSize = 1.0;
	public int numRegions = 1;
	public boolean[][] mask;

	public LetterRect(int x, int y, int width, int height) {
		super(x, y, width, height);
	}
	
	public void add(LetterRect lr2) {
		Rectangle newR = new Rectangle(this);
		newR.add(lr2);
		boolean[][] newMask = new boolean[newR.height][newR.width];
		for (int my = 0; my < newR.height; my++) {
			for (int mx = 0; mx < newR.width; mx++) {
				int thisY = newR.y + my - y;
				int thisX = newR.x + mx - x;
				if (thisY >= 0 && thisY < height && thisX >= 0 && thisX < width) {
					newMask[my][mx] |= mask[thisY][thisX];
				}
				int lr2Y = newR.y + my - lr2.y;
				int lr2X = newR.x + mx - lr2.x;
				if (lr2Y >= 0 && lr2Y < lr2.height && lr2X >= 0 && lr2X < lr2.width) {
					try {
						newMask[my][mx] |= lr2.mask[lr2Y][lr2X];
					} catch (Exception e) {
						System.out.println("JAM");
					}
				}
			}
		}
		
		mask = newMask;
		super.add(lr2);
	}
}
