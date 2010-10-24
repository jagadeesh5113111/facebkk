/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.xml.sax.async;

import jlibs.nbp.*;
import org.apache.xerces.impl.XMLEntityManager;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * @author Santhosh Kumar T
 */
public class XMLFeeder extends Feeder{
    AsyncXMLReader xmlReader;
    String publicID;
    String systemID;
    Runnable postAction;

    public XMLFeeder(AsyncXMLReader xmlReader, NBParser parser, InputSource source) throws IOException{
        super(parser);
        this.xmlReader = xmlReader;
        init(source);
    }

    private CharReader charReader;
    public XMLFeeder(AsyncXMLReader xmlReader, NBParser parser, CharReader charReader) throws IOException{
        super(parser);
        this.xmlReader = xmlReader;
        this.charReader = charReader;
        publicID = charReader.feeder.publicID;
        systemID = charReader.feeder.systemID;
    }

    final void init(InputSource is) throws IOException{
        publicID = systemID = null;
        postAction = null;
        eofSent = false;
        iProlog = 0;
        child = null;
        charBuffer.clear();

        publicID = is.getPublicId();
        systemID = XMLEntityManager.expandSystemId(is.getSystemId(), null, false);

        Reader charStream = is.getCharacterStream();
        if(charStream !=null){
            channel = new NBReaderChannel(charStream);
            iProlog = 7;
        }else{
            InputStream inputStream = is.getByteStream();
            String encoding = is.getEncoding();
            if(inputStream==null){
                assert systemID!=null;

                if(systemID.startsWith("file:/")){
                    try{
                        inputStream = new FileInputStream(new File(new URI(systemID)));
                    }catch(URISyntaxException ex){
                        throw new IOException(ex);
                    }
                }else{
                    URLConnection con = new URL(systemID).openConnection();
                    if(con instanceof HttpURLConnection){
                        final HttpURLConnection httpCon = (HttpURLConnection)con;

                        // set request properties
                        /*
                        Map<String, String> requestProperties = new HashMap<String, String>();
                        for(Map.Entry<String, String> entry: requestProperties.entrySet())
                            httpCon.setRequestProperty(entry.getKey(), entry.getValue());
                        */

                        // set preference for redirection
                        XMLEntityManager.setInstanceFollowRedirects(httpCon, true);
                    }
                    inputStream = con.getInputStream();

                    String contentType;
                    String charset = null;

                    // content type will be string like "text/xml; charset=UTF-8" or "text/xml"
                    String rawContentType = con.getContentType();
                    // text/xml and application/xml offer only one optional parameter
                    int index = (rawContentType != null) ? rawContentType.indexOf(';') : -1;

                    if(index!=-1){
                        // this should be something like "text/xml"
                        contentType = rawContentType.substring(0, index).trim();

                        // this should be something like "charset=UTF-8", but we want to
                        // strip it down to just "UTF-8"
                        charset = rawContentType.substring(index + 1).trim();
                        if(charset.startsWith("charset=")){
                            // 8 is the length of "charset="
                            charset = charset.substring(8).trim();
                            // strip quotes, if present
                            if((charset.charAt(0)=='"' && charset.charAt(charset.length()-1)=='"')
                                || (charset.charAt(0)=='\'' && charset.charAt(charset.length()-1)=='\'')){
                                charset = charset.substring(1, charset.length() - 1);
                            }
                        }
                    }else
                        contentType = rawContentType.trim();

                    String detectedEncoding = null;
                    /**  The encoding of such a resource is determined by:
                        1 external encoding information, if available, otherwise
                             -- the most common type of external information is the "charset" parameter of a MIME package
                        2 if the media type of the resource is text/xml, application/xml, or matches the conventions text/*+xml or application/*+xml as described in XML Media Types [IETF RFC 3023], the encoding is recognized as specified in XML 1.0, otherwise
                        3 the value of the encoding attribute if one exists, otherwise
                        4 UTF-8.
                     **/
                    if(contentType.equals("text/xml")){
                        if(charset!=null)
                            detectedEncoding = charset;
                        else
                            detectedEncoding = "US-ASCII"; // see RFC2376 or 3023, section 3.1
                    }else if(contentType.equals("application/xml")){
                        if(charset!=null)
                            detectedEncoding = charset;
                    }

                    if(detectedEncoding != null)
                        encoding = detectedEncoding;
                }
            }

            NBChannel channel = new NBChannel(new InputStreamChannel(inputStream));
            if(encoding==null)
                channel.setEncoding("UTF-8", true);
            else
                channel.setEncoding(encoding, false);

            this.channel = channel;
        }
    }

