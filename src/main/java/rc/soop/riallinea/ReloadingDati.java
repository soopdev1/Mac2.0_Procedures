/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.riallinea;

import static rc.soop.riallinea.Util.createLog;
import static rc.soop.riallinea.Util.fd;
import static rc.soop.riallinea.Util.formatStringtoStringDate;
import static rc.soop.riallinea.Util.patternita;
import static rc.soop.riallinea.Util.patternsql;
import static rc.soop.riallinea.Util.roundDoubleandFormat;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import rc.soop.maintenance.Monitor;
import static rc.soop.riallinea.AllineaRealOsp.allineaReport;
import static rc.soop.riallinea.Util.estraiEccezione;

/**
 *
 * @author rcosco
 */
public class ReloadingDati {

    public static final Logger log = createLog("Mac2.0_ProceduraRecuperoErrori_", Monitor.rb.getString("path.log"), "yyyyMMdd");

    private static void fase3(DateTime start) {
        if (start == null) {
            start = new DateTime().minusDays(1);
        }

        Db_Master db0 = new Db_Master();
        List<IpFiliale> fi1ial_list_ip = db0.getIpFiliale();
        db0.closeDB();

        List<String> lista = new ArrayList<>();
        try {
            Db_Master db1 = new Db_Master();
            try (Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE); ResultSet rs1 = st1.executeQuery("SELECT cod FROM maccorpita.branch WHERE cod<>'000' AND "
                    + "(fg_annullato = '0' OR (fg_annullato ='1' AND da_annull > '2020-01-01'))")) {
                while (rs1.next()) {
                    lista.add(rs1.getString(1));
                }
            }
            db1.closeDB();
        } catch (Exception e) {
            log.severe(ExceptionUtils.getStackTrace(e));
        }

