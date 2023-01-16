/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.testarea;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import rc.soop.esolver.Branch;
import static rc.soop.esolver.Util.patternnormdate_filter;
import static rc.soop.esolver.Util.patternsql;
import rc.soop.rilasciofile.ControlloGestione;
import rc.soop.rilasciofile.DatabaseCons;
import rc.soop.rilasciofile.GeneraFile;
import static rc.soop.rilasciofile.Utility.patternmonthsql;
import rc.soop.start.Central_Branch;
import static rc.soop.start.Utility.createLog;
import static rc.soop.start.Utility.pattern4;
import static rc.soop.start.Utility.rb;

/**
 *
 * @author rcosco
 */
public class test {

    public static void main(String[] args) {

        try {

            String datecreation1 = new DateTime().toString("yyyyMMddHHmmss");
            String datecreation2 = new DateTime().withZone((DateTimeZone.forID("Europe/Rome"))).toString("yyyyMMddHHmmss");
            System.out.println("rc.soop.testarea.test.main() "+datecreation1);
            System.out.println("rc.soop.testarea.test.main() "+datecreation2);
            
//            GeneraFile gf = new GeneraFile();
//            gf.setIs_IT(true);
//            gf.setIs_UK(false);
//            gf.setIs_CZ(false);
//
//            DatabaseCons db = new DatabaseCons(gf);
//
//            DateTime iniziomese = new DateTime().minusDays(5).dayOfMonth().withMinimumValue().withHourOfDay(0)
//                    .withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
//
////        System.out.println("com.fl.upload.GeneraFile.rilasciafile() "+);
////            String mesemysql = iniziomese.toString(patternmonthsql);
//            DateTime ieri = new DateTime().minusDays(2);
//
////            String meseriferimento = iniziomese.monthOfYear().getAsText(Locale.ITALY).toUpperCase();
////            String annoriferimento = iniziomese.year().getAsText(Locale.ITALY).toUpperCase();
//
//            String data1 = iniziomese.toString(patternsql);
//            String data2 = ieri.toString(patternsql);
//
////            String d3 = iniziomese.toString(patternnormdate_filter);
////            String d4 = ieri.toString(patternnormdate_filter);
////            String meseanno_prec = iniziomese.minusMonths(1).toString(patternmonthsql);
////            String anno_rif = iniziomese.minusMonths(1).year().getAsText();
////            String meseriferimento_prec = iniziomese.minusMonths(1).monthOfYear().getAsText(Locale.ITALY).toUpperCase();
//            String path = db.getPath("temp");
//            ArrayList<String> br1 = db.list_branchcode_completeAFTER311217();
////            ArrayList<String> filiali_soloROMA = db.list_branch_RM();
//            ArrayList<Branch> allenabledbr = db.list_branch();
//
//            String nomereport = "MANAGEMENT CONTROL - REPORT CHANGE ACCOUNTING N1 DA " + data1 + " A " + data2 + ".xlsx";
//            File Output = new File(path + nomereport);
//            String base64 = ControlloGestione.management_change_accounting1(Output, br1, iniziomese, ieri, allenabledbr, db);
//
//            db.closeDB();
//            Logger log = createLog("Mac2.0_AGG_CENTRAL_" + "000", rb.getString("path.log"), pattern4);
////
//            Central_Branch cb = new Central_Branch("000");
//            cb.updateCentral(log);
            //System.out.println("rc.soop.testarea.test.main() "+Double.valueOf("1.72 "));
//            File txt = new File("C:\\Users\\rcosco\\Desktop\\content.txt");
//            
//            String bs4 = Files.toString(txt, Charset.defaultCharset());
//                
//            FileUtils.writeByteArrayToFile(new File("C:\\Users\\rcosco\\Desktop\\content.pdf"), Base64.decodeBase64(bs4)); 
//        try {
//            Db db = new Db(host_TEST_CZ, false);
//            ResultSet rs = db.getC().createStatement().executeQuery("SELECT gruppo_nc,causale_nc,fg_in_out,fg_tipo_transazione_nc,nc_de,de_causale_nc FROM maccorpcz.nc_causali WHERE filiale='000'");
//
////            AtomicInteger i0 = new AtomicInteger(0);
//            while (rs.next()) {
//                String ncde = "00";
//                if (rs.getString(4).equals("1") && rs.getString(3).equals("2")) {
//                    ncde = "01";
//                } else if (rs.getString(4).equals("1") && rs.getString(3).equals("1")) {
//                    ncde = "02";
//                } else if (rs.getString(4).equals("2") && rs.getString(3).equals("3")) {   //STOCK
//                    ncde = "09";
//                } else if (rs.getString(4).equals("2") && rs.getString(3).equals("2")) {   //STOCK
//                    ncde = "10";
//                } else if (rs.getString(4).equals("2") && rs.getString(3).equals("4")) {   //STOCK
//                    ncde = "14";
//                } else if (rs.getString(4).equals("4") && rs.getString(3).equals("1")) {   //NO STOCK
//                    ncde = "15";
//                } else if (rs.getString(4).equals("4") && rs.getString(3).equals("2")) {   //NO STOCK 
//                    ncde = "16";
//                } else {
//                    System.out.println(rs.getString(6) + " : " + rs.getString(4) + ";" + rs.getString(3) + " = " + rs.getString(5) + " ----------------------------");
//                }
//
////                System.out.println(rs.getString(1)+" (-) "+rs.getString(2)+" (-) "+rs.getString(3)+" (-) "+rs.getString(4));
////                String causale_nc = StringUtils.leftPad(String.valueOf(i0.addAndGet(1)), 6, "0");
//                String upd = "UPDATE nc_causali SET nc_de = '" + ncde + "' WHERE filiale = '000' AND gruppo_nc = '"
//                        + rs.getString(1) + "' AND causale_nc = '" + rs.getString(2) + "'";
//                boolean es = db.getC().createStatement().executeUpdate(upd) > 0;
//                System.out.println(upd + " --> " + es);
//            }
//
//            db.closeDB();
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//        }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

;

}
