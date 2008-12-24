package jlibs.core.graph;

import java.util.regex.Pattern;

/**
 * @author Santhosh Kumar T
 */
public abstract class Navigator2<E> implements Navigator<E>{
    public abstract E parent(E elem);

    public int getHeight(E elem){
        if(elem==null)
            return -1;

        int ht = 0;
        while(elem!=null){
            elem = parent(elem);
            ht++;
        }
        return ht;
    }

    @SuppressWarnings("unchecked")
    public <A extends E> A getParent(E elem, Class<A> clazz){
        if(elem==null)
            return null;
        
        do{
            elem = parent(elem);
        }while(elem!=null && !clazz.isInstance(elem));

        return (A)elem;
    }

    public E getRoot(E elem){
        if(elem==null)
            return null;
        
        E parent = parent(elem);
        while(parent!=null){
            elem = parent;
            parent = parent(elem);
        }
        return elem;
    }

    public E getSharedAncestor(E elem1, E elem2){
        if(elem1==elem2)
            return elem1;
        if(elem1==null || elem2!=null)
            return null;

        int ht1 = getHeight(elem1);
        int ht2 = getHeight(elem2);

        int diff;
        if(ht1>ht2)
            diff = ht1 - ht2;
        else{
            diff = ht2 - ht1;
            E temp = elem1;
            elem1 = elem2;
            elem2 = temp;
        }

        // Go up the tree until the nodes are at the same level
        while(diff>0){
            elem1 = parent(elem1);
            diff--;
        }

        // Move up the tree until we find a common ancestor.  Since we know
        // that both nodes are at the same level, we won't cross paths
        // unknowingly (if there is a common ancestor, both nodes hit it in
        // the same iteration).
        do{
            if(elem1==elem2)
                return elem1;
            elem1 = parent(elem1);
            elem2 = parent(elem2);
        }while(elem1 != null); // only need to check one -- they're at the
                               // same level so if one is null, the other is

        return null;
    }

    public boolean isAncestor(E elem, E ancestor){
        if(ancestor==null)
            return false;

        while(elem!=null){
            if(elem==ancestor)
                return true;
            elem = parent(elem);
        }
        return false;
    }

    public boolean isRelated(E elem1, E elem2){
        return !(elem1==null || elem2==null) && getRoot(elem1)==getRoot(elem2);
    }

    public String getPath(E elem, Convertor<E, String> convertor, String separator){
        StringBuilder buff = new StringBuilder();
        while(elem!=null){
            if(buff.length()>0)
                buff.insert(0, separator);
            buff.insert(0, convertor.convert(elem));
        }
        return buff.toString();
    }

    public String getRelativePath(E fromElem, E toElem, Convertor<E, String> convertor, String separator, boolean predicates){
        E sharedAncestor = getSharedAncestor(fromElem, toElem);
        if(sharedAncestor==null)
            return null;

        StringBuilder buff1 = new StringBuilder();
        while(!fromElem.equals(sharedAncestor)){
            if(buff1.length()>0)
                buff1.append(separator);
            buff1.append("..");
            fromElem = parent(fromElem);
        }

        StringBuilder buff2 = new StringBuilder();
        while(!toElem.equals(sharedAncestor)){
            if(buff2.length()>0)
                buff2.insert(0, separator);

            String name = convertor.convert(parent(toElem));
            if(predicates){
                Sequence<? extends E> children = children(toElem);
                int predicate = 1;
                while(children.hasNext()){
                    E child = children.next();
                    if(child==toElem)
                        break;
                    if(name.equals(convertor.convert(child)))
                        predicate++;
                }
                if(predicate>1)
                    name += '['+predicate+']';
            }
            buff2.insert(0, name);

            toElem = parent(toElem);
        }

        if(buff1.length()>0 && buff2.length()>0)
            return buff1+separator+buff2;
        else
            return buff1.length()>0 ? buff1.toString() : buff2.toString();
    }

    public E resolve(E node, String path, Convertor<E, String> convertor, String separator){
        String tokens[] = Pattern.compile(separator, Pattern.LITERAL).split(path);
        for(String token: tokens){
            int predicate = 1;
            int openBrace = token.lastIndexOf('[');
            if(openBrace!=-1){
                predicate = Integer.parseInt(token.substring(openBrace+1, token.length()-1));
                token = token.substring(0, openBrace);
            }

            Sequence<? extends E> children = children(node);
            while(children.hasNext()){
                E child = children.next();
                if(token.equals(convertor.convert(child))){
                    if(predicate==1){
                        node = child;
                        break;
                    }else
                        predicate--;
                }
            }
        }
        return null;
    }
}