        for (int i = 0; i < lista.size(); i++) {
            String fil = lista.get(i);
            log.log(Level.WARNING, "INIZIO: {0}", fil);
            try {
                fase1(fil, start);
            } catch (Exception e1) {
                log.severe(estraiEccezione(e1));
            }

            try {
                fase1_dbfiliale(fil, start, fi1ial_list_ip.stream().filter(f1 -> f1.getFiliale().equals(fil)).findAny().get().getIp());
            } catch (Exception e1) {
                log.severe(estraiEccezione(e1));
            }
            try {
                String fil2[] = {fil, fil};
                Db_Master db = new Db_Master();
                boolean central_resp = allineaReport(db, fil2);
                db.closeDB();

                if (!central_resp) {
                    log.severe("ALLINEAMENTO CENTRALE FALLITO. RIPROVARE. FILIALE: " + fil);
                } else {
                    Db_Master filialdb = new Db_Master(true, fi1ial_list_ip.stream().filter(f1 -> f1.getFiliale().equals(fil)).findAny().get().getIp());
                    boolean filial_resp = allineaReport(filialdb, fil2);
                    filialdb.closeDB();
                    if (!filial_resp) {
                        log.severe("ALLINEAMENTO FILIALE FALLITO. RIPROVARE. FILIALE: " + fil);
                    }
                }

            } catch (Exception e1) {
                log.severe(estraiEccezione(e1));
            }

            try {
                allineaOSP_Prec(fil, fi1ial_list_ip.stream().filter(f1 -> f1.getFiliale().equals(fil)).findAny().get().getIp());
            } catch (Exception e1) {
                log.severe(estraiEccezione(e1));
            }

            log.log(Level.WARNING, "FINE: {0}", fil);
        }

    }

    private static void allineaOSP_Prec(String fil_cod, String ip) {
        Db_Master db = new Db_Master();
        DateTime start = new DateTime().minusDays(10).withMillisOfDay(0);
        DateTime end = new DateTime().withMillisOfDay(0);
        while (start.isBefore(end)) {
            String stdate = start.toString(patternsql) + " 23:59:59";
            String fil[] = {fil_cod, fil_cod};
            ArrayList<BranchStockInquiry_value> dati = db.list_BranchStockInquiry_value(fil, stdate, "CH");
            if (!dati.isEmpty()) {
                Office_sp sp = db.list_query_officesp2(fil[0], stdate.substring(0, 10)).get(0);
                ArrayList<OfficeStockPrice_value> last = db.list_OfficeStockPrice_value(
                        sp.getCodice(), fil[0]);
                for (int x = 0; x < last.size(); x++) {
                    OfficeStockPrice_value od = last.get(x);

                    for (int i = 0; i < dati.size(); i++) {

                        if (dati.get(i).getCurrency().equalsIgnoreCase(od.getCurrency()) && !od.getQta().equals(dati.get(i).getDati().get(0).toString())) {

                            double nc = fd(dati.get(i).getDati().get(0).toString()) * fd(od.getMedioacq());
                            String upd = "UPDATE office_sp_valori SET quantity = '" + dati.get(i).getDati().get(0).toString() + "', controv = '" + roundDoubleandFormat(nc, 2) + "' "
                                    + "WHERE cod ='" + sp.getCodice() + "' AND currency ='" + od.getCurrency() + "' AND kind ='01'";

                            Db_Master filialdb = new Db_Master(true, ip);
                            if (filialdb.getC() == null) {
                                log.info("ALLINEAMENTO FILIALE FALLITO. IMPOSSIBILE CONNETTERSI A FILIALE " + fil_cod);
                            } else {
                                try {
                                    int upd_FILIAL = filialdb.getC().createStatement().executeUpdate(upd);
                                    if (upd_FILIAL > 0) {
                                        db.getC().createStatement().executeUpdate(upd);
                                    }
                                } catch (Exception ex1) {
                                    log.severe("ALLINEAMENTO FILIALE " + fil_cod + " FALLITO. RIPROVARE,");
                                    log.severe(estraiEccezione(ex1));
                                }
                                filialdb.closeDB();
                            }

                        }
                    }

                }
            }
            start = start.plusDays(1);
        }

        db.closeDB();

    }

    public static void riallinea() {
        try {
            Db_Master db = new Db_Master();
            String sql = "SELECT v.cod FROM et_change e, et_change_valori v WHERE e.cod=v.cod AND v.ip_spread<>'0.00' "
                    + "AND e.fg_brba='BR' AND e.dt_it > '2022-06-01' AND e.fg_annullato='0'";
            try (Statement st = db.getC().createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String upd = "UPDATE et_change_valori SET ip_spread = '0.00' WHERE ip_spread<>'0.00' AND cod='" + rs.getString(1) + "'";
                    try (Statement st2 = db.getC().createStatement()) {
                        st2.executeUpdate(upd);
                    }
                }
            }
            db.closeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
        fase3(null);
    }

    public static void main(String[] args) {
        //CHECK EXTERNAL 
//        try {
//            Db_Master db = new Db_Master();
//            String sql = "SELECT v.cod FROM et_change e, et_change_valori v WHERE e.cod=v.cod AND v.ip_spread<>'0.00' "
//                    + "AND e.fg_brba='BR' AND e.dt_it > '2022-06-01' AND e.fg_annullato='0'";
//            try ( Statement st = db.getC().createStatement();  ResultSet rs = st.executeQuery(sql)) {
//                while (rs.next()) {
//                    String upd = "UPDATE et_change_valori SET ip_spread = '0.00' WHERE ip_spread<>'0.00' AND cod='" + rs.getString(1) + "'";
//                    try ( Statement st2 = db.getC().createStatement()) {
//                        st2.executeUpdate(upd);
//                    }
//                }
//            }
//            db.closeDB();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        fase3(null);

        DateTime start = new DateTime(2023, 9, 30, 0, 0).withMillisOfDay(0);
//        fase3(start);
//
        List<String> lista = new ArrayList<>();
//        lista.add("306");
//        lista.add("307");
//        lista.add("312");
//        lista.add("321");
//////        lista.add("172");
////        lista.add("190");
////        lista.add("195");
////        lista.add("048");
////        lista.add("063");
////        lista.add("090");
//        lista.add("019");
        lista.add("202");
//        lista.add("188");
////        lista.add("172");
////        lista.add("188");
////        lista.add("195");
////
        for (int i = 0; i < lista.size(); i++) {
            String fil = lista.get(i);
            log.log(Level.WARNING, "INIZIO: {0}", fil);
            fase1(fil, start);
            log.log(Level.WARNING, "FINE: {0}", fil);
//            fase1_dbfiliale(fil, start, "192.168.115.106");
        }
//        DateTime start = new DateTime().withMillisOfDay(0);
//
//        LinkedList<Dati> result = main("090", start);
//
//        result.forEach(d1 -> {
//            System.out.println(d1.toString());
//        });
//        fase1("090", new DateTime(2021, 1, 20, 0, 0).withMillisOfDay(0));
//        fase3();
//        List<String> listaok = new ArrayList<>();
//        listaok.add("010");//07/12
//        List<String> lista = new ArrayList<>();
//        try {
//
//            Db_Master db1 = new Db_Master();
//            Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
//            ResultSet rs1 = st1.executeQuery("SELECT cod FROM maccorpita.branch WHERE cod<>'000' AND (fg_annullato = '0' OR (fg_annullato ='1' AND da_annull > '2020-01-01')) AND cod NOT IN (SELECT DISTINCT(filiale) FROM macreport.dailyerror)");
//            while (rs1.next()) {
//                if (!listaok.contains(rs1.getString(1))) {
//                    lista.add(rs1.getString(1));
//                }
//            }
//            rs1.close();
//            st1.close();
//            db1.closeDB();
//        } catch (Exception e) {
//            log.severe(ExceptionUtils.getStackTrace(e));
//        }
//
//        for (int i = 0; i < lista.size(); i++) {
//            String fil = lista.get(i);
//            log.warning("INIZIO: " + fil);
//            fase1(fil);
//            fase2(fil);
//            log.warning("FINE: " + fil);
//        }
//        fase2("010");
    }

    public static void fase1_dbfiliale(String filiale, DateTime start, String ip) {

        boolean update = true;

//        DateTime end = new DateTime(2019, 12, 31, 0, 0);
        DateTime end = new DateTime().minusDays(1).withMillisOfDay(0);

        while (start.isBefore(end)) {

            LinkedList<Dati> result = main_dbfiliale(filiale, start, ip);

            Dati dato = result.get(1);

            boolean ERgiornoincorso = false;
            if ((dato.getDAY_COP() == dato.getBR_ST_IN()) && (dato.getDAY_COP() == dato.getOF_ST_PR()) && (dato.getBR_ST_IN() == dato.getOF_ST_PR())) {
            } else {
                ERgiornoincorso = true;
            }

            if (ERgiornoincorso) {

                if (dato.getBR_ST_IN() != dato.getDAY_COP()) {
                    log.log(Level.WARNING, "{0} -> ERRORE BSI DIVERSO DA DAILY COP: {1} -- {2}", new Object[]{dato.getDATA(), dato.getBR_ST_IN(), dato.getDAY_COP()});
                    String d = roundDoubleandFormat(dato.getBR_ST_IN() - dato.getDAY_COP(), 2);
                    double diff = fd(d);
                    String dU = "0.00";
                    String dS = "0.00";

                    if (diff > 0) {
                        dU = d;
                    } else {
                        dS = StringUtils.replace(d, "-", "");
                    }
                    try {
                        Db_Master db1 = new Db_Master();
                        Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        st1.execute("INSERT INTO macreport.dailyerror_branch VALUES('" + filiale
                                + "','" + formatStringtoStringDate(dato.getDATA().split(" ")[0], patternsql, patternita)
                                + "','" + dato.getBR_ST_IN()
                                + "','" + dato.getDAY_COP()
                                + "','" + d + "','','');");
                        st1.close();
                        db1.closeDB();
                    } catch (Exception e) {
                        log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
                        log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
                        break;
                    }

                    String newcod = "ERR" + generaId(22);

                    String insert1 = "INSERT INTO oc_lista VALUES ('" + dato.getFILIALE() + "','"
                            + newcod + "','000000000000000','000','0000','C','"
                            + dato.getDATA() + " 01:00:00','Y','N','N','N','-','-')";

                    String insert2 = "INSERT INTO oc_errors VALUES ('" + dato.getFILIALE() + "','"
                            + newcod + "','CH','EUR','01','-','-','Riallineamento valore EURO a seguito di errore di sistema.','" + d + "','"
                            + dato.getDATA() + " 01:00:00','1.00000000','0.00','" + dU + "','0.00','" + dS + "')";
                    try {

                        Db_Master db1 = new Db_Master(true, ip);
                        Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        Statement st2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        if (update) {
                            st1.execute(insert1);
                            st1.execute(insert2);
                        }
                        st1.close();
                        st2.close();
                        db1.closeDB();
                        log.info("OK: " + insert1);
                        log.info("OK: " + insert2);
//                        break;
                    } catch (Exception e) {
                        log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
                        log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
                        break;
                    }

                } else if ((dato.getBR_ST_IN() != dato.getOF_ST_PR()) || (dato.getDAY_COP() != dato.getOF_ST_PR())) {
                    log.warning(dato.getDATA() + " -> ERRORE BSI (E DAILY COP) DIVERSO DA OSP: " + dato.getBR_ST_IN() + " -- " + dato.getOF_ST_PR());
                    try {
                        String query1 = "SELECT codice FROM office_sp WHERE filiale='" + filiale + "' AND data <= '" + dato.getDATA() + " 23:59:59' ORDER BY data DESC LIMIT 1;";

                        Db_Master db0 = new Db_Master(true, ip);
                        Statement st0 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

                        ResultSet rs0 = st0.executeQuery(query1);

                        if (rs0.next()) {

                            String upd1 = "UPDATE office_sp SET total_cod = '" + dato.getBR_ST_IN() + "' WHERE codice='" + rs0.getString(1) + "'";
                            String upd2 = "UPDATE office_sp_valori SET controv = '" + dato.getBR_ST_IN() + "' WHERE cod='" + rs0.getString(1) + "' AND currency = 'EUR' AND kind = '01'";

                            boolean es1 = false;
                            boolean es2 = false;

                            Statement st1 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            Statement st2 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            if (update) {
                                es1 = st1.executeUpdate(upd1) > 0;
                                es2 = st2.executeUpdate(upd2) > 0;
                            }
                            st1.close();
                            st2.close();
                            log.info(es1 + ": " + upd1);
                            log.info(es2 + ": " + upd2);
                            start = start.plusDays(1);
                        }
                        st0.close();
                        db0.closeDB();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } else {
                log.info("OK " + dato.toString());
                start = start.plusDays(1);
            }
        }

    }

    private static void fase2_dbfiliale(String filiale, String ip) {
        try {

            Db_Master db0 = new Db_Master(true, ip);

            Statement st0 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            Statement st00 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

            String del1 = "DELETE FROM oc_errors where cod IN (SELECT cod FROM oc_lista where cod LIKE 'ER%'"
                    + " AND errors='Y' AND filiale='" + filiale + "' AND id='000000000000000' AND DATA < '2020-01-01 00:00:00')";
            String del2 = "DELETE FROM oc_lista where cod LIKE 'ER%' AND errors='Y' AND filiale='" + filiale
                    + "' AND id='000000000000000' AND DATA < '2020-01-01 00:00:00'";

            st0.executeUpdate(del1);
            st00.executeUpdate(del2);

            st0.close();
            st00.close();
            db0.closeDB();

            String sql1 = "SELECT SUM(diff) FROM macreport.dailyerror_branch"
                    + " WHERE filiale='" + filiale + "' AND STR_TO_DATE(DATA, '%d/%c/%Y') <'2020-01-01'";

            db0 = new Db_Master();
            Statement st01 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs1 = st01.executeQuery(sql1);
            if (rs1.next()) {

                if (rs1.getString(1) == null) {
                    log.log(Level.INFO, "{0} TUTTO OK", filiale);
                } else {
                    String d = roundDoubleandFormat(fd(rs1.getString(1)), 2);
                    double diff = fd(d);

                    String dU = "0.00";
                    String dS = "0.00";
                    if (diff > 0) {
                        dU = d;
                    } else {
                        dS = StringUtils.replace(d, "-", "");
                    }

                    String newcod = "ERR" + generaId(22);

                    String insert1 = "INSERT INTO oc_lista VALUES ('" + filiale + "','"
                            + newcod + "','000000000000000','000','0000','C','"
                            + "2020-01-01 01:00:00','Y','N','N','N','-','-')";

                    String insert2 = "INSERT INTO oc_errors VALUES ('" + filiale + "','"
                            + newcod + "','CH','EUR','01','-','-','Riallineamento valore EURO a seguito di errore di sistema.','"
                            + d + "','" + "2020-01-01 01:00:00','0.00','0.00','" + dU + "','0.00','" + dS + "')";
                    Db_Master db1 = new Db_Master(true, ip);
                    Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    Statement st2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    st1.execute(insert1);
                    st1.execute(insert2);
                    st1.close();
                    st2.close();

                    String sql3 = "SELECT codice,total_cod FROM office_sp WHERE filiale='" + filiale
                            + "' AND data <= '2019-12-31 23:59:59' ORDER BY data DESC LIMIT 1";
                    Statement st3 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    ResultSet rs3 = st3.executeQuery(sql3);
                    if (rs3.next()) {
                        String cod = rs3.getString(1);
                        double t_start = fd(roundDoubleandFormat(fd(rs3.getString(2)), 2));
                        log.info(cod + " START) " + t_start);
                        log.info(cod + " DIFF) " + d);
                        log.info(cod + " OUTPUT) " + roundDoubleandFormat(t_start - diff, 2));

                        String upd1 = "UPDATE office_sp SET total_cod = '" + roundDoubleandFormat(t_start - diff, 2) + "' WHERE codice='" + cod + "'";
                        String upd2 = "UPDATE office_sp_valori SET controv = '" + roundDoubleandFormat(t_start - diff, 2) + "' WHERE cod='" + cod + "' AND currency = 'EUR' AND kind = '01'";

                        Statement st001 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        Statement st02 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        st001.executeUpdate(upd1);
                        st02.executeUpdate(upd2);
                        st001.close();
                        st02.close();
                        db1.closeDB();
                    }
                    rs3.close();
                    st3.close();

                }
            }
            rs1.close();
            st01.close();
            db0.closeDB();
        } catch (Exception e) {
            log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
            log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
        }

    }

    private static void fase1(String filiale, DateTime start) {

        boolean update = true;

//        DateTime end = new DateTime(2020, 12, 31, 0, 0);
        DateTime end = new DateTime().withMillisOfDay(0);

        while (start.isBefore(end)) {

            LinkedList<Dati> result = main(filiale, start);

            Dati dato = result.get(1);

            boolean ERgiornoincorso = false;
            if ((dato.getDAY_COP() == dato.getBR_ST_IN()) && (dato.getDAY_COP() == dato.getOF_ST_PR()) && (dato.getBR_ST_IN() == dato.getOF_ST_PR())) {
            } else {
                ERgiornoincorso = true;
            }

            if (ERgiornoincorso) {
                if (dato.getBR_ST_IN() != dato.getDAY_COP()) {
                    log.warning(dato.getDATA()
                            + " -> ERRORE BSI DIVERSO DA DAILY COP: "
                            + dato.getBR_ST_IN() + " -- "
                            + dato.getDAY_COP());
                    String d = roundDoubleandFormat(dato.getBR_ST_IN() - dato.getDAY_COP(), 2);
                    double diff = fd(d);
                    String dU = "0.00";
                    String dS = "0.00";

                    if (diff > 0) {
                        dU = d;
                    } else {
                        dS = StringUtils.replace(d, "-", "");
                    }
                    try {
                        Db_Master db1 = new Db_Master();
                        Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        st1.execute("INSERT INTO macreport.dailyerror VALUES('" + filiale
                                + "','" + formatStringtoStringDate(dato.getDATA().split(" ")[0], patternsql, patternita)
                                + "','" + dato.getBR_ST_IN()
                                + "','" + dato.getDAY_COP()
                                + "','" + d + "','','');");
                        st1.close();
                        db1.closeDB();
                    } catch (Exception e) {

                        if (e.getMessage().toLowerCase().contains("duplicate entry")) {
                            start = start.plusDays(1);
                            continue;
                        }
                        log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
                        log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
                    }

                    String newcod = "ERR" + generaId(22);

                    String insert1 = "INSERT INTO oc_lista VALUES ('" + dato.getFILIALE() + "','"
                            + newcod + "','000000000000000','000','0000','C','"
                            + dato.getDATA() + " 01:00:00','Y','N','N','N','-','-')";

                    String insert2 = "INSERT INTO oc_errors VALUES ('" + dato.getFILIALE() + "','"
                            + newcod + "','CH','EUR','01','-','-','Riallineamento valore EURO a seguito di errore di sistema.','" + d + "','"
                            + dato.getDATA() + " 01:00:00','1.00000000','0.00','" + dU + "','0.00','" + dS + "')";
                    try {

                        Db_Master db1 = new Db_Master();
                        Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        Statement st2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        if (update) {
                            st1.execute(insert1);
                            st1.execute(insert2);
                        }
                        st1.close();
                        st2.close();
                        db1.closeDB();
                        log.info("OK: " + insert1);
                        log.info("OK: " + insert2);
//                        break;
                    } catch (Exception e) {
                        log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
                        log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
                        break;
                    }

                } else if ((dato.getBR_ST_IN() != dato.getOF_ST_PR()) || (dato.getDAY_COP() != dato.getOF_ST_PR())) {
                    log.warning(dato.getDATA() + " -> ERRORE BSI (E DAILY COP) DIVERSO DA OSP: " + dato.getBR_ST_IN() + " -- " + dato.getOF_ST_PR());
                    try {
                        String query1 = "SELECT codice FROM office_sp WHERE filiale='" + filiale + "' AND data <= '" + dato.getDATA() + " 23:59:59' ORDER BY data DESC LIMIT 1;";

                        Db_Master db0 = new Db_Master();
                        Statement st0 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

                        ResultSet rs0 = st0.executeQuery(query1);

                        if (rs0.next()) {

                            String upd1 = "UPDATE office_sp SET total_cod = '" + dato.getBR_ST_IN() + "' WHERE codice='" + rs0.getString(1) + "'";
                            String upd2 = "UPDATE office_sp_valori SET controv = '" + dato.getBR_ST_IN() + "' WHERE cod='" + rs0.getString(1) + "' AND currency = 'EUR' AND kind = '01'";

                            boolean es1 = false;
                            boolean es2 = false;

                            Statement st1 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            Statement st2 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            if (update) {
                                es1 = st1.executeUpdate(upd1) > 0;
                                es2 = st2.executeUpdate(upd2) > 0;
                            }
                            st1.close();
                            st2.close();
                            log.info(es1 + ": " + upd1);
                            log.info(es2 + ": " + upd2);
                            start = start.plusDays(1);
                        }
                        st0.close();
                        db0.closeDB();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } else {
                log.info("OK " + dato.toString());
                start = start.plusDays(1);
            }
        }

    }

    private static void fase2(String filiale) {
        try {

            Db_Master db0 = new Db_Master();

            Statement st0 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            Statement st00 = db0.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

            String del1 = "DELETE FROM oc_errors where cod IN (SELECT cod FROM oc_lista where cod LIKE 'ER%'"
                    + " AND errors='Y' AND filiale='" + filiale + "' AND id='000000000000000' AND DATA < '2020-01-01 00:00:00')";
            String del2 = "DELETE FROM oc_lista where cod LIKE 'ER%' AND errors='Y' AND filiale='" + filiale
                    + "' AND id='000000000000000' AND DATA < '2020-01-01 00:00:00'";

            st0.executeUpdate(del1);
            st00.executeUpdate(del2);

            st0.close();
            st00.close();
            db0.closeDB();

            String sql1 = "SELECT SUM(diff) FROM macreport.dailyerror WHERE filiale='" + filiale + "' AND STR_TO_DATE(DATA, '%d/%c/%Y') <'2020-01-01'";

            Db_Master db1 = new Db_Master();
            Statement st01 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs1 = st01.executeQuery(sql1);
            if (rs1.next()) {

                if (rs1.getString(1) == null) {
                    log.log(Level.INFO, "{0} TUTTO OK", filiale);
                } else {
                    String d = roundDoubleandFormat(fd(rs1.getString(1)), 2);
                    double diff = fd(d);

                    String dU = "0.00";
                    String dS = "0.00";
                    if (diff > 0) {
                        dU = d;
                    } else {
                        dS = StringUtils.replace(d, "-", "");
                    }

                    String newcod = "ERR" + generaId(22);

                    String insert1 = "INSERT INTO oc_lista VALUES ('" + filiale + "','"
                            + newcod + "','000000000000000','000','0000','C','"
                            + "2020-01-01 01:00:00','Y','N','N','N','-','-')";

                    String insert2 = "INSERT INTO oc_errors VALUES ('" + filiale + "','"
                            + newcod + "','CH','EUR','01','-','-','Riallineamento valore EURO a seguito di errore di sistema.','"
                            + d + "','" + "2020-01-01 01:00:00','1.00000000','0.00','" + dU + "','0.00','" + dS + "')";

                    Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    Statement st2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    st1.execute(insert1);
                    st1.execute(insert2);
                    st1.close();
                    st2.close();

                    String sql3 = "SELECT codice,total_cod FROM office_sp WHERE filiale='" + filiale
                            + "' AND data <= '2019-12-31 23:59:59' ORDER BY data DESC LIMIT 1";
                    Statement st3 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    ResultSet rs3 = st3.executeQuery(sql3);
                    if (rs3.next()) {
                        String cod = rs3.getString(1);
                        double t_start = fd(roundDoubleandFormat(fd(rs3.getString(2)), 2));
                        log.info(cod + " START) " + t_start);
                        log.info(cod + " DIFF) " + d);
                        log.info(cod + " OUTPUT) " + roundDoubleandFormat(t_start - diff, 2));

                        String upd1 = "UPDATE office_sp SET total_cod = '" + roundDoubleandFormat(t_start - diff, 2) + "' WHERE codice='" + cod + "'";
                        String upd2 = "UPDATE office_sp_valori SET controv = '" + roundDoubleandFormat(t_start - diff, 2) + "' WHERE cod='" + cod + "' AND currency = 'EUR' AND kind = '01'";

                        Statement st001 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        Statement st02 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        st001.executeUpdate(upd1);
                        st02.executeUpdate(upd2);
                        st001.close();
                        st02.close();

                    }
                    rs3.close();
                    st3.close();

                }
            }
            rs1.close();
            st01.close();
            db1.closeDB();
        } catch (Exception e) {
            log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
            log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
        }

    }

    private static LinkedList<Dati> main_dbfiliale(String fil, DateTime oggi, String ip) {
        LinkedList<Dati> out = new LinkedList<>();
        String[] filiale = {fil, fil};

        DateTime ieri = oggi.minusDays(1);

        String datad1 = ieri.toString("yyyy-MM-dd") + " 23:59:59";
        Db_Master db1 = new Db_Master(true, ip);
        double br_si = 0.00;
        double osp = 0.00;
        try {
            String sql = "SELECT f.cod,f.data,f.id,f.user,f.fg_tipo,f.till "
                    + "FROM (SELECT till, MAX(data) AS maxd FROM oc_lista WHERE data<'" + datad1 + "'  AND filiale = '" + filiale[0] + "' GROUP BY till) "
                    + "AS x INNER JOIN oc_lista AS f ON f.till = x.till AND f.data = x.maxd AND f.filiale = '" + filiale[0] + "' AND f.data<'" + datad1 + "'"
                    + " ORDER BY f.till";
            ResultSet rs = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql);

            while (rs.next()) {
                String sql2 = "SELECT total FROM stock_report where filiale='" + filiale[0] + "' "
                        + "AND data<'" + datad1 + "' AND tipo='CH' AND kind='01' AND cod_value = 'EUR' "
                        + "AND (codiceopenclose = '" + rs.getString("f.cod") + "' OR codtr = '" + rs.getString("f.cod") + "') "
                        + "AND till='" + rs.getString("f.till") + "'";

                ResultSet rs2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql2);
                while (rs2.next()) {
                    br_si = br_si + fd(rs2.getString(1));
                }
            }

            String sqlosp = "SELECT total_cod FROM office_sp WHERE filiale='" + filiale[0] + "' AND data <= '" + datad1 + "' order by data desc limit 1";
//                System.out.println(sqlosp);
            ResultSet rsosp = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sqlosp);
            if (rsosp.next()) {
                osp = fd(rsosp.getString(1));
            }

        } catch (SQLException e) {
            log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
            log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
        }

        DailyResult res = list_Daily_value_NEW(filiale, datad1.split(" ")[0] + " 00:00", datad1.split(" ")[0] + " 23:30", db1);
        Double[] day = res.getValori();

        db1.closeDB();

        out.add(new Dati(filiale[0], datad1.split(" ")[0], ieri, fd(roundDoubleandFormat(day[0], 2)), fd(roundDoubleandFormat(br_si, 2)),
                fd(roundDoubleandFormat(osp, 2)), fd(roundDoubleandFormat(day[1], 2)), fd(roundDoubleandFormat(day[2], 2)),
                res.getCODICE_OFP(), fd(roundDoubleandFormat(day[3], 2))));

        datad1 = oggi.toString("yyyy-MM-dd") + " 23:59:59";

        db1 = new Db_Master(true, ip);

        br_si = 0.00;
        osp = 0.00;

        try {
            String sql = "SELECT f.cod,f.data,f.id,f.user,f.fg_tipo,f.till "
                    + "FROM (SELECT till, MAX(data) AS maxd FROM oc_lista WHERE data<'" + datad1 + "'  AND filiale = '" + filiale[0] + "' GROUP BY till) "
                    + "AS x INNER JOIN oc_lista AS f ON f.till = x.till AND f.data = x.maxd AND f.filiale = '" + filiale[0] + "' AND f.data<'" + datad1 + "'"
                    + " ORDER BY f.till";
            ResultSet rs = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql);

            while (rs.next()) {
                String sql2 = "SELECT total FROM stock_report where filiale='" + filiale[0] + "' "
                        + "AND data<'" + datad1 + "' AND tipo='CH' AND kind='01' AND cod_value = 'EUR' "
                        + "AND (codiceopenclose = '" + rs.getString("f.cod") + "' OR codtr = '" + rs.getString("f.cod") + "') "
                        + "AND till='" + rs.getString("f.till") + "'";

                ResultSet rs2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql2);
                while (rs2.next()) {
                    br_si = br_si + fd(rs2.getString(1));
                }
            }

            String sqlosp = "SELECT total_cod FROM office_sp WHERE filiale='" + filiale[0] + "' AND data <= '" + datad1 + "' order by data desc limit 1";
            ResultSet rsosp = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sqlosp);
            if (rsosp.next()) {
                osp = fd(rsosp.getString(1));
            }

        } catch (SQLException e) {
            log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
            log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
        }
        res = list_Daily_value_NEW(filiale, datad1.split(" ")[0] + " 00:00", datad1.split(" ")[0] + " 23:30", db1);
        day = res.getValori();

        db1.closeDB();

        out.add(new Dati(filiale[0], datad1.split(" ")[0], oggi, fd(roundDoubleandFormat(day[0], 2)), fd(roundDoubleandFormat(br_si, 2)),
                fd(roundDoubleandFormat(osp, 2)), fd(roundDoubleandFormat(day[1], 2)), fd(roundDoubleandFormat(day[2], 2)),
                res.getCODICE_OFP(), fd(roundDoubleandFormat(day[3], 2))));

        return out;
    }

    public static LinkedList<Dati> main(String fil, DateTime oggi) {
        LinkedList<Dati> out = new LinkedList<>();
        try {
            String[] filiale = {fil, fil};

            DateTime ieri = oggi.minusDays(1);

            String datad1 = ieri.toString("yyyy-MM-dd") + " 23:59:59";
            Db_Master db1 = new Db_Master();
            double br_si = 0.00;
            double osp = 0.00;

            String sql = "SELECT f.cod,f.data,f.id,f.user,f.fg_tipo,f.till "
                    + "FROM (SELECT till, MAX(data) AS maxd FROM oc_lista WHERE data<'" + datad1 + "'  AND filiale = '" + filiale[0] + "' GROUP BY till) "
                    + "AS x INNER JOIN oc_lista AS f ON f.till = x.till AND f.data = x.maxd AND f.filiale = '" + filiale[0] + "' AND f.data<'" + datad1 + "'"
                    + " ORDER BY f.till";
            ResultSet rs = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql);

            while (rs.next()) {
                String sql2 = "SELECT total FROM stock_report where filiale='" + filiale[0] + "' "
                        + "AND data<'" + datad1 + "' AND tipo='CH' AND kind='01' AND cod_value = 'EUR' "
                        + "AND (codiceopenclose = '" + rs.getString("f.cod") + "' OR codtr = '" + rs.getString("f.cod") + "') "
                        + "AND till='" + rs.getString("f.till") + "'";
                ResultSet rs2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql2);
                while (rs2.next()) {
                    br_si = br_si + fd(rs2.getString(1));
                }
            }

            String sqlosp = "SELECT total_cod,total_fx FROM office_sp WHERE filiale='" + filiale[0] + "' AND data <= '" + datad1 + "' order by data desc limit 1";
