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

import minicp.cp.Factory;
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;


/**
 *
 * Element Constraint modeling {@code array[y] = z}
 *
 */
public class Element1DDomainConsistent extends AbstractConstraint {

    private final int[] t;
    private final IntVar y;
    private final IntVar z;

    private int[] yDomValues;
    private int[] zDomValues;
    /**
     * Creates an element constraint {@code array[y] = z}
     *
     * @param array the array to index
     * @param y the index variable
     * @param z the result variable
     */
    public Element1DDomainConsistent(int[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.t = array;
        this.y = y;
        this.z = z;
    }

    @Override
    public void post() {
      y.removeBelow(0);
      y.removeAbove(t.length - 1);
      yDomValues = new int[y.size()];
      zDomValues = new int[z.size()];
      y.propagateOnDomainChange(this);
      z.propagateOnDomainChange(this);
      propagate();
    }


    @Override
    public void propagate() {

      int nYVal, nZVal;

      nYVal = y.fillArray(yDomValues);
      for (int i = 0; i < nYVal; i++)
        if (!z.contains(t[yDomValues[i]]))
          y.remove(yDomValues[i]);



      //System.out.printf("z:");
      //for (int v = z.min(); v <= z.max(); v++)
      //  if (z.contains(v))
      //  System.out.printf(" %d", v);
      //System.out.printf("\n\n");



      nZVal = z.fillArray(zDomValues);
      //System.out.printf("nZVal=%d\n", nZVal);

      //System.out.printf("zDomValues:");
      //for (int i = 0; i < nZVal; i++)
      //    System.out.printf(" %d", zDomValues[i]);
      //System.out.printf("\n\n");

      Integer [] zSup = new Integer[z.size()];
      for (int i = 0; i < zSup.length; i++)
        zSup[i] = 0;

      nYVal = y.fillArray(yDomValues);

      for (int i = 0; i < nYVal; i++)
        for (int j = 0; j < nZVal; j++)
          if (t[yDomValues[i]] == zDomValues[j]) {
            zSup[j] += 1;
            break;
          }

      for (int i = 0; i < nZVal; i++)
        if (zSup[i]==0)
          z.remove(zDomValues[i]);

    }
}
