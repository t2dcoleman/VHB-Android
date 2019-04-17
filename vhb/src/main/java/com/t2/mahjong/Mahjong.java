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
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

class Mahjong {
    private final MahjongLayout mLayout;
    final List<Tile> mTiles;

    public Mahjong(MahjongLayout layout) {
        super();
        mLayout = layout;
        mTiles = new ArrayList<>();

        if (mLayout instanceof MahjongStatefulLayout) {
            mTiles.addAll(((MahjongStatefulLayout) mLayout).getTiles());
        } else {
            while (true) {
                if (generateTiles()) {
                    break;
                }
                mTiles.clear();
            }
        }
    }

    public int getDepth() {
        int d = -1;
        Tile tile = null;
        TileSlot slot = null;
        int size = mTiles.size();
        for (int i = 0; i < size; i++) {
            tile = mTiles.get(i);
            if (!tile.isVisible()) {
                continue;
            }
            slot = tile.getSlot();
            if (slot.getLayer() > d) {
                d = slot.getLayer();
            }
        }
        return d + 1;
    }

    public List<Tile> getFreeTiles() {
        List<Tile> freeTiles = new ArrayList<>();
        for (Tile tile : mTiles) {
            if (isFree(tile)) {
                freeTiles.add(tile);
            }
        }
        return freeTiles;
    }

    public int getGreatestRightmostDepth() {
        int w = -1;
        int d = -1;
        Tile tile = null;
        TileSlot slot = null;
        int size = mTiles.size();
        for (int i = 0; i < size; i++) {
            tile = mTiles.get(i);
            if (!tile.isVisible()) {
                continue;
            }
            slot = tile.getSlot();
            if (slot.getCol() > w) {
                w = slot.getCol();
                d = slot.getLayer();
            } else if (slot.getCol() == w && slot.getLayer() > d) {
                d = slot.getLayer();
            }
        }
        return d + 1;
    }

    public int getGreatestTopmostDepth() {
        int h = Integer.MAX_VALUE;
        int d = -1;
        Tile tile = null;
        TileSlot slot = null;
        int size = mTiles.size();
        for (int i = 0; i < size; i++) {
            tile = mTiles.get(i);
            if (!tile.isVisible()) {
                continue;
            }
            slot = tile.getSlot();
            if (slot.getRow() < h) {
                h = slot.getRow();
                d = slot.getLayer();
            } else if (slot.getRow() == h && slot.getLayer() > d) {
                d = slot.getLayer();
            }
        }
        return d + 1;
    }

    public int getHeight() {
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = -1;
        Tile tile = null;
        TileSlot slot = null;
        int size = mTiles.size();
        for (int i = 0; i < size; i++) {
            tile = mTiles.get(i);
            if (!tile.isVisible()) {
                continue;
            }
            slot = tile.getSlot();
            if (slot.getRow() < minHeight) {
                minHeight = slot.getRow();
            }
            if (slot.getRow() > maxHeight) {
                maxHeight = slot.getRow();
            }
        }

        return (maxHeight - minHeight) + 2;
    }

    /**
     * @return the layout
     */
    public MahjongLayout getLayout() {
        return mLayout;
    }

    public int getMinCol() {
        int minCol = Integer.MAX_VALUE;
        Tile tile = null;
        TileSlot slot = null;
        int size = mTiles.size();
        for (int i = 0; i < size; i++) {
            tile = mTiles.get(i);
            if (!tile.isVisible()) {
                continue;
            }
            slot = tile.getSlot();
            if (slot.getCol() < minCol) {
                minCol = slot.getCol();
            }
        }

        return minCol;
    }

    public int getMinRow() {
        int minRow = Integer.MAX_VALUE;
        Tile tile = null;
        TileSlot slot = null;
        int size = mTiles.size();
        for (int i = 0; i < size; i++) {
            tile = mTiles.get(i);
            if (!tile.isVisible()) {
                continue;
            }
            slot = tile.getSlot();
            if (slot.getRow() < minRow) {
                minRow = slot.getRow();
            }
        }

        return minRow;
    }

