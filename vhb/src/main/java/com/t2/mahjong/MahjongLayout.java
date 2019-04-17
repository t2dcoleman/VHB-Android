/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * MahjongLib
 *
 * Copyright  2009-2014 United States Government as represented by
 * the Chief Information Officer of the National Center for Telehealth
 * and Technology. All Rights Reserved.
 *
 * Copyright  2009-2014 Contributors. All Rights Reserved.
 *
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE,
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY").
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES,
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 *
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: MahjongLib001
 * Government Agency Original Software Title: MahjongLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */
package com.t2.mahjong;

import java.util.ArrayList;
import java.util.List;

public abstract class MahjongLayout {
	final List<TileSlot> mSlots;

	MahjongLayout() {
		mSlots = new ArrayList<>();
	}

	/**
	 * @return the slots
	 */
	public List<TileSlot> getSlots() {
		return mSlots;
	}

	private void addGrid(int topLeftRow, int topLeftCol, int width, int height, int startDepth) {
		for (int row = 0; row < height * 2; row += 2) {
			for (int col = 0; col < width * 2; col += 2) {
				addStack(topLeftRow + row, topLeftCol + col, 1, startDepth);
			}
		}
	}

	protected void addPyramid(int topLeftRow, int topLeftCol, int size, int height, boolean halfstep, int startDepth) {
		int step = halfstep ? 1 : 2;
		for (int depth = 0; depth < height; depth++) {
			addGrid(topLeftRow + (depth * step), topLeftCol + (depth * step), size - (depth * step), size - (depth * step), depth
					+ startDepth);
		}
	}

	private void addStack(int row, int col, int height, int startDepth) {
		for (int i = 0; i < height; i++) {
			mSlots.add(new TileSlot(row, col, i + startDepth));
		}

		if (mSlots.size() > 144) {
			throw new IllegalArgumentException("More tiles than tile graphics support");
		}
	}

	protected void offset(int rows, int cols) {
		for (TileSlot slot : mSlots) {
			slot.setRow(slot.getRow() + rows);
			slot.setCol(slot.getCol() + cols);
		}
	}
}