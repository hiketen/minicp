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
import minicp.util.exception.NotImplementedException;

/**
 * Maximum Constraint
 */
public class Maximum extends AbstractConstraint {

    private final IntVar[] x;
    private final IntVar y;

    /**
     * Creates the maximum constraint y = maximum(x[0],x[1],...,x[n])?
     *
     * @param x the variable on which the maximum is to be found
     * @param y the variable that is equal to the maximum on x
     */
    public Maximum(IntVar[] x, IntVar y) {
        super(x[0].getSolver());
        assert (x.length > 0);
        this.x = x;
        this.y = y;
    }


    @Override
    public void post() {
        // TODO
        //  - call the constraint on all bound changes for the variables (x.propagateOnBoundChange(this))
        //  - call a first time the propagate() method to trigger the propagation
      for (int i=0; i < x.length; i++)
        x[i].propagateOnBoundChange(this);
      y.propagateOnBoundChange(this);
      propagate();
    }


    @Override
    public void propagate() {
      int found;

      //System.out.printf("\n@sp:");
      //for (int i=0; i < x.length; i++) 
      //  System.out.printf(" x[%d]: %d..%d;", i, x[i].min(), x[i].max());
      //System.out.printf(" y: %d..%d;\n", y.min(), y.max());

      //  - update the min and max values of each x[i] based on the bounds of y
      //  - update the min and max values of each y based on the bounds of all x[i]
      int xMaxMin = x[0].min();
      int xMaxMax = x[0].max();
      for (int i=0; i < x.length; i++) {
        //System.out.printf("processing i=%d\n", i);
        if (x[i].max() > y.max())
          x[i].removeAbove(y.max());
        if (x[i].max() > xMaxMax)
          xMaxMax = x[i].max();
        if (x[i].min() > xMaxMin)
          xMaxMin = x[i].min();
      }

      //System.out.printf("xMaxMin=%d; xMaxMax=%d\n", xMaxMin, xMaxMax);
      if (y.max() > xMaxMax)
        y.removeAbove(xMaxMax);

      if (y.min() < xMaxMin)
        y.removeBelow(xMaxMin);
      else if (y.min() > xMaxMin)  {
        // if there is only one x whose max is greater than ymin,
	// then it can only be the chosen one and it's lower range should be removed. 
        found = -1; // > 0 if one index found; -2 if more than one indices are found
        for (int i=0; i < x.length; i++) {
          if (x[i].max() > y.min()) {
	    if (found == -1)
	      found = i;
	    else {
	      found = -2;
	      break;
	    }
          }
        }
	if (found >= 0)
	  x[found].removeBelow(y.min());
      }
      
      //System.out.printf("@ep:");
      //for (int i=0; i < x.length; i++) 
      //  System.out.printf(" x[%d]: %d..%d;", i, x[i].min(), x[i].max());
      //System.out.printf(" y: %d..%d;\n\n", y.min(), y.max());

    }
}
