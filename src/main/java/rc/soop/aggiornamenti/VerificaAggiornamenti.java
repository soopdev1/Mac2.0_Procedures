/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.aggiornamenti;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static rc.soop.aggiornamenti.Mactest.agg;
import static rc.soop.aggiornamenti.Mactest.host_PROD;
import static rc.soop.aggiornamenti.Utility.patternnormdate;
import static rc.soop.aggiornamenti.Utility.sendMail;
import org.apache.commons.lang3.RandomStringUtils;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import static rc.soop.aggiornamenti.Utility.formatStringtoStringDate;
import static rc.soop.aggiornamenti.Utility.patternsqldate;

/**
 *
 * @author rcosco
 */
public class VerificaAggiornamenti {

    public static final int limitAGG = 5000;
    public static final int limitMINUTES = 30;

    public static void verificaSpreadKO() {
        List<String> errorspread = new ArrayList<>();

        Db db = new Db(host_PROD, false);
        try {

            String sql1 = "SELECT c.cod,c.filiale,c.id,c.tipotr,c.data,v.valuta FROM ch_transaction c, ch_transaction_valori v "
                    + " WHERE c.cod=v.cod_tr AND v.spread LIKE '%KO%' AND c.data > '2024-01-01 00:00:00';";
            try (ResultSet rs = db.getC().createStatement().executeQuery(sql1)) {
                while (rs.next()) {
                    String tipotr = rs.getString(4).equals("B") ? "BUY" : "SELL";
                    String add = "FILIALE: " + rs.getString(2) + " - ID: " + rs.getString(3)
                            + " - TIPO: " + tipotr + " - DATA: "
                            + formatStringtoStringDate(rs.getString(5), patternsqldate, patternnormdate)
                            + " - VALUTA: " + rs.getString(6);
                    errorspread.add(add);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String pathtemp = db.getPath("temp");
        db.closeDB();
        File txt;
        String text;
        if (errorspread.isEmpty()) {
            text = "NESSUNA ANOMALIA CALCOLO SPREAD.";
            sendMail("VERIFICA ANOMALIE CALCOLO SPREAD", text, null);
        } else {
            text = "IN ALLEGATO I DETTAGLI DI UNA O PIU' TRANSAZIONI CON UN'ANOMALIA CALCOLO SPREAD.";
            txt = new File(pathtemp + RandomStringUtils.randomAlphanumeric(50) + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(txt))) {

                for (String s1 : errorspread) {
                    writer.write(s1);
                    writer.newLine();
                }
                writer.flush();
                sendMail("VERIFICA ANOMALIE CALCOLO SPREAD", text, txt);
                txt.deleteOnExit();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void ultimo_Aggiornamento22() {
        Db db = new Db(host_PROD, false);
        try {
            DateTime dt_now = new DateTime();

            File txt = new File(db.getPath("temp")
                    + RandomStringUtils.randomAlphanumeric(50) + ".txt");

            AtomicInteger content;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(txt))) {
                content = new AtomicInteger(0);
                String sql1 = "SELECT dt_start,filiale,SUBDATE(NOW(), INTERVAL 30 MINUTE) "
                        + "FROM aggiornamenti_mod_verifica WHERE filiale = '000' "
                        + "AND date_start > SUBDATE(NOW(), INTERVAL " + limitMINUTES + " MINUTE) LIMIT 1";

                try (ResultSet rs = db.getC().createStatement().executeQuery(sql1)) {
                    if (rs.next()) {
                        System.out.println("ULTIMO AGGIORNAMENTO: " + rs.getString(2) + " - " + rs.getString(1) + " NOW: " + dt_now.toString(patternnormdate));
                    } else {
                        content.addAndGet(1);
                        String print = "NON RISULTANO AGGIORNAMENTI IN CENTRALE DA PIU' DI " + limitMINUTES + " MINUTI PROVENIENTI DALLE FILIALI. CONTROLLARE.";
                        writer.write(print);
                        writer.newLine();
                    }
                }
                String sql2 = "SELECT dt_start,filiale,SUBDATE(NOW(), INTERVAL 30 MINUTE) "
                        + "FROM aggiornamenti_mod_verifica WHERE filiale <> '000' "
                        + "AND date_start > SUBDATE(NOW(), INTERVAL " + limitMINUTES + " MINUTE) LIMIT 1";

                try (ResultSet rs2 = db.getC().createStatement().executeQuery(sql2)) {
                    if (rs2.next()) {
                        System.out.println("ULTIMO AGGIORNAMENTO: " + rs2.getString(2) + " - " + rs2.getString(1) + " NOW: " + dt_now.toString(patternnormdate));
                    } else {
                        content.addAndGet(1);
                        String print = "NON RISULTANO AGGIORNAMENTI IN CENTRALE DA PIU' DI " + limitMINUTES + " MINUTI DA INVIARE ALLE FILIALI. CONTROLLARE.";
                        writer.write(print);
                        writer.newLine();
                    }
                }
            }
            if (content.get() > 0) {
                sendMail(txt);
            }
            txt.deleteOnExit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        db.closeDB();
    }

    private static void ultimo_Aggiornamento() {
        Db db = new Db(host_PROD, false);
        String sql_LE1 = "SELECT dt_start,filiale FROM aggiornamenti_mod_verifica WHERE filiale = '000' ORDER BY date_start DESC LIMIT 1";

        try {
            ResultSet rs = db.getC().createStatement().executeQuery(sql_LE1);
            File txt = new File(db.getPath("temp")
                    + RandomStringUtils.randomAlphanumeric(50) + ".txt");
            AtomicInteger content;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(txt))) {
                content = new AtomicInteger(0);
                DateTime dt_now = new DateTime();
                if (rs.next()) {
                    String last = rs.getString(1);
                    DateTime dt_last = Utility.formatStringtoStringDate(last, patternnormdate);
                    int m = Minutes.minutesBetween(dt_last, dt_now).getMinutes();
                    if (m >= limitMINUTES) {
                        content.addAndGet(1);
                        String print = "NON RISULTANO AGGIORNAMENTI IN CENTRALE DA PIU' DI " + limitMINUTES + " MINUTI PROVENIENTI DALLE FILIALI. CONTROLLARE.";
                        writer.write(print);
                        writer.newLine();
                    } else {
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        db.closeDB();
    }

    public static void verifica_Aggiornamenti22() {
        try {
            Db db = new Db(host_PROD, false);
            List<String> filialidanoncontrollare = Arrays.asList(db.getConf("filialinoncontrollare").split(","));
            String el1 = StringUtils.replace(filialidanoncontrollare.toString(), "[", "(");
            el1 = StringUtils.replace(el1, "]", ")");

            File txt = new File(db.getPath("temp")
                    + randomAlphanumeric(50) + ".txt");
            AtomicInteger index = new AtomicInteger(0);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(txt))) {
                String sql1 = "SELECT b.cod FROM branch b WHERE b.fg_annullato='0' AND b.cod NOT IN " + el1;
                try (ResultSet rs1 = db.getC().createStatement().executeQuery(sql1)) {
                    while (rs1.next()) {
                        StatusBranch sb1 = new StatusBranch();

                        String fildest = rs1.getString(1);
                        sb1.setCod(fildest);
                        if (sb1.getCod().equals("000")) {
                            sb1.setIp("AWS");
                        } else {
                            sb1.setIp(db.getIpFiliale(sb1.getCod()).get(0)[1]);
                        }

                        String sql2 = "SELECT count(cod) FROM aggiornamenti_mod WHERE fg_stato='0' AND filiale = '" + fildest
                                + "' AND now()>STR_TO_DATE(dt_start, '%d/%m/%Y %H:%i:%s')";

                        try (ResultSet rs2 = db.getC().createStatement().executeQuery(sql2)) {
                            if (rs2.next()) {
                                sb1.setAggto(rs2.getInt(1));
                            }
                        }

                        if (!sb1.getCod().equals("000")) {
                            Db dbfil = new Db("//" + sb1.getIp() + ":3306/maccorp", true);
                            if (dbfil.getC() != null) {
                                String sql3 = "SELECT count(cod) FROM aggiornamenti_mod Where fg_stato='0'";
                                try (ResultSet rs3 = dbfil.getC().createStatement().executeQuery(sql3)) {
                                    if (rs3.next()) {
                                        sb1.setAggfrom(rs3.getInt(1));
                                    }
                                }
                                dbfil.closeDB();
                                sb1.setRagg(true);
                            } else {
                                sb1.setRagg(false);
                            }
                        }

                        if (sb1.getAggfrom() >= limitAGG || sb1.getAggto() >= limitAGG) {
                            try {
                                String print = "FILIALE " + sb1.getCod() + " - DA RICEVERE/CARICARE: " + sb1.getAggfrom()
                                        + " ; DA INVIARE/ESEGUIRE: " + sb1.getAggto() + " - RAGGIUNGIBILITA': " + sb1.isRagg();
                                writer.write(print);
                                writer.newLine();
                                index.addAndGet(1);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        }
                    }
                }
            }
            db.closeDB();
            if (index.get() > 0) {
                sendMail(txt);
            }
            txt.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void verifica_Aggiornamenti() {

        try {
            Db db = new Db(host_PROD, false);
            List<String> filialidanoncontrollare = Arrays.asList(db.getConf("filialinoncontrollare").split(","));
            db.closeDB();
            AtomicInteger index = new AtomicInteger(0);
            List<StatusBranch> complete = agg(null);
            File txt = new File(db.getPath("temp")
                    + RandomStringUtils.randomAlphanumeric(50) + ".txt");
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

//    public static void main(String[] args) {
//       
//    }
}
