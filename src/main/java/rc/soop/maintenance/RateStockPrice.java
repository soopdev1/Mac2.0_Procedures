/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.maintenance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import rc.soop.aggiornamenti.Db;
import static rc.soop.start.Utility.fd;
import static rc.soop.start.Utility.rb;

/**
 *
 * @author rcosco
 */
public class RateStockPrice {
//    public static void main(String[] args) {
//        engine("IT");
//    }
    public static void engine(String nazione) {

        String nameprod = "maccorpita";
        String nametest = "maccorp";
        String codvalue = "EUR";
        try {
            if (nazione.equals("CZ")) {
                nameprod = "maccorpczprod";
                nametest = "maccorpcz";
                codvalue = "CZK";
            }
        } catch (Exception e) {
            nameprod = "maccorpczprod";
            nametest = "maccorpcz";
            codvalue = "CZK";
        }

        String PROD = rb.getString("db.ip") + "/" + nameprod;
        String TEST = rb.getString("db.ip") + "/" + nametest;
        recuperorate(PROD, nameprod, nametest);
        test(TEST, codvalue);
        today(PROD, codvalue);
        today_branch(PROD, codvalue);
        recuperatuttelefiliali(TEST, true);

    }

    private static void all(String PROD, String codvalue) {
        DateTime start = new DateTime(2020, 1, 1, 0, 0);
        DateTime today = new DateTime().withMillisOfDay(0);

        while (start.isBefore(today)) {

            Db db = new Db(PROD, false);
            ArrayList<String> listCurrency = db.listCurrency(codvalue);
            String giorno = start.toString("yyyy-MM-dd");
            listCurrency.forEach(valuta1 -> {
                String value = db.get_BCE(giorno, valuta1);
                if (fd(value) > 0) {
                    Rate r1 = new Rate(giorno, valuta1, "000", value);
                    boolean es = db.insert_RATE(r1);
                    System.out.println(es + ": " + r1.toString());
                }
            });

            db.closeDB();
            start = start.plusDays(1);
            System.out.println("mactest.InsertRateStockPrice.all() " + start);
        }
    }

    private static void today_branch(String PROD, String codvalue) {
        DateTime today = new DateTime();

        Db db = new Db(PROD, false);

        ArrayList<String> listCurrency = db.listCurrency(codvalue);
        String giorno = today.toString("yyyy-MM-dd");

        listCurrency.forEach(valuta1 -> {
            String value = db.get_BCE(giorno, valuta1);
            if (fd(value) > 0) {
                Rate r1 = new Rate(giorno, valuta1, "000", value);
                boolean es = db.insert_RATE_AGG(r1);
                System.out.println(es + ": " + r1.toString());
            }
        });
    }

    private static void today(String PROD, String codvalue) {
        DateTime today = new DateTime();

        Db db = new Db(PROD, false);

        ArrayList<String> listCurrency = db.listCurrency(codvalue);
        String giorno = today.toString("yyyy-MM-dd");
        listCurrency.forEach(valuta1 -> {
            String value = db.get_BCE(giorno, valuta1);
            if (fd(value) > 0) {
                Rate r1 = new Rate(giorno, valuta1, "000", value);
                boolean es = db.insert_RATE(r1);

                System.out.println(es + ": " + r1.toString());

            }
        });

        db.closeDB();
    }