    /**
     * @return the tiles
     */
    public List<Tile> getTiles() {
        return mTiles;
    }

    public int getWidth() {
        int minWidth = Integer.MAX_VALUE;
        int maxWidth = -1;
        Tile tile = null;
        TileSlot slot = null;
        int size = mTiles.size();
        for (int i = 0; i < size; i++) {
            tile = mTiles.get(i);
            if (!tile.isVisible()) {
                continue;
            }
            slot = tile.getSlot();
            if (slot.getCol() < minWidth) {
                minWidth = slot.getCol();
            }
            if (slot.getCol() > maxWidth) {
                maxWidth = slot.getCol();
            }
        }
        return (maxWidth - minWidth) + 2;
    }

    public boolean isFree(Tile tile) {
        if (!tile.isVisible()) {
            return false;
        }

        boolean blockedLeft = false;
        boolean blockedRight = false;
        TileSlot slot = tile.getSlot();
        for (Tile t : mTiles) {
            if (t == tile || !t.isVisible()) {
                continue;
            }

            TileSlot s = t.getSlot();
            if (s.getLayer() == slot.getLayer() + 1 &&
                    (s.getCol() >= slot.getCol() - 1 && s.getCol() <= slot.getCol() + 1) &&
                    (s.getRow() >= slot.getRow() - 1 && s.getRow() <= slot.getRow() + 1)) {
                return false;
            }

            if (s.getLayer() == slot.getLayer() && (s.getRow() >= slot.getRow() - 1 && s.getRow() <= slot.getRow() + 1)) {
                if (s.getCol() == slot.getCol() - 2) {
                    blockedLeft = true;
                }
                if (s.getCol() == slot.getCol() + 2) {
                    blockedRight = true;
                }
                if (blockedLeft && blockedRight) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean generateTiles() {
        for (TileSlot slot : mLayout.getSlots()) {
            Tile tile = new Tile(slot);
            mTiles.add(tile);
        }

        Collections.sort(mTiles, (lhs, rhs) -> {
            final int layerDelta = lhs.getSlot().getLayer() - rhs.getSlot().getLayer();
            if (layerDelta != 0) {
                return layerDelta;
            }

            final int xDelta = lhs.getSlot().getCol() - rhs.getSlot().getCol();
            final int yDelta = lhs.getSlot().getRow() - rhs.getSlot().getRow();
            if (xDelta > yDelta) {
                return -1;
            } else if (xDelta < yDelta) {
                return 1;
            }

            return 0;
        });

        ArrayList<Integer> tileTypes = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            tileTypes.add(i);
        }
        Collections.shuffle(tileTypes);

        int seasonIndex = 0;
        int flowerIndex = 0;

        ArrayList<Integer> finalTiles = new ArrayList<>();
        for (int i = 0; i < mTiles.size() / 4; i++) {
            finalTiles.add(tileTypes.get(i));
            finalTiles.add(tileTypes.get(i));
        }
        Collections.shuffle(finalTiles);

        try {
            for (int i = 0; i < mTiles.size() / 2; i++) {
                List<Tile> freeTiles = getFreeTiles();
                Collections.shuffle(freeTiles);
                if (freeTiles.isEmpty()) {
                    Timber.wtf("Something is terribly wrong, no more free tiles");
                }

                int matchId = finalTiles.get(i);
                if (matchId == 33) {
                    // seasons
                    freeTiles.get(0).setSubId(seasonIndex++);
                    freeTiles.get(1).setSubId(seasonIndex++);
                } else if (matchId == 34) {
                    // flowers
                    freeTiles.get(0).setSubId(flowerIndex++);
                    freeTiles.get(1).setSubId(flowerIndex++);
                }
                freeTiles.get(0).setMatchId(matchId);
                freeTiles.get(0).setVisible(false);
                freeTiles.get(1).setMatchId(matchId);
                freeTiles.get(1).setVisible(false);
            }

            resetTiles();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void resetTiles() {
        for (Tile tile : mTiles) {
            tile.setVisible(true);
        }
    }

}
