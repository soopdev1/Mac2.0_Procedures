/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.maintenance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author rcosco
 */
public class Monitor {

    private static final ResourceBundle rb = ResourceBundle.getBundle("maintenance.conf");

    private static final String host = rb.getString("path.monitor.host");
    private static final int port = parseIntR(rb.getString("path.monitor.port"));
    private static final String usr = rb.getString("path.monitor.user");
    private static final String psw = rb.getString("path.monitor.psw");
//    private static final String dir = rb.getString("path.monitor.dir");
    private static final String config = rb.getString("path.monitor.config");
    private static final String patterndir = "yyyyMMdd";
    private static final String patternnormdate = "dd/MM/yyyy HH:mm:ss";
    private static final String pattern4 = "yyyyMMdd";
    private static final String pattern3 = "HHmmssSSS";

    public static final Logger log = createLog("Mac2.0_MONITOR", rb.getString("path.log"), pattern4);

    private static Logger createLog(String appname, String folderini, String patterndatefolder) {
        Logger LOGGER = Logger.getLogger(appname);
        try {
            DateTime dt = new DateTime();
            String filename = appname + "-" + dt.toString(pattern3) + ".log";
            File dirING = new File(folderini);
            dirING.mkdirs();
            if (patterndatefolder != null) {
                File dirINGNew = new File(dirING.getPath() + File.separator + dt.toString(patterndatefolder));
                dirINGNew.mkdirs();
                filename = dirINGNew.getPath() + File.separator + filename;
            } else {
                filename = dirING.getPath() + File.separator + filename;
            }
            Handler fileHandler = new FileHandler(filename);
            LOGGER.addHandler(fileHandler);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException localIOException) {
        }

        return LOGGER;
    }

    private static String formatValue(String value) {
//        double d1 = 0.000;
//        try {
//            d1 = Double.parseDouble(value);
//        } catch (Exception ex) {
//            log.severe(ex.getStackTrace()[0].getMethodName() + ": " + ex.getMessage());
//            d1 = 0.000;
//        }
//
////        DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.ITALY);
//        DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.UK);
//        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
//        symbols.setCurrencySymbol(""); // Don't use null.
//        formatter.setDecimalFormatSymbols(symbols);

//        System.out.println("macmonitor.Monitor.formatValue() " + formatMysqltoDisplay(value));
        return formatMysqltoDisplay(value);
    }

    private static String addStandard(String buy_std, String cambio_bce) {
        double d_rifbce = Double.parseDouble(cambio_bce);
        double d_standard = Double.parseDouble(buy_std);
        double tot_st = d_rifbce * (100 + d_standard) / 100;
        return roundDoubleandFormat(tot_st, 3);
    }

    private static String formatMysqltoDisplay(String ing) {

        String decimal = ".";
        String thousand = ",";

        if (ing == null) {
            return "";
        }
        if (ing.trim().equals("") || ing.trim().equals("-")) {
            return "";
        }
        if (ing.length() == 0) {
            return "";
        }
        if (ing.trim().startsWith(".") || ing.trim().startsWith(",")) {
            return "0" + decimal + "00";
        }

        String start = ing.substring(0, 1);
        if (start.equals("-") || start.equals("+")) {
            ing = ing.replaceAll(start, "");
        } else {
            start = "";
        }

        String out = "";
        if (ing.contains(",")) {
            ing = ing.replaceAll(",", "");
        }
        if (ing.contains(".")) {
            String[] inter1 = splitStringEvery(ing.split("\\.")[0], 3);
            if (inter1.length > 1) {
                for (int i = 0; i < inter1.length; i++) {
                    out = out + inter1[i] + thousand;
                }
            } else {
                out = inter1[0];
            }
            if (out.lastIndexOf(thousand) + 1 == out.length()) {
                out = out.substring(0, out.lastIndexOf(thousand));
            }
            String dec = ing.split("\\.")[1];
            out = out + decimal + dec;
        } else {

            String[] inter1 = splitStringEvery(ing, 3);
            if (inter1.length > 1) {
                for (int i = 0; i < inter1.length; i++) {
                    out = out + inter1[i] + thousand;
                }
            } else {
                out = inter1[0];
            }
            if (out.lastIndexOf(thousand) + 1 == out.length()) {
                out = out.substring(0, out.lastIndexOf(thousand));
            }
        }
        return start + out;
    }