//                System.out.println(sqlosp);
            ResultSet rsosp = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sqlosp);
            if (rsosp.next()) {
                osp = fd(rsosp.getString(1));
            }

            DailyResult res = list_Daily_value_NEW(filiale, datad1.split(" ")[0] + " 00:00", datad1.split(" ")[0] + " 23:30", db1);
            Double[] day = res.getValori();

            out.add(new Dati(filiale[0], datad1.split(" ")[0], oggi, fd(roundDoubleandFormat(day[0], 2)), fd(roundDoubleandFormat(br_si, 2)),
                    fd(roundDoubleandFormat(osp, 2)), fd(roundDoubleandFormat(day[1], 2)), fd(roundDoubleandFormat(day[2], 2)),
                    res.getCODICE_OFP(), fd(roundDoubleandFormat(day[3], 2))));

            datad1 = oggi.toString("yyyy-MM-dd") + " 23:59:59";

            br_si = 0.00;
            osp = 0.00;

            String sql1 = "SELECT f.cod,f.data,f.id,f.user,f.fg_tipo,f.till "
                    + "FROM (SELECT till, MAX(data) AS maxd FROM oc_lista WHERE data<'" + datad1 + "'  AND filiale = '" + filiale[0] + "' GROUP BY till) "
                    + "AS x INNER JOIN oc_lista AS f ON f.till = x.till AND f.data = x.maxd AND f.filiale = '" + filiale[0] + "' AND f.data<'" + datad1 + "'"
                    + " ORDER BY f.till";
            ResultSet rs1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql1);

            while (rs1.next()) {
                String sql2 = "SELECT total FROM stock_report where filiale='" + filiale[0] + "' "
                        + "AND data<'" + datad1 + "' AND tipo='CH' AND kind='01' AND cod_value = 'EUR' "
                        + "AND (codiceopenclose = '" + rs1.getString("f.cod") + "' OR codtr = '" + rs1.getString("f.cod") + "') "
                        + "AND till='" + rs1.getString("f.till") + "'";

                ResultSet rs2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql2);
                while (rs2.next()) {
                    br_si = br_si + fd(rs2.getString(1));
                }
            }

            String sqlosp1 = "SELECT total_cod FROM office_sp WHERE filiale='" + filiale[0] + "' AND data <= '" + datad1 + "' order by data desc limit 1";
            ResultSet rsosp1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sqlosp1);
            if (rsosp1.next()) {
                osp = fd(rsosp1.getString(1));
            }