    private static void test(String TEST, String codvalue) {
        try {
            Db db = new Db(TEST, false);
            ArrayList<String[]> ip = db.getIpFiliale();
            for (int i = 0; i < ip.size(); i++) {
                String[] f1 = ip.get(i);
                String fil = f1[0];
                if (fil.equals("043") 
                        || fil.equals("079") 
                        || fil.equals("306") 
                        || fil.equals("307") 
                        || fil.equals("305")) {
                    Db dbfil = new Db("//" + f1[1] + ":3306/maccorp", true);
                    if (dbfil.getC() != null) {
                        String sql1 = "SELECT cod_value,date FROM stock WHERE total <>'0.00' "
                                + "AND tipostock = 'CH' AND kind ='01' "
                                + "AND codice NOT IN (SELECT codice FROM stock WHERE cod_value='" + codvalue + "')";
                        ResultSet rs = dbfil.getC().createStatement().executeQuery(sql1);
                        while (rs.next()) {
                            String sql2 = "SELECT * FROM rate WHERE data = '" + rs.getString("date").split(" ")[0] + "' "
                                    + "AND filiale='000' AND valuta ='" + rs.getString("cod_value") + "'";
                            System.out.println(sql2);
                            ResultSet rs0 = dbfil.getC().createStatement().executeQuery(sql2);
                            if (rs0.next()) {
                                System.out.println("testing.recupero.main() GIA' PRESENTE IN RATE");
                            } else {
                                ResultSet rs2 = db.getC().createStatement().executeQuery(sql2);
                                if (rs2.next()) {
                                    String ins2 = "INSERT INTO rate VALUES ('" + rs2.getString(1) + "','" + rs2.getString(2) + "','" + rs2.getString(3) + "','" + rs2.getString(4) + "')";
                                    boolean es1 = dbfil.getC().createStatement().executeUpdate(ins2) > 0;
                                    System.out.println(es1);
                                } else {
                                    System.out.println("testing.recupero.main() NON TROVATO");
                                }
                            }
                        }
                        dbfil.closeDB();
//                        break;
                    } else {
                        System.out.println("testing.recupero.main(NON CONNESSO)");
                    }
                }
            }

            db.closeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void prod(String PROD, String codvalue) {
        try {
            Db db = new Db(PROD, false);
            ArrayList<String[]> ip = db.getIpFiliale();
            db.closeDB();

            for (int i = 0; i < ip.size(); i++) {
                String[] f1 = ip.get(i);
                String fil = f1[0];
                if (!fil.equals("000")) {
                    System.out.println("testing.recupero.main() " + fil);
                    Db dbfil = new Db("//" + f1[1] + ":3306/maccorp", true);
                    if (dbfil.getC() != null) {
                        String sql1 = "SELECT cod_value,date FROM stock WHERE total <>'0.00' "
                                + "AND tipostock = 'CH' AND kind ='01' "
                                + "AND codice NOT IN (SELECT codice FROM stock WHERE cod_value='" + codvalue + "')";
                        ResultSet rs = dbfil.getC().createStatement().executeQuery(sql1);
                        while (rs.next()) {
                            String sql2 = "SELECT * FROM rate WHERE data = '" + rs.getString("date").split(" ")[0] + "' "
                                    + "AND filiale='000' AND valuta ='" + rs.getString("cod_value") + "'";
                            System.out.println(sql2);
                            ResultSet rs0 = dbfil.getC().createStatement().executeQuery(sql2);
                            if (rs0.next()) {
                                System.out.println("testing.recupero.main() GIA' PRESENTE IN RATE");
                            } else {
                                try {
                                    Db db1 = new Db(PROD, false);
                                    ResultSet rs2 = db1.getC().createStatement().executeQuery(sql2);
                                    if (rs2.next()) {
                                        String ins2 = "INSERT INTO rate VALUES ('" + rs2.getString(1) + "','" + rs2.getString(2) + "','" + rs2.getString(3) + "','" + rs2.getString(4) + "')";
                                        boolean es1 = dbfil.getC().createStatement().executeUpdate(ins2) > 0;
                                        System.out.println(es1);
                                    } else {
                                        System.out.println("testing.recupero.main() NON TROVATO");
                                    }
                                    db1.closeDB();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        dbfil.closeDB();
                    } else {
                        System.out.println("testing.recupero.main(NON CONNESSO)");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void recuperorate(String PROD, String nameprod, String nametest) {
        try {
            Db dbP = new Db(PROD, false);
            String sql1 = "select * from " + nameprod + ".rate WHERE data NOT IN (SELECT DISTINCT(data) FROM " + nametest + ".rate)";
            try (Statement st = dbP.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE); ResultSet rs = st.executeQuery(sql1)) {
                while (rs.next()) {
                    String ins = "INSERT INTO " + nametest + ".rate VALUES ('" + rs.getString(1) + "','"
                            + rs.getString(2) + "','" + rs.getString(3) + "','" + rs.getString(4) + "')";
                    System.out.println(ins);
                    try (Statement st2 = dbP.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                        st2.execute(ins);
                    }
                }
            }
            dbP.closeDB();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void recuperatuttelefiliali(String host, boolean test) {
        try {
            Db db = new Db(host, false);
            ArrayList<String[]> ip = db.getIpFiliale();
            Map<String, String> listarate = new HashMap<>();

            System.out.println("mactest.InsertRateStockPrice.all() " + host);
            DateTime start = new DateTime(2023, 2, 15, 0, 0);
            DateTime today = new DateTime().withMillisOfDay(0);

            while (start.isBefore(today) || start.isEqual(today)) {
                String giorno = start.toString("yyyy-MM-dd");
                String sql2 = "SELECT data,valuta,rif_bce FROM rate WHERE data = '" + giorno + "' AND filiale='000'";
                ResultSet rs2 = db.getC().createStatement().executeQuery(sql2);
                while (rs2.next()) {
                    String r_key = rs2.getString(1) + ";" + rs2.getString(2);
                    String r_value = rs2.getString(3);
                    listarate.put(r_key, r_value);
                }
                start = start.plusDays(1);
            }
            db.closeDB();

//            listarate.forEach((K,V) -> System.out.println(K + ", BCE : " + V));
            for (int i = 0; i < ip.size(); i++) {
                String[] f1 = ip.get(i);
                String fil = f1[0];
                if (!fil.equals("000")) {
                    if (test) {
                        if (!fil.equals("019") && !fil.equals("043") && !fil.equals("051") && !fil.equals("079")
                                && !fil.equals("306") && !fil.equals("307") && !fil.equals("305")) {
                            continue;
                        }
                    }

                    Db dbfil = new Db("//" + f1[1] + ":3306/maccorp", true);
                    if (dbfil.getC() != null) {
                        System.out.println(fil + " CONNESSO");

                        listarate.forEach((K, V)
                                -> {
                            String data = K.split(";")[0];
                            String valuta = K.split(";")[1];
                            String sql3 = "SELECT data,rif_bce FROM rate WHERE data = '" + data + "' AND valuta = '"
                                    + valuta + "' AND filiale ='000'";
                            try {
                                ResultSet rs3 = dbfil.getC_RAF("//" + f1[1] + ":3306/maccorp").createStatement().executeQuery(sql3);
                                if (!rs3.next()) {
                                    String insert = "INSERT INTO rate VALUES ('" + data + "','" + valuta + "','000','" + V + "')";
                                    dbfil.getC_RAF("//" + f1[1] + ":3306/maccorp").createStatement().execute(insert);
                                    System.out.println(fil + " -- " + insert);
                                }
                            } catch (Exception e) {
                                System.out.println("ERRORE " + fil + " " + e.getMessage());
                            }
                        });

                        dbfil.closeDB();
                    } else {
                        System.out.println(fil + " NON CONNESSO");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