    private static String[] splitStringEvery(String s, int interval) {
        int arrayLength = (int) Math.ceil(((s.length() / (double) interval)));
        String[] result = new String[arrayLength];
        int j = s.length();
        int lastIndex = result.length - 1;
        for (int i = lastIndex; i >= 0 && j >= interval; i--) {
            result[i] = s.substring(j - interval, j);
            j -= interval;
        } //Add the last bit
        if (result[0] == null) {
            result[0] = s.substring(0, j);
        }
        return result;
    }

    private static boolean generateFile(String path, String filiale, ArrayList<Branch> allbr, FTPClient ftpClient) {
        Db_Master dbm = new Db_Master();
        ArrayList<String[]> al = dbm.getValuteForMonitor(filiale);
        dbm.closeDB();

        if (!al.isEmpty()) {

            if (filiale.equals("---")) {
                boolean es1 = true;
                for (int i = 0; i < allbr.size(); i++) {
                    ArrayList<String[]> alinside = new ArrayList<>();
                    String fil = allbr.get(i).getCod();
                    for (int j = 0; j < al.size(); j++) {
                        if (fil.equals(al.get(j)[9])) {
                            alinside.add(al.get(j));
                        }
                    }
                    if (!alinside.isEmpty()) {
                        boolean es = printFile(path, fil, alinside, config, ftpClient);
                        if (!es) {
                            es1 = false;
                            break;
                        }
                    }
                }
                return es1;
            } else {
                return printFile(path, filiale, al, config, ftpClient);
            }

        }
        return false;
    }