//        day = list_Daily_value_NEW(filiale, datad1.split(" ")[0] + " 00:00", datad1.split(" ")[0] + " 23:30", db1);
            res = list_Daily_value_NEW(filiale, datad1.split(" ")[0] + " 00:00", datad1.split(" ")[0] + " 23:30", db1);
            day = res.getValori();

            db1.closeDB();

            out.add(new Dati(filiale[0], datad1.split(" ")[0], oggi, fd(roundDoubleandFormat(day[0], 2)), fd(roundDoubleandFormat(br_si, 2)),
                    fd(roundDoubleandFormat(osp, 2)), fd(roundDoubleandFormat(day[1], 2)), fd(roundDoubleandFormat(day[2], 2)),
                    res.getCODICE_OFP(), fd(roundDoubleandFormat(day[3], 2))));

        } catch (Exception e) {
            log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
            log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
        }

        return out;
    }

    private static DailyResult list_Daily_value_NEW(String[] fil, String datad1, String datad2, Db_Master db1) {

        if (datad1 != null && datad2 != null) {

            try {
                ArrayList<NC_causal> nc_caus = db1.query_nc_causal_filial(fil[0], null);

                double setPurchTotal = 0.0;
                double setSalesTotal = 0.0;
                double setCashAdNetTot = 0.0;
                double refund = 0.0;
                //refund
                String sql0 = "SELECT value FROM ch_transaction_refund where status = '1' and method = 'BR' and branch_cod = '" + fil[0] + "'";
                sql0 = sql0 + "AND dt_refund >= '" + datad1 + ":00' ";
                sql0 = sql0 + "AND dt_refund <= '" + datad2 + ":59' ";
                ResultSet rs0 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql0);
                while (rs0.next()) {
                    refund = refund + fd(rs0.getString("value"));
                }
                //TRANSACTION
                String sql = "SELECT tr1.tipotr,cod,tr1.pay,tr1.localfigures,tr1.pos FROM ch_transaction tr1 WHERE tr1.del_fg='0' AND tr1.filiale = '" + fil[0] + "' ";
                sql = sql + "AND tr1.data >= '" + datad1 + ":00' ";
                sql = sql + "AND tr1.data <= '" + datad2 + ":59' ";
//                System.out.println("tester.ReloadingDati.list_Daily_value_NEW() "+sql);
                ResultSet rs = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql);
                double poamount = 0.00;
                ArrayList<String[]> cc = db1.credit_card_enabled();
                ArrayList<String[]> bc = db1.list_bankAccount();
                ArrayList<DailyCOP> dclist = new ArrayList<>();
                for (int x = 0; x < cc.size(); x++) {
                    DailyCOP dc = new DailyCOP(cc.get(x)[1], cc.get(x)[0]);
                    dclist.add(dc);
                }
                ArrayList<DailyBank> listdb = new ArrayList<>();
                for (int x = 0; x < bc.size(); x++) {
                    DailyBank dc = new DailyBank(bc.get(x)[1], bc.get(x)[0]);
                    listdb.add(dc);
                }
                while (rs.next()) {
                    if (rs.getString("tr1.tipotr").equals("B")) {
                        ResultSet rsval = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery("SELECT supporto,net,pos FROM ch_transaction_valori WHERE cod_tr = '" + rs.getString("cod") + "'");
                        while (rsval.next()) {
                            if (rsval.getString("supporto").equals("04")) {//CASH ADVANCE
                                setCashAdNetTot = setCashAdNetTot + fd(rsval.getString("net"));
                            } else if (rsval.getString("supporto").equals("06")) {//CREDIT CARD
                                DailyCOP dc = DailyCOP.get_obj(dclist, rsval.getString("pos"));
                                if (dc != null) {
                                    poamount = poamount + fd(rsval.getString("net"));
                                }
                            } else if (rsval.getString("supporto").equals("07")) {// bancomat
                                DailyCOP dc = DailyCOP.get_obj(dclist, rsval.getString("pos"));
                                if (dc != null) {
                                    poamount = poamount + fd(rsval.getString("net"));
                                }
                            } else if (rsval.getString("supporto").equals("08")) {
                                DailyBank dc = DailyBank.get_obj(listdb, rsval.getString("pos"));
                                if (dc != null) {
                                    poamount = poamount + fd(rsval.getString("net"));
                                }
                            } else {
                                setPurchTotal = setPurchTotal + fd(rsval.getString("net"));
                            }
                        }

                    } else {
                        setSalesTotal = setSalesTotal + fd(rs.getString("tr1.pay"));
                        if (rs.getString("tr1.localfigures").equals("06")) {//CREDIT CARD
                            DailyCOP dc = DailyCOP.get_obj(dclist, rs.getString("tr1.pos"));
                            if (dc != null) {
                                poamount = poamount + fd(rs.getString("tr1.pay"));
                            }
                        } else if (rs.getString("tr1.localfigures").equals("07")) {// bancomat
                            DailyCOP dc = DailyCOP.get_obj(dclist, rs.getString("tr1.pos"));
                            if (dc != null) {
                                poamount = poamount + fd(rs.getString("tr1.pay"));
                            }
                        } else if (rs.getString("localfigures").equals("08")) {
                            DailyBank dc = DailyBank.get_obj(listdb, rs.getString("tr1.pos"));
                            if (dc != null) {
                                poamount = poamount + fd(rs.getString("tr1.pay"));
                            }
                        }
                    }
                }

                //NO CHANGE
                String sql1 = "SELECT causale_nc,supporto,total,pos,fg_inout,quantita FROM nc_transaction WHERE del_fg='0' AND filiale = '" + fil[0] + "' ";
                sql1 = sql1 + "AND data >= '" + datad1 + ":00' ";
                sql1 = sql1 + "AND data <= '" + datad2 + ":59' AND (supporto = 01 || supporto ='...') ";
                ResultSet rs1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql1);
                double setToLocalCurr = 0.00;
                double setFromLocalCurr = 0.00;
                while (rs1.next()) {
                    NC_causal nc2 = getNC_causal(nc_caus, rs1.getString("causale_nc"));
                    if (nc2 == null) {
                        continue;
                    }
                    if (rs1.getString("fg_inout").equals("1") || rs1.getString("fg_inout").equals("3")) {
                        setFromLocalCurr = setFromLocalCurr + fd(rs1.getString("total"));
                    } else {
                        if (!nc2.getNc_de().equals("14")) { //solo gli acquisti non vengono considerati
                            setToLocalCurr = setToLocalCurr + fd(rs1.getString("total"));
                        }
                    }

                }
                double totalnotesnochange = setToLocalCurr + setFromLocalCurr;

                double setBaPurchTransfNotes = 0.00;
                double setBaSalesTransfNotes = 0.00;
                double setBraPurchLocalCurr = 0.00;
                double setBraSalesLocalCurr = 0.00;

                //EXTERNAL TRANSFER
                String sql2 = "SELECT cod,fg_tofrom,fg_brba FROM et_change WHERE fg_annullato = '0' AND filiale = '" + fil[0] + "' ";
                sql2 = sql2 + "AND dt_it >= '" + datad1 + ":00' ";
                sql2 = sql2 + "AND dt_it <= '" + datad2 + ":59' ";
                ResultSet rs2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(sql2);
                while (rs2.next()) {
                    ResultSet rs2val = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery("SELECT ip_total FROM et_change_valori WHERE cod = '"
                            + rs2.getString("cod") + "' AND kind ='01' AND currency='EUR'");
                    if (rs2.getString("fg_tofrom").equals("T")) { //sales
                        if (rs2.getString("fg_brba").equals("BA")) { //BANK
                            while (rs2val.next()) {
                                setBaSalesTransfNotes = setBaSalesTransfNotes + fd(rs2val.getString("ip_total"));
                            }
                        } else {//BRANCH
                            while (rs2val.next()) {
                                setBraSalesLocalCurr = setBraSalesLocalCurr + fd(rs2val.getString("ip_total"));
                            }
                        }
                    } else if (rs2.getString("fg_brba").equals("BA")) { //BANK
                        while (rs2val.next()) {
                            setBaPurchTransfNotes = setBaPurchTransfNotes + fd(rs2val.getString("ip_total"));
                        }
                    } else {
                        while (rs2val.next()) {
                            setBraPurchLocalCurr = setBraPurchLocalCurr + fd(rs2val.getString("ip_total"));
                        }
                    }
                }

                double setLastCashOnPrem = 0.0;
                double OFP_FX = 0.0;
                double setFx = 0.0;

                String osp_codice = "";

                ArrayList<Office_sp> li = db1.list_query_officesp2(fil[0], subDays(datad1.substring(0, 10), patternsql, 1));

                if (!li.isEmpty()) {
                    setLastCashOnPrem = fd(li.get(0).getTotal_cod());
                    // setFx = fd(li.get(0).getTotal_fx());
                    Office_sp o = db1.list_query_last_officesp(fil[0], datad2);
                    if (o != null) {
                        double[] d1 = db1.list_dettagliotransazioni(fil, o.getData(), datad2, "EUR");
                        osp_codice = o.getCodice();
                        OFP_FX = fd(o.getTotal_fx());
                        setFx = OFP_FX + d1[1];
                    }
                }

                double setCashOnPremFromTrans
                        = setSalesTotal //sell
                        - setPurchTotal //buy
                        + setBaPurchTransfNotes //bank
                        - setBaSalesTransfNotes //bank
                        + setBraPurchLocalCurr //branch
                        - setBraSalesLocalCurr //branch
                        + totalnotesnochange //nochange
                        + setLastCashOnPrem
                        - refund
                        - setCashAdNetTot
                        - poamount;

                //System.out.println("tester.ReloadingDati.list_Daily_value_NEW() "+setSalesTotal);
                //System.out.println("tester.ReloadingDati.list_Daily_value_NEW() "+setPurchTotal);
                double setCashOnPremError = 0.0;
                String qe = "SELECT total_user,total_system FROM oc_errors where filiale = '"
                        + fil[0] + "' AND cod IN "
                        + "(SELECT cod FROM oc_lista where data like '" + datad1.substring(0, 10) + "%' AND errors='Y') "
                        + "AND tipo='CH' AND kind='01' AND valuta = 'EUR'";
