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

package jlibs.examples.xml.sax.dog.tests;

import jlibs.core.io.FileNavigator;
import jlibs.core.io.FileUtil;
import jlibs.core.lang.JavaProcessBuilder;
import jlibs.core.util.logging.AnsiFormatter;
import jlibs.examples.xml.sax.dog.TestCase;
import jlibs.examples.xml.sax.dog.TestSuite;

import java.io.*;

/**
 * @author Santhosh Kumar T
 */
public class XPathPerformanceTest{
    private static final int runCount = 20;
    private TestSuite testSuite;

    public XPathPerformanceTest(TestSuite testSuite){
        this.testSuite = testSuite;
    }

    public void run(boolean cumulative, boolean xmlDog) throws Exception{
        long stats[] = new long[testSuite.testCases.size()];

        if(cumulative){
            for(int i=0; i<testSuite.testCases.size(); i++){
                TestCase testCase = testSuite.testCases.get(i);

                for(int count=0; count<runCount; count++){
                    long time = System.nanoTime();
                    if(xmlDog)
                        testCase.usingXMLDog();
                    else
                        testCase.usingDOM();
                    time = System.nanoTime() - time;
                    stats[i] += time;
                    if(xmlDog)
                        testCase.dogResult = null;
                    else
                        testCase.jdkResult = null;
                }
            }
        }else{
            for(int count=0; count<runCount; count++){
                for(int i=0; i<testSuite.testCases.size(); i++){
                    TestCase testCase = testSuite.testCases.get(i);
                    long time = System.nanoTime();
                    if(xmlDog)
                        testCase.usingXMLDog();
                    else
                        testCase.usingDOM();
                    time = System.nanoTime() - time;
                    stats[i] += time;
                    if(xmlDog)
                        testCase.dogResult = null;
                    else
                        testCase.jdkResult = null;
                }
            }
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(FileUtil.TMP_DIR+FileUtil.SEPARATOR+xmlDog+".txt"));
        for(long stat: stats){
            writer.write(String.valueOf(stat/runCount));
            writer.newLine();
        }
        writer.close();
    }

    private static void printLine(int maxlen){
        maxlen += 41;
        for(int i=0; i<maxlen; i++)
            System.out.print("-");
        System.out.println();
    }

    private static void printStat(int maxlen, String file, int xpaths, long dogTime, long domTime){
        long diff = dogTime - domTime;
        double faster = (1.0*Math.max(dogTime, domTime)/Math.min(dogTime, domTime)) * (dogTime<=domTime ? -1 : +1);
        (diff>=0?AnsiFormatter.SEVERE:AnsiFormatter.INFO).outFormat("%"+maxlen+"s | %6d %6d %6d %6d   %+2.2f%n", file, xpaths, (long)(dogTime*1E-06), (long)(domTime*1E-06), (long)(diff*1E-06), faster);
    }

    public static void main(String[] args) throws Exception{
        if(args.length==0)
            args = new String[]{ TestSuite.DEFAULT_TEST_SUITE };

        if(args.length==1){
            JavaProcessBuilder jvm = new JavaProcessBuilder()
                    .jvmArg("-server")
                    .jvmArg("-classpath")
                    .jvmArg(System.getProperty("java.class.path"))
                    .mainClass(XPathPerformanceTest.class.getName())
                    .arg(args[0]);

            jvm.arg("true");
            System.out.format("%6s............", "XMLDog");
            if(jvm.launch(System.out, System.err).waitFor()!=0)
                return;
            System.out.println(" Done");

            System.out.format("%6s............", TestCase.domEngine.getName());
            jvm.args().set(jvm.args().size()-1, "false");
            if(jvm.launch(System.out, System.err).waitFor()!=0)
                return;
            System.out.println(" Done");

            TestSuite testSuite = new TestSuite(args[0]);
            BufferedReader dogReader = new BufferedReader(new FileReader(FileUtil.TMP_DIR+FileUtil.SEPARATOR+"true.txt"));
            BufferedReader domReader = new BufferedReader(new FileReader(FileUtil.TMP_DIR+FileUtil.SEPARATOR+"false.txt"));

            File configFile = new File(args[0]);
            int maxlen = 0;
            for(TestCase testCase: testSuite.testCases){
                testCase.file = FileNavigator.INSTANCE.getRelativePath(configFile.getParentFile(), new File(testCase.file));
                maxlen = Math.max(maxlen, testCase.file.length());
            }

            System.out.format("%nAverage Execution Time over %d runs:%n", runCount);
            printLine(maxlen);
            System.out.format("%"+maxlen+"s | %6s %6s %6s %6s %6s%n", "File", "XPaths", "XMLDog", TestCase.domEngine.getName(), "Diff", "Percentage");
            printLine(maxlen);

            long dogTotal = 0;
            long domTotal = 0;

            for(TestCase testCase: testSuite.testCases){
                long dogTime = Long.parseLong(dogReader.readLine());
                long domTime = Long.parseLong(domReader.readLine());
                printStat(maxlen, testCase.file, testCase.xpaths.size(), dogTime, domTime);
                dogTotal += dogTime;
                domTotal += domTime;
            }
            printLine(maxlen);
            printStat(maxlen, "Total", testSuite.total, dogTotal, domTotal);

            dogReader.close();
            domReader.close();
        }else if(args.length==2){
            TestSuite testSuite = new TestSuite(args[0]);
            new XPathPerformanceTest(testSuite).run(true, args[1].equals("true"));
        }
    }
}