    private static boolean printFile(String path, String filiale, ArrayList<String[]> al, String config, FTPClient ftpClient) {
        try {
            String directory_str = path + new DateTime().toString(patterndir);
            new File(directory_str).mkdirs();
            File xml_output = new File(directory_str + File.separator + filiale + ".xml");
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("valute");
            doc.appendChild(rootElement);
            DateTime d = new DateTime();
            String dataMillis = d.toString(patternnormdate);
            DateTimeFormatter df = DateTimeFormat.forPattern(patternnormdate);
            DateTime d2 = df.parseDateTime(dataMillis);
            Element timestamp = doc.createElement("timestamp");
            dataMillis = (d2.getMillis() + "").substring(0, 10);
            timestamp.appendChild(doc.createTextNode(dataMillis));
            rootElement.appendChild(timestamp);
            for (int i = 0; i < al.size(); i++) {
                if (!addStandard(al.get(i)[5], al.get(i)[2]).equals("0.0") && !addStandard(al.get(i)[8], al.get(i)[2]).equals("0.0")) {
                    Element valuta = doc.createElement("valuta");
                    rootElement.appendChild(valuta);
                    // firstname elements
                    Element code = doc.createElement("code");
                    code.appendChild(doc.createTextNode(al.get(i)[0].toUpperCase()));
                    valuta.appendChild(code);
                    // lastname elements
                    Element immagine = doc.createElement("immagine");
                    immagine.appendChild(doc.createCDATASection(StringUtils.replace(config, "###", al.get(i)[0].toLowerCase())));//SETTARE VALORE
                    valuta.appendChild(immagine);
                    // nickname elements
                    Element nome = doc.createElement("nome");
                    nome.appendChild(doc.createTextNode(al.get(i)[1]));
                    valuta.appendChild(nome);
                    // salary elements

                    Element buy = doc.createElement("buy");     // VALUTA
                    if (al.get(i)[10].equals("0")) {
                        buy.appendChild(doc.createTextNode(""));
                    } else {
                        if (al.get(i)[4].equals("0")) {
                            buy.appendChild(doc.createTextNode(formatValue(addStandard(al.get(i)[5], al.get(i)[2]))));
                        } else {
                            buy.appendChild(doc.createTextNode(al.get(i)[4]));
                        }
                    }
                    valuta.appendChild(buy);

                    // salary elements
                    Element sell = doc.createElement("sell");   // VALUTA
                    if (al.get(i)[11].equals("0")) {
                        sell.appendChild(doc.createTextNode(""));
                    } else {
                        if (al.get(i)[7].equals("0")) {
                            sell.appendChild(doc.createTextNode(formatValue(addStandard(al.get(i)[8], al.get(i)[2]))));
                        } else {
                            sell.appendChild(doc.createTextNode(al.get(i)[6]));
                        }
                    }
                    valuta.appendChild(sell);
                    // salary elements
                    Element alwaysVisible = doc.createElement("alwaysVisible");
                    alwaysVisible.appendChild(doc.createTextNode("true"));
                    valuta.appendChild(alwaysVisible);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xml_output);
            transformer.transform(source, result);

            boolean es = ftpUploadFile(ftpClient, xml_output);
            return es;
        } catch (ParserConfigurationException | TransformerException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return false;
    }

    private static String roundDoubleandFormat(double d, int scale) {
        return StringUtils.replace(String.format("%." + scale + "f", d), ",", ".");
    }

    private static int parseIntR(String value) {
        value = value.replaceAll("-", "").trim();
        if (value.contains(".")) {
            StringTokenizer st = new StringTokenizer(value, ".");
            value = st.nextToken();
        }
        int d1;
        try {
            d1 = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
            d1 = 0;
        }
        return d1;
    }

    private static FTPClient ftpConnect(String server, int port, String user, String pass) {
        try {
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(server, port);
            ftpClient.setDefaultTimeout(2000);
//            ftpClient.enterLocalPassiveMode();
            boolean logi = ftpClient.login(user, pass);
            if (logi) {
                if (ftpClient.isConnected()) {
                    log.warning("FTP CONNECTED.");
                    return ftpClient;
                } else {
                    log.severe("1 FTP NOT CONNECTED.");
                }
            } else {
                log.severe("2 FTP NOT CONNECTED.");
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return null;
    }

    private static boolean ftpChangeDir(FTPClient ftpClient, String dir) {
        if (ftpClient.isConnected()) {
            try {
                boolean es = ftpClient.changeWorkingDirectory(dir);
                return es;
            } catch (IOException ex) {
                log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
            }
        }
        return false;
    }

    private static boolean ftpUploadFile(FTPClient ftpClient, File fileup) {
        try {
            String firstRemoteFile = StringUtils.deleteWhitespace(fileup.getName());
            boolean done;
            try ( InputStream inputStream = new FileInputStream(fileup)) {
                done = ftpClient.storeFile(firstRemoteFile, inputStream);
            }
            if (done) {
                long originalsize = fileup.length();
                FTPFile[] filenames = ftpClient.listFiles(firstRemoteFile);
                for (int i = 0; i < filenames.length; i++) {
                    long destsize = filenames[i].getSize();
                    long perce = originalsize * 5 / 100;
                    long range = originalsize - perce;
                    if (destsize > range) {
                        log.log(Level.INFO, "{0} UPLOADED.", filenames[i].getName());
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        } catch (IOException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return false;
    }

    private static boolean ftpDisconnect(FTPClient ftpClient) {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            log.warning("FTP DISCONNECTED.");
            return true;
        } catch (IOException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return false;
    }

    public static void exe() {
        Db_Master dbm = new Db_Master();
        String path = dbm.getPath("temp");
        ArrayList<Branch> brl = dbm.list_branch_enabled();
        dbm.closeDB();
        FTPClient ftpClient = ftpConnect(host, port, usr, psw);
        if (ftpClient != null) {
//            ftpChangeDir(ftpClient, dir);
            generateFile(path, "---", brl, ftpClient);
            ftpDisconnect(ftpClient);
        }

    }

}
