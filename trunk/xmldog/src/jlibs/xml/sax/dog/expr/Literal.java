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

package jlibs.xml.sax.dog.expr;

import jlibs.core.lang.ImpossibleException;
import jlibs.xml.sax.dog.DataType;
import jlibs.xml.sax.dog.Scope;
import jlibs.xml.sax.dog.sniff.Event;

/**
 * @author Santhosh Kumar T
 */
public final class Literal extends Expression{
    public final Object literal;

    public Literal(Object literal, DataType dataType){
        super(Scope.GLOBAL, dataType);
        assert DataType.valueOf(literal)==dataType;
        this.literal = literal;
    }

    @Override
    public Object getResult(){
        return literal;
    }

    @Override
    public Object getResult(Event event){
        throw new ImpossibleException();
    }

    @Override
    public Expression simplify(){
        return this;
    }

    @Override
    public String toString(){
        if(resultType==DataType.STRING)
            return String.format("'%s'", literal);
        else
            return literal.toString();
    }
}