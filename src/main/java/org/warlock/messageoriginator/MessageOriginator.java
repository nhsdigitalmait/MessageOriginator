/*
 * MessageOriginator.java
 *
 * Created on 22 March 2006, 08:44
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// $Id: MessageOriginator.java 3 2015-12-10 14:08:03Z sfarrow $
// Rev 3 Mods for make SSL changes work (Tested via OpenTest)
// Rev 2 Mods for SSL Usage (Not tested) default to cleartext
package org.warlock.messageoriginator;

import org.warlock.messageoriginator.hl7.HL7Interaction;
import org.warlock.messageoriginator.transmission.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Properties;

// import java.io.IOException;
/**
 *
 * @author DAMU2
 */
public class MessageOriginator {

    private static final int SOURCE = 0;
    private static final int DESTINATION = 1;
    private static final int LOGFILE = 2;

    private static final int DEFAULTINTERBURST = 10;

    private static boolean verbose = false;
    private static boolean multi = false;

    private MessageSource source = null;
    private String logFile = null;
    private OutputManager output = null;

    /**
     * Creates a new instance of MessageOriginator
     *
     * @param sourceDir
     * @param wrappedDir
     * @param log
     */
    public MessageOriginator(String sourceDir, String wrappedDir, String log) {
        try {
            if (multi) {
                source = new MultiAttachmentMessageSource(sourceDir);
                output = new MultiAttachmentOutputManager(wrappedDir);
            } else {
                source = new MessageSource(sourceDir);
                output = new OutputManager(wrappedDir);
            }
            logFile = log;
        } catch (MessageOriginatorException e) {
            System.err.println("init: MessageOriginatorException: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("init: " + e.getClass().getName() + ": " + e.getMessage());
        }
        String directsend = System.getProperty("messageoriginator.directsend");
        if (directsend != null) {
            if (directsend.startsWith("Y") || directsend.startsWith("y")) {
                output.setDoNet(true);
            }
        }
    }

    /**
     *
     * @return the output manager object
     */
    public OutputManager getOutputManager() {
        return output;
    }

    /**
     *
     */
    public void exec() {
        Wrapper w = null;
        FileWriter log = null;
        HL7Interaction[] hl7 = null;
        int interBurst = 0;

        try {
            interBurst = Integer.parseInt(System.getProperty("messageoriginator.interburstperiod"));
        } catch (Exception e) {
            interBurst = DEFAULTINTERBURST;
        }
        try {
            log = new FileWriter(logFile);
        } catch (Exception e) {
            System.err.println("MessageOriginator.exec(): Cannot create logfile " + logFile + "\n" + e.getMessage());
            return;
        }

        while ((hl7 = source.getInteractions()) != null) {
            if (verbose) {
                System.out.println("Got batch (size: " + hl7.length + ")");
            }
            for (int i = 0; i < hl7.length; i++) {

                try {
                    hl7[i].load();
                } catch (MessageOriginatorException e) {
                    System.err.println("exec: MessageOriginatorException: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("exec: " + e.getClass().getName() + ": " + e.getMessage());
                }
                if (hl7[i].isOK()) {
                    if (verbose) {
                        System.out.println("Sending " + hl7[i].getMessageId());
                    }
                    try {
                        w = new Wrapper(hl7[i]);
                        w.makeTransmission();
                        output.output(w);
                    } catch (Exception e) {
                        try {
                            log.write("HL7Interaction failed to serialise: " + hl7[i].getFileName() + ", " + e.getMessage() + "\n");
                            if (verbose) {
                                System.err.println("HL7Interaction failed to serialise: " + hl7[i].getFileName() + ", " + e.getMessage());
                            }
                        } catch (Exception elog) {
                            System.err.println("Error writing to logfile " + logFile + "\n" + elog.getMessage());
                        }
                    }
                } else {
                    try {
                        log.write("HL7Interaction failed to load: " + hl7[i].getFileName() + ", " + hl7[i].getFileProblem() + "\n");
                        if (verbose) {
                            System.out.println("HL7Interaction failed to load: " + hl7[i].getFileName() + ", " + hl7[i].getFileProblem());
                        }
                    } catch (Exception e) {
                        System.err.println("Error writing to logfile " + logFile + "\n" + e.getMessage());
                    }
                }
            }
            try {
                if (verbose) {
                    System.err.println("Sleeping...");
                }
                Thread.sleep(interBurst * 1000);
            } catch (Exception e) {
            }
        }

        try {
            log.close();
        } catch (Exception e) {
            System.err.println("MessageOriginator.exec(): Error closing logfile " + logFile + "\n" + e.getMessage());
        }
    }

    /**
     * 
     * @param f string containing property filename
     */
    public static void loadProperties(String f) {
        try {
            FileInputStream input = new FileInputStream(f);
            Properties p = new Properties();
            p.load(input);
            input.close();
            Enumeration<Object> pkeys = p.keys();
            while (pkeys.hasMoreElements()) {
                String key = (String) pkeys.nextElement();
                String value = p.getProperty(key);
                System.setProperty(key, value);
            }
        } catch (Exception e) {
            System.err.println("Failed to read properties file " + f + ": " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * 
     * @param args 
     */
    static void processArgs(String[] args) {
        if ((args.length < 1) || (args.length > 3)) {
            System.err.println("Usage: java -jar messageoriginator.jar [-verbose] [-multi] propertiesfile");
            System.exit(1);
        }
        multi = false;
        int i = 0;
        for (; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-verbose")) {
                verbose = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("-multi")) {
                multi = true;
                continue;
            }
        }
        MessageOriginator.loadProperties(args[(args.length - 1)]);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        String proxy = null;
        processArgs(args);
        String source = System.getProperty("messageoriginator.source");
        String destination = System.getProperty("messageoriginator.destination");
        String logfile = System.getProperty("messageoriginator.logfile");
        proxy = System.getProperty("messageoriginator.proxyurl");
        MessageOriginator m = new MessageOriginator(source, destination, logfile);
        OutputManager o = m.getOutputManager();
        try {
            if ((proxy != null) && (proxy.trim().length() > 0)) {
                o.setProxy(proxy);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Output directory: " + o.getOutputDirectory());
        System.out.println("Proxy: " + o.getProxyName());
        m.exec();
    }

}
