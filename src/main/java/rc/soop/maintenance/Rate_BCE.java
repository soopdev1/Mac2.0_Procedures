/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.maintenance;

import it.refill.testarea.Db;
import it.refill.testarea.Mactest;
import java.util.ArrayList;
import static it.refill.testarea.ExportDataCLiente.fd;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author rcosco
 */
public class Rate_BCE {

    private static final String pattern1 = "yyyy-MM-dd";
    private static final String startday = "2020-09-01";
//  private static final String value = "2019-01-01";

    public static void today(String[] args) {
        DateTime today = new DateTime();

        Db db = new Db(Mactest.host_PROD, false);
        ArrayList<String> listCurrency = db.listCurrency("EUR");
        String giorno = today.toString(pattern1);
        listCurrency.forEach(valuta1 -> {
            String value = db.get_BCE(giorno, valuta1);
            if (fd(value) > 0) {
                Rate r1 = new Rate(giorno, valuta1, "000", value);
//                    boolean es = db.insert_RATE(r1);
                System.out.println(": " + r1.toString());
//                    System.out.println(es + ": " + r1.toString());
            }
        });

        db.closeDB();
    }

    public static void engine() {
        DateTime dt = new DateTime().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern1);
        DateTime start = formatter.parseDateTime(startday);
        Db db = new Db(Mactest.host_PROD, false);
        ArrayList<String> listCurrency = db.listCurrency("EUR");
        while (start.isBefore(dt)) {
            String giorno = start.toString(pattern1);
            start = start.plusDays(1);
            listCurrency.forEach(valuta1 -> {
                String value = db.get_BCE(giorno, valuta1);
                if (fd(value) > 0) {
                    Rate r1 = new Rate(giorno, valuta1, "000", value);
                    boolean es = db.insert_RATE(r1);
                    System.out.println(es + ": " + r1.toString());
                }
            });
        }
        db.closeDB();
    }

//    public static void main(String[] args) {
//        DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern1);
//        DateTime start = formatter.parseDateTime(startday);
//        DateTime dt = new DateTime().minusDays(1);
//        Db db = new Db(Mactest.host_PROD, false);
//        ArrayList<String> br = db.list_cod_branch_ALL();
//        while (start.isBefore(dt)) {
//            String giorno = start.toString(pattern1);
//            start = start.plusDays(1);
//            String usd = db.get_BCE_USD(giorno);
//            String gbp = db.get_BCE_GBP(giorno);
//            String czk = db.get_BCE_CZK(giorno);
//            for (int i = 0; i < br.size(); i++) {
////                Rate r1 = new Rate(giorno, "USD", br.get(i), usd);
//                Rate r2 = new Rate(giorno, "GBP", br.get(i), gbp);
//                Rate r3 = new Rate(giorno, "CZK", br.get(i), czk);
////                db.insert_RATE(r1);
//                db.insert_RATE(r2);
//                db.insert_RATE(r3);
////                System.out.println(r1.toString());
////                System.out.println(r2.toString());
////                System.out.println(r3.toString());
//            }
//        }
//        db.closeDB();
//    }
}
