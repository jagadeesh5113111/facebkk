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

package jlibs.core.util;

import jlibs.core.lang.NotImplementedException;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

/**
 * @author Santhosh Kumar T
 */
public class DefaultComparator<T> implements Comparator<T>{

    /**
     * this method can handle nulls ( null<non-null )
     */
    @Override
    public final int compare(T o1, T o2){
        if(o1==o2)
            return 0;
        else if(o1==null)
            return -1;
        else if(o2==null)
            return 1;
        else
            return _compare(o1, o2);
    }

    @SuppressWarnings({"unchecked"})
    protected int _compare(@NotNull T o1, @NotNull T o2){
        if(o1 instanceof Comparable && o2 instanceof Comparable){
            if(o1.getClass().isInstance(o2))
                return ((Comparable)o1).compareTo(o2);
            else if(o2.getClass().isInstance(o1))
                return ((Comparable)o2).compareTo(o1);
        }
        throw new NotImplementedException("can't compare objects of type: "+o1.getClass());
    }
}