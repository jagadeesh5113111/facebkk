/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.xml.sax.sniff.model.expr.bool;

import jlibs.xml.sax.sniff.model.Datatype;
import jlibs.xml.sax.sniff.model.Node;

/**
 * @author Santhosh Kumar T
 */
public abstract class Relational extends Comparison{
    public Relational(Node contextNode, String name){
        super(contextNode, name);
    }

    @Override
    protected final boolean evaluateObjectObject(Object lhs, Object rhs){
        Double lhsNum = Datatype.asNumber(lhs);
        Double rhsNum = Datatype.asNumber(rhs);

        //noinspection SimplifiableIfStatement
        if(Double.isNaN(lhsNum) || Double.isNaN(rhsNum))
            return false;
        else
            return evaluateDoubles(lhsNum, rhsNum);
    }

    protected abstract boolean evaluateDoubles(Double lhs, Double rhs);
}