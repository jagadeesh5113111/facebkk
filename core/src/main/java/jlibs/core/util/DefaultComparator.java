/**
 * Copyright 2015 Santhosh Kumar Tekuri
 *
 * The JLibs authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package jlibs.core.util;

import jlibs.core.lang.NotImplementedException;

import java.util.Comparator;

/**
 * This is default implementation of {@link Comparator}, which can compare 
 * objects implementing {@link Comparable}.
 * <p>
 * <b>Null-Friendly:</b>
 * <p>
 * {@code null} is treated less than {@code non-null}.
 * <pre class="prettyprint">
 * Comparator<String> comp = new DefaultComparator<String>();
 * System.out.println(comp.compareTo(null, "helloworld")); // prints -1
 * System.out.println(comp.compare("helloworld", null)); // prints 1
 * System.out.println(comp.compare(null, null)); // prints 0
 * </pre>
 * 
 * If the objects compared doesn't implement {@link Comparable}, it throws
 * {@link NotImplementedException}.<br>
 * You can extend {@link DefaultComparator} for any objects that doesn't implement {@link Comparable}.<br>
 * For Example:
 * <pre class="prettyprint">
 * class Employee{
 *     int marks;
 *     int age;
 * 
 *     public Employee(int marks, int age){
 *         this.marks = marks;
 *         this.age = age;
 *     }
 * }
 * 
 * class EmployeeAgeComparator extends DefaultComparator<Employee>{
 *     &#064;Override
 *     protected int _compare(Employee emp1, Employee emp2){
 *         return emp1.age - emp2.age;
 *     }
 * } 
 * </pre>
 * 
 * the {@link #compare(Object, Object) compare(...)} method in {@link DefaultComparator} is final. 
 * This method takes care of comparing values involving {@code nulls}. If both arguments are {@code non-null},
 * then it delegates the comparison to {@link #_compare(Object, Object) _compare(...)}
 * <p>
 * So it is guaranteed that, both arguments of {@link #_compare(Object, Object) _compare(...)} are {@code non-null}.
 *  
 * @author Santhosh Kumar T
 */
public class DefaultComparator<T> implements Comparator<T>{
    /**
     * this method can handle nulls ( null&lt;non-null )
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

    /**
     * Arguments {@code o1} and {@code o2} will be non-null
     */
    @SuppressWarnings({"unchecked"})
    protected int _compare(T o1, T o2){
        if(o1 instanceof Comparable && o2 instanceof Comparable)
            return ((Comparable<T>)o1).compareTo(o2);
        throw new NotImplementedException("can't compare objects of type: "+o1.getClass());
    }
}