    // <  6  see if it has prolog
    // ==7   found declared encoding
    private int iProlog = 0;
    CharBuffer singleChar = CharBuffer.allocate(1);
    CharBuffer sixChars = CharBuffer.allocate(6);
    private static final int MAX_PROLOG_LENGTH = 70;

    @Override
    protected Feeder read() throws IOException{
        xmlReader.setFeeder(this);
        if(charReader!=null){
            parser.consume(new char[]{ ' '}, 0, 1);
            charReader.index = 0;
            if(child!=null)
                return child;

            char chars[] = charReader.chars;
            int index = charReader.index;
            int len = chars.length;
            charReader.index = parser.consume(chars, index, index+len);
            if(child!=null)
                return child;

            parser.consume(new char[]{ ' '}, 0, 1);
            charReader.index++;

            // EOF is not sent for CharReader
            return child!=null ? child : parent();
        }else{
            while(iProlog<6){
                sixChars.clear();
                int read = channel.read(sixChars);
                if(read==0)
                    return this;
                else if(read==-1){
                    charBuffer.append("<?xml ", 0, iProlog);
                    return onPrologEOF();
                }else{
                    char chars[] = sixChars.array();
                    for(int i=0; i<read; i++){
                        char ch = chars[i];
                        if(isPrologStart(ch)){
                            iProlog++;
                            if(iProlog==6){
                                charBuffer.append("<?xml ");
                                for(i=0; i<MAX_PROLOG_LENGTH; i++){
                                    singleChar.clear();
                                    read = channel.read(singleChar);
                                    if(read==1){
                                        ch = singleChar.get(0);
                                        charBuffer.append(ch);
                                        if(ch=='>')
                                            break;
                                    }else
                                        break;
                                }
                                if(charBuffer.position()>0)
                                    feedCharBuffer();
                                if(read==0)
                                    return this;
                                else if(read==-1)
                                    return onPrologEOF();
                                break;
                            }
                        }else{
                            charBuffer.append("<?xml ", 0, iProlog);
                            while(i<read)
                                charBuffer.append(chars[i++]);
                            iProlog = 7;
                            break;
                        }
                    }
                }
            }
            while(iProlog!=7){
                singleChar.clear();
                int read = channel.read(singleChar);
                if(read==0)
                    return this;
                else if(read==-1)
                    return onPrologEOF();
                else
                    parser.consume(singleChar.array(), 0, 1);
            }

            return super.read();
        }
    }

    private Feeder onPrologEOF() throws IOException{
        if(charBuffer.position()>0){
            if(feedCharBuffer())
                return child;
        }

        try{
            if(!eofSent){
                if(canClose()){
                    eofSent = true;
                    parser.eof();
                    if(child!=null)
                        return child;
                }
            }
            return parent();
        }finally{
            try{
                if(child==null){
                    if(canClose())
                        parser.reset();
                    channel.close();
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private boolean isPrologStart(char ch){
        switch(iProlog){
            case 0:
                return ch=='<';
            case 1:
                return ch=='?';
            case 2:
                return ch=='x';
            case 3:
                return ch=='m';
            case 4:
                return ch=='l';
            case 5:
                return ch==0x20 || ch==0x9 || ch==0xa || ch==0xd;
            default:
                throw new Error("impossible");
        }
    }

    void setDeclaredEncoding(String encoding){
        iProlog = 7;
        if(encoding!=null && channel instanceof NBChannel){
            NBChannel nbChannel = (NBChannel)channel;
            String detectedEncoding = nbChannel.decoder().charset().name().toUpperCase(Locale.ENGLISH);
            String declaredEncoding = encoding.toUpperCase(Locale.ENGLISH);
            if(!detectedEncoding.equals(declaredEncoding)){
                if(detectedEncoding.startsWith("UTF-16") && declaredEncoding.equals("UTF-16"))
                    return;
                if(!detectedEncoding.equals(encoding))
                    nbChannel.decoder(Charset.forName(encoding).newDecoder());
            }
        }
    }

    public InputSource resolve(String publicID, String systemID) throws IOException{
        InputSource inputSource = new InputSource(resolve(systemID));
        inputSource.setPublicId(publicID);
        return inputSource;
    }

    public String resolve(String systemID) throws IOException{
        return XMLEntityManager.expandSystemId(systemID, this.systemID, false);
    }
}

class CharReader{
    XMLFeeder feeder;
    char chars[];
    int index = -1;

    CharReader(XMLFeeder feeder, char[] chars){
        this.feeder = feeder;
        this.chars = chars;
    }
}