//                System.out.println(qe);

                ResultSet rs10 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE).executeQuery(qe);
                while (rs10.next()) {
                    double eurerr = fd(rs10.getString("total_user")) - fd(rs10.getString("total_system"));
                    setCashOnPremError = setCashOnPremError + eurerr;
                }
                double setCashOnPrem = setCashOnPremFromTrans + setCashOnPremError;

                Double[] d1 = {setLastCashOnPrem, setCashOnPrem, setFx, OFP_FX};
                DailyResult dr = new DailyResult(d1, osp_codice);

                return dr;
            } catch (Exception e) {
                log.severe("METHOD: " + ExceptionUtils.getRootCause(e).getStackTrace()[0].getMethodName());
                log.severe("ERROR: " + ExceptionUtils.getStackTrace(e));
            }
        }
        return null;
    }

    private static NC_causal getNC_causal(ArrayList<NC_causal> li, String nc_code) {
        for (int i = 0; i < li.size(); i++) {
            if (li.get(i).getCausale_nc().equals(nc_code)) {
                return li.get(i);
            }
        }
        return null;
    }

    private static String subDays(String start, String pattern, int days) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
        DateTime dt = formatter.parseDateTime(start);
        return dt.minusDays(days).toString(pattern);
    }

    private static String generaId(int length) {
        String random = RandomStringUtils.randomAlphanumeric(length - 15).trim();
        return new DateTime().toString("yyMMddHHmmssSSS") + random;
    }
}

