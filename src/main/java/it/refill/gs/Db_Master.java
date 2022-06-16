package it.refill.gs;

import static it.refill.gs.Config.categoria;
import static it.refill.gs.Config.codiceTenant;
import static it.refill.gs.Config.fd;
import static it.refill.gs.Config.log;
import static it.refill.gs.Config.patternD1;
import static it.refill.gs.Config.roundDoubleandFormat;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

public class Db_Master {

    public Connection c = null;

    public Db_Master() {
        try {
            String drivername = "org.mariadb.jdbc.Driver";
            String typedb = "mariadb";
            String user = "maccorp";
            String pwd = "M4cc0Rp";
            String host = "//machaproxy01.mactwo.loc:3306/maccorpita";
            Class.forName(drivername).newInstance();
            Properties p = new Properties();
            p.put("user", user);
            p.put("password", pwd);
            p.put("useUnicode", "true");
            p.put("characterEncoding", "UTF-8");
            p.put("useSSL", "false");
            this.c = DriverManager.getConnection("jdbc:" + typedb + ":" + host, p);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            log.log(Level.SEVERE, "{0} ERROR: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
            this.c = null;
        }
    }

    public Connection getC() {
        return c;
    }

    public void setC(Connection c) {
        this.c = c;
    }

    public void closeDB() {
        try {
            if (this.c != null) {
                this.c.close();
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "{0} ERROR: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
    }

    public ArrayList<DatiInvio> query_datiinvio_annuale(ArrayList<String[]> ing) {
        ArrayList<DatiInvio> out = new ArrayList<>();

        DateTime date = new DateTime().dayOfYear().withMinimumValue().withTimeAtStartOfDay();
        DateTime last = new DateTime().minusDays(1);

        for (int x = 0; x < ing.size(); x++) {
            String[] ingvalue = ing.get(x);
            String filiale = ingvalue[0];
            String codice = ingvalue[1];
            while (date.isBefore(last) || date.isEqual(last)) {
//                DateTime dt1 = dt.minusDays(31 - y);
                String datestart = date.toString(patternD1);
                Daily_value dv = list_Daily_value(filiale, datestart + " 00:00", datestart + " 23:59", true);
                double buy = fd(dv.getPurchGrossTot());
                double sel = fd(dv.getSalesGrossTot());
                double sco = fd(dv.getNoTransPurch()) + fd(dv.getNoTransSales());
                //query con filiale
                String lordo = roundDoubleandFormat(buy + sel, 2);
//                String netto = roundDoubleandFormat(buy + sel, 2);
                String scontrini = StringUtils.leftPad(roundDoubleandFormat(sco, 0), 3, "0");
                log.log(Level.INFO, "FILIALE {0} -  DATA {1} : LORDO {2} SC {3}", new Object[]{filiale, datestart, lordo, scontrini});
                DatiInvio di = new DatiInvio(codiceTenant, codice, datestart, categoria, lordo, lordo, scontrini);
                out.add(di);
                date = date.plusDays(1);
            }

        }

        System.out.println("com.seta.rest.Db_Master.query_datiinvio_annuale() " + date.toString(patternD1));
        System.out.println("com.seta.rest.Db_Master.query_datiinvio_annuale() " + last.toString(patternD1));

        return out;
    }

    public List<DatiInvio> query_datiinvio(List<Filiale> ing, DateTime dt1, DateTime dtEND) {

        dtEND = dtEND.withMillisOfDay(0);

        List<DatiInvio> out = new ArrayList<>();
        for (int x = 0; x < ing.size(); x++) {
            Filiale ingvalue = ing.get(x);
            DateTime dtSTART = dt1.withMillisOfDay(0);
            while (dtSTART.isBefore(dtEND)) {
                String datestart = dtSTART.toString(patternD1);
                Daily_value dv = list_Daily_value(ingvalue.getCod(), datestart + " 00:00", datestart + " 23:59", true);
                double buy = fd(dv.getPurchGrossTot());
                double sel = fd(dv.getSalesGrossTot());
                double sco = fd(dv.getNoTransPurch()) + fd(dv.getNoTransSales());
                //query con filiale
                String lordo = roundDoubleandFormat(buy + sel, 2);
                String scontrini = StringUtils.leftPad(roundDoubleandFormat(sco, 0), 3, "0");
//                log.log(Level.INFO, "FILIALE {0} -  DATA {1} : LORDO {2} SC {3}", new Object[]{ingvalue.getCod(), datestart, lordo, scontrini});
                DatiInvio di = new DatiInvio(codiceTenant, ingvalue.getContratto(), datestart, categoria, lordo, lordo, scontrini);
                out.add(di);
                dtSTART = dtSTART.plusDays(1);
            }
        }
        return out;
    }

    public List<DatiInvio> query_datiinvio(List<Filiale> ing, DateTime dt) {
        List<DatiInvio> out = new ArrayList<>();
        for (int x = 0; x < ing.size(); x++) {
            Filiale ingvalue = ing.get(x);
            for (int y = 0; y < 31; y++) {
                DateTime dt1 = dt.minusDays(31 - y);
                String datestart = dt1.toString(patternD1);
                Daily_value dv = list_Daily_value(ingvalue.getCod(), datestart + " 00:00", datestart + " 23:59", true);
                double buy = fd(dv.getPurchGrossTot());
                double sel = fd(dv.getSalesGrossTot());
                double sco = fd(dv.getNoTransPurch()) + fd(dv.getNoTransSales());
                //query con filiale
                String lordo = roundDoubleandFormat(buy + sel, 2);
//                String netto = roundDoubleandFormat(buy + sel, 2);
                String scontrini = StringUtils.leftPad(roundDoubleandFormat(sco, 0), 3, "0");
                log.log(Level.INFO, "FILIALE {0} -  DATA {1} : LORDO {2} SC {3}", new Object[]{ingvalue.getCod(), datestart, lordo, scontrini});
                DatiInvio di = new DatiInvio(codiceTenant, ingvalue.getContratto(), datestart, categoria, lordo, lordo, scontrini);
                out.add(di);
            }
        }
        return out;
    }

    public Daily_value list_Daily_value(String fil, String datad1, String datad2, boolean now) {

        if (datad1 != null && datad2 != null) {

            try {

                Daily_value d = new Daily_value();

                double setPurchTotal = 0.0;
                double setPurchComm = 0.0;
                double setSalesTotal = 0.0;
                double setSalesGrossTot = 0.0;

                //TRANSACTION
                String sql = "SELECT * FROM ch_transaction tr1 WHERE tr1.del_fg='0' AND tr1.filiale = '" + fil + "' ";

                sql = sql + "AND tr1.data >= '" + datad1 + ":00' ";

                sql = sql + "AND tr1.data <= '" + datad2 + ":59' ";

                sql = sql + " ORDER BY tr1.data";

                ResultSet rs = this.c.createStatement().executeQuery(sql);

                int setNoTransPurch = 0;
                int setNoTransSales = 0;

                while (rs.next()) {
                    if (rs.getString("tr1.tipotr").equals("B")) {
                        boolean ca = false;
                        ResultSet rsval = this.c.createStatement().executeQuery("SELECT * FROM ch_transaction_valori WHERE cod_tr = '" + rs.getString("cod") + "'");
                        while (rsval.next()) {
                            if (rsval.getString("supporto").equals("04")) {//CASH ADVANCE
                                ca = true;
                            } else if (rsval.getString("supporto").equals("06")) {//CREDIT CARD
                                ca = true;
                            } else if (rsval.getString("supporto").equals("07")) {// bancomat
                                ca = true;
                            } else if (rsval.getString("supporto").equals("08")) {
                                ca = true;
                            } else {
                                setPurchTotal = setPurchTotal + fd(rsval.getString("net"));
                                setPurchComm = setPurchComm + fd(rsval.getString("tot_com")) + fd(rsval.getString("roundvalue"));
                            }
                        }
                        if (!ca) {
                            setNoTransPurch++;
                        }
                    } else {
                        setNoTransSales++;
                        setSalesTotal = setSalesTotal + fd(rs.getString("tr1.pay"));
                        setSalesGrossTot = setSalesGrossTot + fd(rs.getString("tr1.total"));
                    }
                }
                d.setPurchGrossTot(roundDoubleandFormat(setPurchTotal + setPurchComm, 2));
                d.setSalesTotal(roundDoubleandFormat(setSalesTotal, 2));
                d.setSalesGrossTot(roundDoubleandFormat(setSalesGrossTot, 2));
                d.setNoTransPurch(String.valueOf(setNoTransPurch));
                d.setNoTransSales(String.valueOf(setNoTransSales));
                return d;
            } catch (SQLException | NumberFormatException ex) {
                log.log(Level.SEVERE, "{0} ERROR: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
            }
        }
        return null;
    }

}
