package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridENS extends RedBlackTreeSweep {
    private static final int MAX_SIZE = 400;

    private int[][] ensIndices;
    private int[] ensSize;
    private int[] ensRank;
    private int[] ensNext;
    private int ensFirst, ensCount;
    private int lastCurr, lastIndex;

    public RedBlackTreeSweepHybridENS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        ensIndices = new int[MAX_SIZE][MAX_SIZE];
        ensNext = new int[MAX_SIZE];
        ensRank = new int[MAX_SIZE];
        ensSize = new int[MAX_SIZE];
    }

    private void ensInit() {
        ensFirst = -1;
        ensCount = 0;
    }

    private int ensFindRank(int index, int maxObj) {
        int curr = ensFirst;
        int dominatingIndex = -1;
        int pointRank = ranks[index];
        lastCurr = -1;
        lastIndex = index;
        while (curr != -1 && ensRank[curr] >= pointRank) {
            boolean dominates = false;
            int[] ensCurrIndices = ensIndices[curr];
            for (int i = ensSize[curr] - 1; i >= 0; --i) {
                if (strictlyDominatesAssumingNotSame(ensCurrIndices[i], index, maxObj)) {
                    dominates = true;
                    break;
                }
            }
            if (dominates) {
                dominatingIndex = curr;
                break;
            }
            lastCurr = curr;
            curr = ensNext[curr];
        }
        return dominatingIndex == -1 ? 0 : ensRank[dominatingIndex] + 1;
    }

    private int ensCreateNewRow(int index, int pointRank, int next) {
        ensRank[ensCount] = pointRank;
        ensNext[ensCount] = next;
        ensSize[ensCount] = 1;
        ensIndices[ensCount][0] = index;
        return ensCount++;
    }

    private void ensInsertPoint(int index) {
        int pointRank = ranks[index];
        if (ensFirst == -1 || ensRank[ensFirst] < pointRank) {
            ensFirst = ensCreateNewRow(index, pointRank, ensFirst);
        } else if (pointRank == ensRank[ensFirst]) {
            ensIndices[ensFirst][ensSize[ensFirst]++] = index;
        } else {
            int prev = index == lastIndex && lastCurr >= 0 ? lastCurr : ensFirst;
            if (ensRank[prev] == pointRank) {
                ensIndices[prev][ensSize[prev]++] = index;
            } else {
                int next = ensNext[prev];
                while (next != -1 && ensRank[next] > pointRank) {
                    if (ensRank[next] >= ensRank[prev]) {
                        throw new AssertionError();
                    }
                    prev = next;
                    next = ensNext[next];
                }
                if (next != -1 && ensRank[next] == pointRank) {
                    ensIndices[next][ensSize[next]++] = index;
                } else {
                    ensNext[prev] = ensCreateNewRow(index, pointRank, next);
                }
            }
        }
    }

    @Override
    protected void closeImpl() throws Exception {
        super.closeImpl();

        ensIndices = null;
        ensNext = null;
        ensRank = null;
        ensSize = null;
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (tree sweep, hybrid with ENS)";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 1: return false;
            case 2: return size < 100;
            default: return size < MAX_SIZE;
        }
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        ensInit();
        int minOverflow = until;
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            int r = Math.max(ranks[ii], ensFindRank(ii, obj));
            ranks[ii] = r;
            if (r <= maximalMeaningfulRank) {
                ensInsertPoint(ii);
            } else if (minOverflow > i) {
                minOverflow = i;
            }
        }
        return kickOutOverflowedRanks(minOverflow, until);
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        ensInit();
        int minOverflowed = weakUntil;
        for (int gi = goodFrom, wi = weakFrom; wi < weakUntil; ++wi) {
            while (gi < goodUntil && indices[gi] < indices[wi]) {
                ensInsertPoint(indices[gi++]);
            }
            int ii = indices[wi];
            int r = Math.max(ranks[ii], ensFindRank(ii, obj));
            ranks[ii] = r;
            if (minOverflowed > wi && r > maximalMeaningfulRank) {
                minOverflowed = wi;
            }
        }
        return kickOutOverflowedRanks(minOverflowed, weakUntil);
    }
}