class Dati {

    String FILIALE, DATA;
    double DAY_LCP, BR_ST_IN, OF_ST_PR, DAY_COP, DAY_FX, OFP_FX;
    String CODICE_OFP;
    DateTime now;

    public Dati(String FILIALE, String DATA, DateTime now, double DAY_LCP, double BR_ST_IN, double OF_ST_PR, double DAY_COP, double DAY_FX,
            String CODICE_OFP, double OFP_FX) {
        this.FILIALE = FILIALE;
        this.DATA = DATA;
        this.DAY_LCP = DAY_LCP;
        this.BR_ST_IN = BR_ST_IN;
        this.OF_ST_PR = OF_ST_PR;
        this.DAY_COP = DAY_COP;
        this.DAY_FX = DAY_FX;
        this.CODICE_OFP = CODICE_OFP;
        this.OFP_FX = OFP_FX;
        this.now = now;
    }

    public double getOFP_FX() {
        return OFP_FX;
    }

    public void setOFP_FX(double OFP_FX) {
        this.OFP_FX = OFP_FX;
    }

    public String getCODICE_OFP() {
        return CODICE_OFP;
    }

    public void setCODICE_OFP(String CODICE_OFP) {
        this.CODICE_OFP = CODICE_OFP;
    }

    public String getFILIALE() {
        return FILIALE;
    }

