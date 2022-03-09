/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.rilasciofile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import it.refill.qlik.LoggerNew;
import static it.refill.rilasciofile.Utility.getHASH;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class SftpRate {

    public double spread = 0.0D;
    public double costiservizio = 0.0D;
    public double reference = 0.0D;

    public String commission = "0.00";

    public LoggerNew logger;

    public String pathout;
    public String pathtemp;
    public String dtnow;

    public SftpRate() {
        this.dtnow = new DateTime().toString("yyyy-MM-dd_HHmmss");
        Db db = new Db(false);
        this.logger = new LoggerNew("SFTP_RATE", db.getPath("Pathlog"));
        this.spread = fd(db.getPath("atm.spread"));
        this.costiservizio = fd(db.getPath("atm.costiservizio"));
        this.reference = fd(db.getPath("atm.reference"));
        this.commission = db.getPath("atm.fix");
        this.pathout = db.getPath("upload_atm");
        this.pathtemp = db.getPath("temp");
        db.closeDB();
    }

    public static double fd(String si_t_old) {
        double d1;
        si_t_old = si_t_old.replace(",", "").trim();
        try {
            d1 = Double.parseDouble(si_t_old);
        } catch (Exception ex) {
            d1 = 0.0D;
        }
        return d1;
    }

    public static String roundDoubleandFormat(double d, int scale) {
        return StringUtils.replace(BigDecimal.valueOf(BigDecimal.valueOf(d).setScale(scale + 1,
                BigDecimal.ROUND_HALF_DOWN).doubleValue()).setScale(scale, BigDecimal.ROUND_HALF_DOWN).toPlainString(), ",", ".");
    }

    public static String formatMysqltoDisplay(String ing) {
        String decimal = ",";
        String thousand = ".";

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

    public static String[] splitStringEvery(String s, int interval) {
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

    public File newengine() {
        File output = null;
        List<Tassi> lista = new ArrayList<>();
        try {
            Db dbmac = new Db(true);
            String sql = "SELECT valuta,cambio_bce FROM valute WHERE filiale='000' order by valuta";
            ResultSet rs = dbmac.getConnectionDB().createStatement().executeQuery(sql);
            while (rs.next()) {
                String TASSOBCE = rs.getString(2);
                double TASSODICAMBIO = fd(TASSOBCE) * (100.0D + this.spread) / 100.0D;
//                double Costidiservizio = fd(TASSOBCE) * (this.costiservizio) / 100.0D;
//                double referenceontop = fd(TASSOBCE) * (this.reference) / 100.0D;

//                System.out.println("BCE " + fd(TASSOBCE));
//                System.out.println("Costidiservizio " + roundDoubleandFormat(Costidiservizio, 8));
//                System.out.println("reference " + roundDoubleandFormat(referenceontop, 8));
//                System.out.println("FINAL " + roundDoubleandFormat(TASSODICAMBIO, 8));
                Tassi t1 = new Tassi(rs.getString(1),
                        TASSOBCE,
                        roundDoubleandFormat(this.costiservizio, 2),
                        roundDoubleandFormat(this.reference, 2),
                        roundDoubleandFormat(this.spread, 2),
                        roundDoubleandFormat(TASSODICAMBIO, 8),
                        this.commission);
                lista.add(t1);
//                lista.add(new Tassi(
//                        rs.getString(1), 
//                        roundDoubleandFormat(tot_st, 8), 
//                        this.commission));
//                lista.add(new Tassi(
//                        rs.getString(1),
//                        rs.getString(2),
//                        
//                        roundDoubleandFormat(TASSODICAMBIO, 8), 
//                        this.commission));
            }
            dbmac.closeDB();
        } catch (SQLException e) {
            this.logger.log.log(Level.SEVERE, "ERRORE SFTP QUERY: {0}", e.getMessage());
        }

        if (!lista.isEmpty()) {

            try {

                //CSV
                output = new File(this.pathtemp + new DateTime().toString(dtnow) + "_Tassi.csv");
                try (PrintWriter writer = new PrintWriter(output)) {
                    writer.println("VALUTA;TASSO BCE;Costi di servizio;Reference rate on top;% EXCHANGE RATE;TASSO DI CAMBIO (BUY);COMMISSIONE (importo fisso)");
                    lista.forEach(tasso -> {
                        writer.println(
                                tasso.getCell1() + ";"
                                + tasso.getCell2() + ";"
                                + tasso.getCell3() + ";"
                                + tasso.getCell4() + ";"
                                + tasso.getCell5() + ";"
                                + tasso.getCell6() + ";"
                                + tasso.getCell7()
                        );
                    });
                    writer.flush();
                }

                //XLSX
//                output = new File(this.pathtemp + new DateTime().toString(dtnow) + "_Tassi.xlsx");
//                XSSFWorkbook myExcelBook = new XSSFWorkbook(new FileInputStream(new File("ATM.XLSX")));
//                XSSFSheet myExcelSheet = myExcelBook.getSheet("TASSI");
//
//                AtomicInteger indice = new AtomicInteger(1);
//                lista.forEach(tasso -> {
//                    XSSFRow row = myExcelSheet.getRow(indice.get());
//                    row.getCell(0).setCellValue(tasso.getCell1());
//                    row.getCell(1).setCellValue(tasso.getCell2());
//                    row.getCell(2).setCellValue(tasso.getCell3());
//                    row.getCell(3).setCellValue(tasso.getCell4());
//                    row.getCell(4).setCellValue(tasso.getCell5());
//                    row.getCell(5).setCellValue(tasso.getCell6());
//                    row.getCell(6).setCellValue(tasso.getCell7());
//                    indice.addAndGet(1);
//                });
//
//                myExcelBook.write(new FileOutputStream(output));
//                myExcelBook.close();
            } catch (IOException e) { // if any exception occurs it will catch
                output = null;
                this.logger.log.log(Level.SEVERE, "ERRORE SFTP ERROR: {0}", e.getMessage());
            }
        }
        return output;
    }

//    public File engine() {
//        File output = null;
//        List<Tassi> lista = new ArrayList<>();
//        try {
//            Db dbmac = new Db(true);
//            String sql = "SELECT valuta,cambio_bce FROM valute WHERE filiale='000' order by valuta";
//            ResultSet rs = dbmac.getConnectionDB().createStatement().executeQuery(sql);
//            while (rs.next()) {
//                double tot_st = fd(rs.getString(2)) * (100.0D + this.spread) / 100.0D;
//                lista.add(new Tassi(rs.getString(1), roundDoubleandFormat(tot_st, 8), this.commission));
//            }
//            dbmac.closeDB();
//        } catch (SQLException e) {
//            this.logger.log.log(Level.SEVERE, "ERRORE SFTP QUERY: {0}", e.getMessage());
//        }
//        if (!lista.isEmpty())
//      try {
//            output = new File(this.pathtemp + (new DateTime()).toString(this.dtnow) + "_Tassi.xlsx");
//            try (XSSFWorkbook myExcelBook = new XSSFWorkbook(new FileInputStream(new File("ATM.XLSX")))) {
//                XSSFSheet myExcelSheet = myExcelBook.getSheet("TASSI");
//                AtomicInteger indice = new AtomicInteger(1);
//                lista.forEach(tasso -> {
//                    XSSFRow row = myExcelSheet.getRow(indice.get());
//                    row.getCell(0).setCellValue(tasso.getCell1());
//                    row.getCell(1).setCellValue(tasso.getCell2());
//                    row.getCell(2).setCellValue(tasso.getCell3());
//                    indice.addAndGet(1);
//                });
//                myExcelBook.write(new FileOutputStream(output));
//            }
//        } catch (IOException e) {
//            output = null;
//            this.logger.log.log(Level.SEVERE, "ERRORE SFTP ERROR: {0}", e.getMessage());
//        }
//        return output;
//    }

    public static String getValueDiff_R(String value1, String value2, String diff, String rate, boolean dividi) {
        double diff1 = fd(value1) - fd(value2);
        double out;
        if (dividi) {
            out = diff1 / fd(rate);
        } else {
            out = diff1 * fd(rate);
        }
        return roundDoubleandFormat(out, 2);
    }

    public static void main(String[] args) {
        SftpRate sf = new SftpRate();
        File output_csv = sf.newengine();
        
//RILASCIO TEST
        try {
            if (output_csv != null) {
                ChannelSftp sftpnexi_preprod = SftpConnection.connect(
                        "setacom", //USER
                        "stgtransfer.nexi.it", //   HOST
                        115, //PORTA
                        "private_key_PREPROD.ppk",
                        sf.logger);
                if (sftpnexi_preprod != null && sftpnexi_preprod.isConnected()) {
                    try {
                        sftpnexi_preprod.cd(sf.pathout);
                        sftpnexi_preprod.put(new FileInputStream(output_csv), output_csv.getName());
                        sf.logger.log.log(Level.INFO, "{3}: FILE CARICATO PREPROD: {0} - SIZE: {1} - HASH: {2}",
                                new Object[]{output_csv.getName(), output_csv.length(), getHASH(output_csv), "TAS"});
                    } catch (SftpException e) {
                        sf.logger.log.log(Level.SEVERE, "ERRORE SFTP UPLOAD: {0}", e.getMessage());
                    }
                    SftpConnection.closeConnection(sftpnexi_preprod, sf.logger);
                }
            }
        } catch (Exception e) {
        }

        //RILASCIO PROD OLD
//        try {
//            File output_xslx = sf.engine();
//            if (output_xslx != null) {
//
//                ChannelSftp sftpnexi_prod = SftpConnection.connect(
//                        "setacom", //USER
//                        "transfer.nexi.it", //HOST
//                        115, //PORTA
//                        "private_key_PROD.ppk",
//                        sf.logger);
//                if (sftpnexi_prod != null && sftpnexi_prod.isConnected()) {
//                    try {
//                        sftpnexi_prod.cd(sf.pathout);
//                        sftpnexi_prod.put(new FileInputStream(output_xslx), output_xslx.getName());
//                        sf.logger.log.log(Level.INFO, "{3}: FILE CARICATO PROD: {0} - SIZE: {1} - HASH: {2}",
//                                new Object[]{output_xslx.getName(), output_xslx.length(), getHASH(output_xslx), "TAS"});
//                    } catch (Exception e) {
//                        sf.logger.log.log(Level.SEVERE, "ERRORE SFTP UPLOAD: {0}", e.getMessage());
//                    }
//                    SftpConnection.closeConnection(sftpnexi_prod, sf.logger);
//                }
//            }
//        } catch (Exception e) {
//        }
//RILASCIO PROD NEW
        try {
            if (output_csv != null) {
                ChannelSftp sftpnexi_prod = SftpConnection.connect(
                        "setacom", //USER
                        "transfer.nexi.it", //HOST
                        115, //PORTA
                        "private_key_PROD.ppk",
                        sf.logger);
                if (sftpnexi_prod != null && sftpnexi_prod.isConnected()) {
                    try {
                        sftpnexi_prod.cd(sf.pathout);
                        sftpnexi_prod.put(new FileInputStream(output_csv), output_csv.getName());
                        sf.logger.log.log(Level.INFO, "{3}: FILE CARICATO PROD: {0} - SIZE: {1} - HASH: {2}",
                                new Object[]{output_csv.getName(), output_csv.length(), getHASH(output_csv), "TAS"});
                    } catch (Exception e) {
                        sf.logger.log.log(Level.SEVERE, "ERRORE SFTP UPLOAD: {0}", e.getMessage());
                    }
                    SftpConnection.closeConnection(sftpnexi_prod, sf.logger);
                }
            }
        } catch (Exception e) {
        }
    }

}

class Tassi {

    String cell1, cell2, cell3, cell4, cell5, cell6, cell7;

    public Tassi(String cell1, String cell2, String cell3) {
        this.cell1 = cell1;
        this.cell2 = cell2;
        this.cell3 = cell3;
    }

    public Tassi(String cell1, String cell2, String cell3, String cell4, String cell5, String cell6, String cell7) {
        this.cell1 = cell1;
        this.cell2 = cell2;
        this.cell3 = cell3;
        this.cell4 = cell4;
        this.cell5 = cell5;
        this.cell6 = cell6;
        this.cell7 = cell7;
    }

    public String getCell1() {
        return cell1;
    }

    public void setCell1(String cell1) {
        this.cell1 = cell1;
    }

    public String getCell2() {
        return cell2;
    }

    public void setCell2(String cell2) {
        this.cell2 = cell2;
    }

    public String getCell3() {
        return cell3;
    }

    public void setCell3(String cell3) {
        this.cell3 = cell3;
    }

    public String getCell4() {
        return cell4;
    }

    public void setCell4(String cell4) {
        this.cell4 = cell4;
    }

    public String getCell5() {
        return cell5;
    }

    public void setCell5(String cell5) {
        this.cell5 = cell5;
    }

    public String getCell6() {
        return cell6;
    }

    public void setCell6(String cell6) {
        this.cell6 = cell6;
    }

    public String getCell7() {
        return cell7;
    }

    public void setCell7(String cell7) {
        this.cell7 = cell7;
    }

}
