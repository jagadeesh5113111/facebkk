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

package jlibs.xml.sax.sniff;

import jlibs.core.lang.Util;
import jlibs.xml.DefaultNamespaceContext;
import jlibs.xml.dom.DOMNavigator;
import jlibs.xml.dom.DOMUtil;
import jlibs.xml.xpath.DefaultXPathVariableResolver;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunctionResolver;
import java.io.IOException;
import java.util.*;

/**
 * @author Santhosh Kumar T
 */
public class TestCase{
    static XPathEngine domEngine = new JDKEngine();
//    static XPathEngine domEngine = new JaxenEngine();

    String file;
    List<String> xpaths = new ArrayList<String>();
    List<QName> resultTypes = new ArrayList<QName>();
    List<Integer> hasAttributes = new ArrayList<Integer>();
    List<Integer> hasNamespaces = new ArrayList<Integer>();
    DefaultNamespaceContext nsContext = new DefaultNamespaceContext();
    DefaultNamespaceContext resultNSContext = new DefaultNamespaceContext();
    DefaultXPathVariableResolver variableResolver = new DefaultXPathVariableResolver();
    XPathFunctionResolver functionResolver;

    Document doc;

    public void createDocument() throws ParserConfigurationException, IOException, SAXException{
        doc = DOMUtil.newDocumentBuilder(true, false, true, false).parse(new InputSource(file));
    }

    List<Object> jdkResult = new ArrayList<Object>(xpaths.size());
    public List<Object> usingDOM() throws Exception{
        if(doc==null)
            createDocument();
        jdkResult = domEngine.evaluate(this, doc);
        doc = null;
        return jdkResult;
    }
    
    public void usingJDK() throws Exception{
        for(int i=0; i<xpaths.size(); i++){
            javax.xml.xpath.XPath xpathObj = XPathFactory.newInstance().newXPath();
            xpathObj.setXPathVariableResolver(variableResolver);
            xpathObj.setNamespaceContext(nsContext);
            jdkResult.add(xpathObj.evaluate(xpaths.get(i), doc, resultTypes.get(i)));
        }
    }

    List<Object> dogResult;
    public List<Object> usingXMLDog() throws Exception{
        InputSource source = new InputSource(file);
        XMLDog dog = new XMLDog(nsContext, variableResolver, functionResolver);
        jlibs.xml.sax.sniff.XPath xpathObjs[] = new jlibs.xml.sax.sniff.XPath[xpaths.size()];
        for(int i=0; i<xpaths.size(); i++){
            xpathObjs[i] = dog.add(xpaths.get(i));
            resultTypes.add(xpathObjs[i].resultType());
        }

        XPathResults dogResults = dog.sniff(source);
        resultNSContext = (DefaultNamespaceContext)dogResults.getNamespaceContext();

        dogResult = new ArrayList<Object>(xpaths.size());
        for(jlibs.xml.sax.sniff.XPath xpathObj: xpathObjs)
            dogResult.add(dogResults.getResult(xpathObj));
        return dogResult;
    }

    DOMNavigator navigator = new DOMNavigator();
    
    public void translateDOMResult(int test){
        Object obj = jdkResult.get(test);
        List<NodeItem> result = domEngine.translate(obj, resultNSContext);
        jdkResult.set(test, result);

        for(NodeItem item: result){
            if(item.type==NodeItem.ATTRIBUTE){
                hasAttributes.add(test);
                break;
            }else if(item.type==NodeItem.NAMESPACE){
                hasNamespaces.add(test);
                break;
            }
        }
    }

    private List<Integer> translated = new ArrayList<Integer>();
    public Object jdkResults(int i){
        if(!translated.contains(i) && resultTypes.get(i).equals(XPathConstants.NODESET)){
            translateDOMResult(i);
            translated.add(i);
        }
        return jdkResult.get(i);
    }

    public Object dogResults(int i){
        return dogResult.get(i);
    }

    Comparator<NodeItem> attrComparator = new Comparator<NodeItem>(){
        @Override
        public int compare(NodeItem item1, NodeItem item2){
            return item1.location.compareTo(item2.location);
        }
    };
    
    Comparator<NodeItem> nsComparator = new Comparator<NodeItem>(){
        @Override
        public int compare(NodeItem item1, NodeItem item2){
            String location1 = item1.location.substring(item1.location.lastIndexOf('/'));
            String location2 = item2.location.substring(item2.location.lastIndexOf('/'));
            return location1.compareTo(location2);
        }
    };

    @SuppressWarnings({"unchecked"})
    public boolean passed(int i){
        Object jdkResults = jdkResults(i);
        Object dogResults = dogResults(i);

        if(jdkResults instanceof TreeSet)
            jdkResult.set(i, jdkResults = new ArrayList((TreeSet)jdkResults));
        if(dogResults instanceof TreeSet)
            dogResult.set(i, dogResults = new ArrayList((TreeSet)dogResults));
        
        if(hasAttributes.contains(i)){
            Collections.sort((List<NodeItem>)jdkResults, attrComparator);
            Collections.sort((List<NodeItem>)dogResults, attrComparator);
        }else if(hasNamespaces.contains(i)){
            Collections.sort((List<NodeItem>)jdkResults, nsComparator);
            Collections.sort((List<NodeItem>)dogResults, nsComparator);
        }

        if(jdkResults instanceof List)
            return equals((List<NodeItem>)jdkResults, (List<NodeItem>)dogResults);
        else
            return jdkResults.equals(dogResults);
    }

    private boolean equals(List<NodeItem> jdkList, List<NodeItem> dogList){
        if(jdkList.size()!=dogList.size())
            return false;

        Iterator<NodeItem> jdkIter = jdkList.iterator();
        Iterator<NodeItem> dogIter = dogList.iterator();
        while(jdkIter.hasNext()){
            NodeItem jdkItem = jdkIter.next();
            NodeItem dogItem = dogIter.next();

            if(!Util.equals(jdkItem.value, dogItem.value))
                return false;

            String jdkLocation = jdkItem.location;
            String dogLocation = dogItem.location;
            if(dogItem.type==NodeItem.NAMESPACE){
                jdkLocation = jdkLocation.substring(jdkLocation.lastIndexOf('/'));
                dogLocation = dogLocation.substring(dogLocation.lastIndexOf('/'));
            }
            if(!jdkLocation.equals(dogLocation))
                return false;
        }
        
        return true;
    }

    /*-------------------------------------------------[ Printing ]---------------------------------------------------*/

    @SuppressWarnings({"unchecked"})
    public static int printResults(Object result){
        if(result instanceof Collection)
            return printResults((Collection)result);
        else{
            System.out.println(result);
            return -1;
        }
    }

    public static int printResults(Collection results){
        boolean first = true;
        for(Object item: results){
            if(first)
               first = false;
            else
                System.out.print(", ");
            System.out.print(item);
        }
        System.out.println();
        return results.size();
    }

    public void printResults(int i){
        System.out.println("            file : "+file);
        System.out.println("           xpath : "+xpaths.get(i));
        System.out.print("    Xalan result : ");
        int count = printResults(jdkResults(i));
        if(count!=-1)
            System.out.println(" Xalan hit-count : "+count);
        jdkResult.set(i, null);

        System.out.print("   XMLDog result : ");
        count = printResults(dogResults(i));
        if(count!=-1)
            System.out.println("XMLDog hit-count : "+count);
        dogResult.set(i, null);
        
        System.out.println("-------------------------------------------------");
    }
}