    public void setFILIALE(String FILIALE) {
        this.FILIALE = FILIALE;
    }

    public String getDATA() {
        return DATA;
    }

    public void setDATA(String DATA) {
        this.DATA = DATA;
    }

    public double getDAY_LCP() {
        return DAY_LCP;
    }

    public void setDAY_LCP(double DAY_LCP) {
        this.DAY_LCP = DAY_LCP;
    }

    public double getBR_ST_IN() {
        return BR_ST_IN;
    }

    public void setBR_ST_IN(double BR_ST_IN) {
        this.BR_ST_IN = BR_ST_IN;
    }

    public double getOF_ST_PR() {
        return OF_ST_PR;
    }

    public void setOF_ST_PR(double OF_ST_PR) {
        this.OF_ST_PR = OF_ST_PR;
    }

    public double getDAY_COP() {
        return DAY_COP;
    }

    public void setDAY_COP(double DAY_COP) {
        this.DAY_COP = DAY_COP;
    }

    public DateTime getNow() {
        return now;
    }

    public void setNow(DateTime now) {
        this.now = now;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

    public double getDAY_FX() {
        return DAY_FX;
    }

    public void setDAY_FX(double DAY_FX) {
        this.DAY_FX = DAY_FX;
    }

}

class DailyResult {

    Double[] valori;
    String CODICE_OFP;

    public DailyResult(Double[] valori, String CODICE_OFP) {
        this.valori = valori;
        this.CODICE_OFP = CODICE_OFP;
    }

    public Double[] getValori() {
        return valori;
    }

    public void setValori(Double[] valori) {
        this.valori = valori;
    }

    public String getCODICE_OFP() {
        return CODICE_OFP;
    }

    public void setCODICE_OFP(String CODICE_OFP) {
        this.CODICE_OFP = CODICE_OFP;
    }

}
