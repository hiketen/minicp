/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.util.GraphUtil;
import minicp.util.GraphUtil.Graph;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Arc Consistent AllDifferent Constraint
 *
 * Algorithm described in
 * "A filtering algorithm for constraints of difference in CSPs" J-C. Régin, AAAI-94
 */
public class AllDifferentDC extends AbstractConstraint {

    private IntVar[] x;

    private final MaximumMatching maximumMatching;

    private final int nVar;
    private int nVal;

    // residual graph
    private ArrayList<Integer>[] in;
    private ArrayList<Integer>[] out;
    private int nNodes;
    protected Graph g = new Graph() {
        @Override
        public int n() {
            return nNodes;
        }

        @Override
        public Iterable<Integer> in(int idx) {
            return in[idx];
        }

        @Override
        public Iterable<Integer> out(int idx) {
            return out[idx];
        }
    };

    private int[] match;
    private boolean[] matched;

    private int minVal;
    private int maxVal;

    public AllDifferentDC(IntVar... x) {
        super(x[0].getSolver());
        this.x = x;
        maximumMatching = new MaximumMatching(x);
        match = new int[x.length];
        this.nVar = x.length;
    }

    @Override
    public void post() {
        for (int i = 0; i < nVar; i++) {
            x[i].propagateOnDomainChange(this);
        }
        updateRange();

        matched = new boolean[nVal];
        nNodes = nVar + nVal + 1;
        in = new ArrayList[nNodes];
        out = new ArrayList[nNodes];
        for (int i = 0; i < nNodes; i++) {
            in[i] = new ArrayList<>();
            out[i] = new ArrayList<>();
        }
        propagate();
    }

    private void updateRange() {
        minVal = Integer.MAX_VALUE;
        maxVal = Integer.MIN_VALUE;
        for (int i = 0; i < nVar; i++) {
            minVal = Math.min(minVal, x[i].min());
            maxVal = Math.max(maxVal, x[i].max());
        }
        nVal = maxVal - minVal + 1;
    }


    private void updateGraph() {
        nNodes = nVar + nVal + 1;
        int sink = nNodes - 1;
        for (int j = 0; j < nNodes; j++) {
            in[j].clear();
            out[j].clear();
        }

        // debug------------------------
	//boolean DBG = true;
	boolean DBG = false;
	if (DBG) {
	  System.out.printf("nVar:%d, nNodes:%d, minVal:%d, maxVal:%d\n", nVar, nNodes, minVal, maxVal);
	  System.out.printf("match:");
	  for (int i = 0; i < nVar; i++) {
	    System.out.printf(" [%d]:%d;", i, match[i]);
	  }
	  System.out.printf("\n");
	}

        // continue the implementation for representing the residual graph
        for (int i = 0; i < nVar; i++) {
	  if (match[i] != MaximumMatching.NONE){
            int valNodeIdx = nVar + match[i] - minVal;
	    in[i].add(valNodeIdx);
	    out[valNodeIdx].add(i);

	    matched[match[i]-minVal] = true;

	    out[sink].add(valNodeIdx);
	    in[valNodeIdx].add(sink);

	  }
	}
        for (int i = nVar; i < nNodes-1; i++) {
	  if (!matched[i-nVar]) {
	    in[sink].add(i);
	    out[i].add(sink);
	  }
	}

        for (int i = 0; i < nVar; i++) {
	  for (int v = x[i].min(); v <= x[i].max(); v++) { // TODO-SK: replace with fillarray
	    if (x[i].contains(v)) {
	      if (match[i] != v) {
		in[nVar + v - minVal].add(i);
		out[i].add(nVar + v - minVal);
	      } 
	    }
	  }
	}

	// debug
	/*
        for (int i = nVar; i < nNodes-1; i++) {
	  System.out.printf("in[%d](val=%d):  ", i, minVal+i-nVar);
	  for (Integer varIdx: in[i])  {
	    System.out.printf(" %d", varIdx);
	  }
	  System.out.printf("\n");
	}
	*/


	
    }


    @Override
    public void propagate() {
        // TODO Implement the filtering
        // hint: use maximumMatching.compute(match) to update the maximum matching
        //       use updateRange() to update the range of values
        //       use updateGraph() to update the residual graph
        //       use  GraphUtil.stronglyConnectedComponents to compute SCC's
        maximumMatching.compute(match);
        updateRange();
        updateGraph();
	int[] sccIdx;
	sccIdx = GraphUtil.stronglyConnectedComponents(g);

        // debug------------------------
	//boolean DBG = true;
	boolean DBG = false;
	if (DBG) {
	  System.out.printf("nNodes:%d\n", nNodes);

	  System.out.printf("matched:", nNodes);
	  for (int i = 0; i < nVal; i++) {
	    System.out.printf(" [%d]:%s;", i+minVal, matched[i]);
	  }
	  System.out.printf("\n");

	  System.out.printf("sccIdx:", nNodes);
	  for (int i = 0; i < nNodes; i++) {
	    System.out.printf(" [%d]:%d;", i, sccIdx[i]);
	  }
	  System.out.printf("\n");

	}

        // Remove the non-matching edges that are between SCCs
	for (int i = nVar; i < nNodes-1; i++) {
	  //System.out.printf("Processing value node %d(%d) \n", i, i-nVar+minVal);
	  for (Integer varIdx: in[i])  {
	    if (varIdx == nNodes-1)
	      continue;
	    //System.out.printf("  Processing edge from %d to %d(%d) \n", varIdx, i, i-nVar+minVal);
	    if (sccIdx[varIdx] != sccIdx[i]) {
	      //System.out.printf("   Removing value %d from x[%d] domain \n", i-nVar+minVal, i);
	      x[varIdx].remove(i-nVar+minVal);
	      //System.out.printf("   Removing edge \n");
	      //in[i].remove(varIdx);
	      //System.out.printf("    out[%d]", varIdx);
	      //for (Integer valIdx: out[varIdx])  
		//System.out.printf(" %d", valIdx);
	      //System.out.printf("\n");
	      //System.out.printf("Removing %d from out[%d]\n", i, varIdx);
	      //out[varIdx].remove(Integer.valueOf(i));
	      //System.out.printf("Removing one\n");
	    }
	  }
	}
	
    }
}
