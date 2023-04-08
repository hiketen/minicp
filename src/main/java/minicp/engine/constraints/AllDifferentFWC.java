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
import minicp.state.StateInt;
import minicp.util.exception.NotImplementedException;

import java.util.stream.IntStream;

/**
 * Forward Checking filtering AllDifferent Constraint
 *
 * Whenever one variable is fixed, this value
 * is removed from the domain of other variables.
 * This filtering is weaker than the {@link AllDifferentDC}
 * but executes faster.
 */
public class AllDifferentFWC extends AbstractConstraint {

    private IntVar[] x;
    private int[] fixed;
    private StateInt nFixed;
    

    public AllDifferentFWC(IntVar... x) {
        super(x[0].getSolver());
        this.x = x;
        fixed = IntStream.range(0, x.length).toArray();
        nFixed = getSolver().getStateManager().makeStateInt(0);
        
    }

    @Override
    public void post() {
      for (int i = 0; i < x.length; i++) {
        x[i].propagateOnFix(this);
      }
    }

    @Override
    public void propagate() {
      // TODO use the sparse-set trick as seen in Sum.java
      int nF = nFixed.value();
      for (int i = nF; i < x.length; i++) {
        int idx = fixed[i];
        if (x[idx].isFixed()) {

          // swap the variables
          fixed[i] = fixed[nF];
          fixed[nF] = idx;
          nF++;

          // filter the unfixed variables
          for (int j = nF; j < x.length; j++) {
            x[fixed[j]].remove(x[idx].min());
          }
        }
      }
      nFixed.setValue(nF);
    }
}
