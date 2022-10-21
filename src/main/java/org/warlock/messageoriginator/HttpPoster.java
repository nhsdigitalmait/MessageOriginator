/*
 * HttpPoster.java
 *
 * Created on 22 March 2006, 14:11
 *
 */
package org.warlock.messageoriginator;

import java.net.URL;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.warlock.messageoriginator.transmission.Wrapper;

/**
 *
 * @author DAMU2
 */
public class HttpPoster
        extends Thread {

    private String message = null;
    private String resultFilename = null;
    private Wrapper wrapper = null;
    private URL url = null;
    private OutputManager output = null;
    private Exception lastException = null;

    private static final int DEFAULTMAXBACKOFF = 2000;
    private static final int DEFAULTMAXRETRIES = 3;
    private static final int FILEBUFFERSIZE = 50200;

    private static final int MAXLINE = 1024000;
    private boolean eofReadingHeader = false;

    /**
     * Creates a new instance of HttpPoster
     *
     * @param u
     * @param msg
     * @param o
     * @param w
     * @param fName
     */
    public HttpPoster(URL u, String msg, OutputManager o, Wrapper w, String fName) {
        message = msg;
        url = u;
        output = o;
        wrapper = w;
        resultFilename = fName;
        this.start();
    }

    @Override
    public void run() {
        boolean cleartext = System.getProperty("messageoriginator.cleartext", "Y").toUpperCase().equals("Y");

        BufferedWriter bw = null;
        Socket s = null;
        boolean done = false;
        //     for (retryCount = 0; retryCount < maxRetries; retryCount++) {
        FileOutputStream fos = null;
        //FileWriter fOut = null;
        OutputStreamWriter fOut = null;
        try {
            InetAddress pAddr = InetAddress.getByName(url.getHost());
            InetSocketAddress iAddr = new InetSocketAddress(pAddr, url.getPort());
            if (cleartext) {
                s = new Socket();
                s.connect(iAddr, 0);
            } else {
                SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket ssl = (SSLSocket)ssf.createSocket(pAddr, url.getPort());
                ssl.setNeedClientAuth(true);
                s = ssl;
            }
            s.setTcpNoDelay(true);
            s.setKeepAlive(false);
            s.setSoLinger(false, 0);
            //s.setSoTimeout(20000);
            bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            //BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            if (resultFilename != null) {
                fos = new FileOutputStream(resultFilename);
//                    fOut = new FileWriter(resultFilename);
                fOut = new OutputStreamWriter(fos);
            }
            if (message != null) {
                bw.write(message);
            } else {
                sendFromFile(bw);
            }
            bw.flush();
        } catch (Exception e) {
            System.err.println("Exception " + e.getMessage() + " caught transmitting");
            lastException = e;
        }
        try {
            String line = null;
            int responseContentLength = -1;
            while ((line = getLine(s.getInputStream())).trim().length() != 0) {
                if (fOut != null) {
                    fOut.write(line);
                    fOut.flush();
                }
                int colon = line.indexOf(":");
                if (colon != -1) {
                    String httpField = line.substring(0, colon);
                    if (httpField.equalsIgnoreCase("content-length")) {
                        String clen = line.substring(colon + 1).trim();
                        responseContentLength = Integer.parseInt(clen);
                    }
                }
            }
            if (eofReadingHeader) {
                System.err.println("EOF reading header for .in file " + resultFilename);
            }
            byte b[] = null;
            int bytesReceived = 0;
            if (responseContentLength > 0) {
                b = new byte[responseContentLength];
                int r = 0;
                InputStream is = s.getInputStream();
                while (bytesReceived < responseContentLength) {
                    r = is.read(b, bytesReceived, responseContentLength - bytesReceived);
                    if (r == -1) {
                        System.out.println("Unexpected EOF reading synchronous response: " + bytesReceived + " read, and " + responseContentLength + " expected.");
                        break;
                    }
                    bytesReceived += r;
//                        if (fOut != null) {
//                            if (b != null) {
//                                for (int i = 0; i < r; i++) {
//                                    fOut.write((int)b[i]);
//                                }
//                                fOut.flush();
//                            }
//                        }

                }
            }
            s.close();
            if (b != null) {
                if (fos != null) {
                    fos.write(b);
                    fos.flush();
                }
            }
            done = true;
            if (fos != null) {
                fos.close();
            }
//                if (fOut != null) { 
//                    if (b != null) {
//                        for (int i = 0; i < b.length; i++) {
//                            fOut.write((int)b[i]);
//                        }
//                    }
//                    fOut.flush();
//                    fOut.close();
//                }                
            //      break;
        } catch (Exception e) {
            System.err.println("Exception " + e.getMessage() + " caught receiving");
            lastException = e;
        }
        //}
        if (!done) {
            output.sendLogger(lastException);
        }
    }

    private String getLine(InputStream in)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        int r = 0;
        int c = 0;
        while ((r = in.read()) != (int) '\r') {
            if (r == -1) {
                eofReadingHeader = true;
                return sb.toString();
            }
            if (r == 0) {
                throw new Exception("NULL character read from HTTP response header");
            }
            sb.append((char) r);
            if (++c == MAXLINE) {
                throw new Exception("HTTP response header line too long: " + c);
            }
        }
        r = in.read();
        return sb.toString();
    }

    private void sendFromFile(BufferedWriter bw)
            throws Exception {
        String fileName = wrapper.getTransmission().getSaveFile();
        String outputDirectory = System.getProperty("messageoriginator.destination");
        File f = new File(outputDirectory, fileName);
        FileReader fr = new FileReader(f);
        int i = 0;
        char buf[] = new char[FILEBUFFERSIZE];
        while ((i = fr.read(buf)) != -1) {
            bw.write(buf, 0, i);
        }
        fr.close();
    }
}
