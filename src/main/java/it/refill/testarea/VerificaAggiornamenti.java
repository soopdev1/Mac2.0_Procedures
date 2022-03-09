/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.testarea;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static it.refill.testarea.Mactest.agg;
import static it.refill.testarea.Mactest.host_PROD;
import static it.refill.testarea.Utility.patternnormdate;
import static it.refill.testarea.Utility.sendMail;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

/**
 *
 * @author rcosco
 */
public class VerificaAggiornamenti {

    public static final int limitAGG = 5000;
    public static final int limitMINUTES = 30;

    private static void ultimo_Aggiornamento() {
        Db db = new Db(host_PROD, false);
//        String sql_LE1 = "SELECT STR_TO_DATE(dt_start, '%d/%m/%Y %H:%i:%s'),dt_start,filiale FROM aggiornamenti_mod "
//                + "WHERE fg_stato='1' AND filiale='000' ORDER BY STR_TO_DATE(dt_start, '%d/%m/%Y %H:%i:%s') DESC LIMIT 1";

//        String sql_LE1 = "SELECT * FROM last_agg";
        String sql_LE1 = "SELECT dt_start,filiale FROM aggiornamenti_mod_verifica WHERE filiale = '000' ORDER BY date_start DESC LIMIT 1";

        try {
            ResultSet rs = db.getC().createStatement().executeQuery(sql_LE1);
            File txt = new File(RandomStringUtils.randomAlphanumeric(50) + ".txt");
            AtomicInteger content;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(txt))) {
                content = new AtomicInteger(0);
                DateTime dt_now = new DateTime();
                if (rs.next()) {
                    String last = rs.getString(1);

                    DateTime dt_last = Utility.formatStringtoStringDate(last, patternnormdate);
//                DateTime dt_last = Utility.formatStringtoStringDate(last, patternsqldate);
                    int m = Minutes.minutesBetween(dt_last, dt_now).getMinutes();
                    if (m >= limitMINUTES) {
                        content.addAndGet(1);
                        String print = "NON RISULTANO AGGIORNAMENTI IN CENTRALE DA PIU' DI " + limitMINUTES + " MINUTI PROVENIENTI DALLE FILIALI. CONTROLLARE.";
                        writer.write(print);
                        writer.newLine();
                    } else {
//                    System.out.println("ULTIMO AGGIORNAMENTO: " + rs.getString(3) + " - " + rs.getString(2) + " NOW: " + dt_now.toString(patternnormdate));
                        System.out.println("ULTIMO AGGIORNAMENTO: " + rs.getString(2) + " - " + rs.getString(1) + " NOW: " + dt_now.toString(patternnormdate));
                    }
                } else {
                    System.err.println("ERRORE-1");
                }   //            String sql_LE2 = "SELECT * FROM last_agg_f;";
                String sql_LE2 = "SELECT dt_start,filiale FROM aggiornamenti_mod_verifica WHERE filiale <> '000' ORDER BY date_start DESC LIMIT 1";
                System.out.println("mactest.VerificaAggiornamenti.ultimo_Aggiornamento() " + sql_LE2);
                //            String sql_LE2 = "SELECT STR_TO_DATE(dt_start, '%d/%m/%Y %H:%i:%s'),dt_start,filiale FROM aggiornamenti_mod "
//                    + "WHERE fg_stato='1' AND filiale<>'000' ORDER BY STR_TO_DATE(dt_start, '%d/%m/%Y %H:%i:%s') DESC LIMIT 1";
//
                ResultSet rs2 = db.getC().createStatement().executeQuery(sql_LE2);
                if (rs2.next()) {
                    String last = rs2.getString(1);
                    DateTime dt_last = Utility.formatStringtoStringDate(last, patternnormdate);
//                DateTime dt_last = Utility.formatStringtoStringDate(last, patternsqldate);
                    int m = Minutes.minutesBetween(dt_last, dt_now).getMinutes();
                    if (m >= limitMINUTES) {
                        content.addAndGet(1);
                        String print = "NON RISULTANO AGGIORNAMENTI IN CENTRALE DA PIU' DI " + limitMINUTES + " MINUTI DA INVIARE ALLE FILIALI. CONTROLLARE.";
                        writer.write(print);
                        writer.newLine();
                    } else {
                        System.out.println("ULTIMO AGGIORNAMENTO: " + rs2.getString(2) + " - " + rs2.getString(1) + " NOW: " + dt_now.toString(patternnormdate));
//                    System.out.println("ULTIMO AGGIORNAMENTO: " + rs2.getString(3) + " - " + rs2.getString(2) + " NOW: " + dt_now.toString(patternnormdate));
                    }
                } else {
                    System.err.println("ERRORE-2");
                }
            }
            if (content.get() > 0) {
                sendMail(txt);
            }
            txt.deleteOnExit();
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
        }
        db.closeDB();
    }

    private static void verifica_Aggiornamenti() {

        try {
            Db db = new Db(host_PROD, false);
            List<String> filialidanoncontrollare = Arrays.asList(db.getConf("filialinoncontrollare").split(","));
            db.closeDB();

//            filialinoncontrollare
//            List<String> filialidanoncontrollare = new ArrayList<>();
//
//            filialidanoncontrollare.add("125");
//            filialidanoncontrollare.add("130");
//            filialidanoncontrollare.add("137");
//            filialidanoncontrollare.add("139");
//            filialidanoncontrollare.add("149");
//            filialidanoncontrollare.add("154");
//            filialidanoncontrollare.add("155");
//            filialidanoncontrollare.add("156");
//            filialidanoncontrollare.add("157");
//            filialidanoncontrollare.add("160");
//            filialidanoncontrollare.add("170");
//            filialidanoncontrollare.add("171");
//            filialidanoncontrollare.add("180");
//            filialidanoncontrollare.add("182");
//            filialidanoncontrollare.add("184");
//            filialidanoncontrollare.add("185");
//            filialidanoncontrollare.add("187");
//            filialidanoncontrollare.add("189");
//            filialidanoncontrollare.add("540");
//            filialidanoncontrollare.add("169");
            AtomicInteger index = new AtomicInteger(0);
            List<StatusBranch> complete = agg(null);
            File txt = new File(RandomStringUtils.randomAlphanumeric(50) + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(txt))) {
                complete.forEach(agg -> {
                    if (agg.getAggfrom() >= limitAGG || agg.getAggto() >= limitAGG) {
                        if (!filialidanoncontrollare.contains(agg.getCod())) {
                            try {
                                String print = "FILIALE " + agg.getCod() + " - DA RICEVERE/CARICARE: " + agg.getAggfrom() + " ; DA INVIARE/ESEGUIRE: " + agg.getAggto() + " - RAGGIUNGIBILITA': " + agg.isRagg();
                                writer.write(print);
                                writer.newLine();
                                index.addAndGet(1);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
            }
            if (index.get() > 0) {
                sendMail(txt);
            }
            txt.deleteOnExit();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ultimo_Aggiornamento();
        verifica_Aggiornamenti();
    }

}
