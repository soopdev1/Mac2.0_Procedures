/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.maintenance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static it.refill.maintenance.Db_Master.fd;
import static it.refill.maintenance.Db_Master.getControvalore;
import static it.refill.maintenance.Db_Master.roundDoubleandFormat;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class AllineaRealOsp {

    public static void main(String[] args) {
        String fil[] = {"307", "307"};

        //CENTRALE
        Db_Master db = new Db_Master();
//        Db_Master db = new Db_Master(true, false);
        
        List<IpFiliale> fi1 = db.getIpFiliale();
        boolean central_resp = main(db, fil);
        db.closeDB();
        System.out.println("macmonitor.AllineaRealOsp.main() " + central_resp);

        String ip = fi1.stream().filter(f1 -> f1.getFiliale().equals(fil[0])).map(f1 -> f1.getIp()).collect(Collectors.toList()).get(0);

        Db_Master filialdb = new Db_Master(true, ip);

        boolean filial_resp = main(filialdb, fil);

        filialdb.closeDB();

        System.out.println("macmonitor.AllineaRealOsp.main() " + filial_resp);

    }

    public static boolean main(Db_Master db, String fil[]) {
        Allineamento_Response resp = new Allineamento_Response();
        resp.setFil(fil);
        DateTime start = new DateTime().plusDays(1).withMillisOfDay(0);
        boolean dividi = db.get_national_office_changetype().equals("/");
        ArrayList<BranchStockInquiry_value> bsi = db.list_BranchStockInquiry_value(fil, start.toString(ProceduraDaily.patternsqlcomplete), "CH");
        ArrayList<OfficeStockPrice_value> osp = db.list_OfficeStockPrice_value(fil[0]);
        for (int i = 0; i < bsi.size(); i++) {
            for (int x = 0; x < osp.size(); x++) {
                if (bsi.get(i).getCurrency().equals(osp.get(x).getCurrency()) && osp.get(x).getSupporto().equals("01")) {
                    if (!bsi.get(i).getDati().get(0).toString().equals(osp.get(x).getQta())) {
                        
                        System.out.println("macmonitor.AllineaRealOsp.main1(" + i + ") " + bsi.get(i).getCurrency());
                        System.out.println("macmonitor.AllineaRealOsp.main1(CORRETTO) " + bsi.get(i).getDati().get(0).toString());
                        System.out.println("macmonitor.AllineaRealOsp.main2(ERRATO) " + osp.get(x).getQta());
                        String diff = roundDoubleandFormat(fd(bsi.get(i).getDati().get(0).toString()) - fd(osp.get(x).getQta()), 2);
                        System.out.println("macmonitor.AllineaRealOsp.main2(DIFF) " + diff);

                        if (fd(diff) > 0) {
                            String sql = "SELECT codice,rate FROM stock WHERE filiale='" + fil[0] + "' AND kind='01' "
                                    + "AND cod_value='" + bsi.get(i).getCurrency()+ "' AND idoperation LIKE 'ET%'"
                                    + "AND total = '0.00' ORDER by date DESC LIMIT 1";
                            try {
                                ResultSet rs = db.getC().createStatement().executeQuery(sql);
                                while (rs.next()) {
                                    String controval = roundDoubleandFormat(getControvalore(fd(diff), fd(rs.getString("rate")), dividi), 2);
                                    String upd = "UPDATE stock SET total = '" + diff + "', controval = '" + controval + "' WHERE codice = '" + rs.getString("codice") + "'";
                                    boolean ex = db.getC().createStatement().executeUpdate(upd) > 0;
                                    System.out.println("(ADD) " + upd + " : " + ex);
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                return false;
                            }
                        } else {
                            String sql = "SELECT * FROM stock WHERE filiale='" + fil[0] + "' AND kind='01' AND cod_value='" + bsi.get(i).getCurrency()
                                    + "' AND total <>'0.00' ORDER by date";
                            try {
                                double remove = -fd(diff);
                                System.out.println("macmonitor.AllineaRealOsp.main(REM) " + remove);
                                ResultSet rs = db.getC().createStatement().executeQuery(sql);
                                while (rs.next() && remove > 0) {
                                    if (remove >= fd(rs.getString("total"))) {
                                        String upd = "UPDATE stock SET filiale = '---' WHERE codice = '" + rs.getString("codice") + "'";
                                        boolean ex = db.getC().createStatement().executeUpdate(upd) > 0;
                                        System.out.println("(REM) " + upd + " : " + ex);
                                        remove = remove - fd(rs.getString("total"));
                                        System.out.println("(REM) " + remove);
                                    } else {
                                        String newtotal = roundDoubleandFormat(fd(rs.getString("total")) - remove, 2);
                                        String controval = roundDoubleandFormat(getControvalore(fd(newtotal), fd(rs.getString("rate")), dividi), 2);
                                        String upd = "UPDATE stock SET total = '" + newtotal + "', controval = '" + controval + "' "
                                                + "WHERE codice = '" + rs.getString("codice") + "'";
                                        boolean ex = db.getC().createStatement().executeUpdate(upd) > 0;
                                        System.out.println("(REM) " + upd + " : " + ex);
                                        remove = remove - (fd(rs.getString("total")) - fd(newtotal));
                                        System.out.println("(REM) " + remove);
                                    }
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

}

class Allineamento_Response {

    String fil[];

    boolean central, filiale;

    public String[] getFil() {
        return fil;
    }

    public void setFil(String[] fil) {
        this.fil = fil;
    }

    public boolean isCentral() {
        return central;
    }

    public void setCentral(boolean central) {
        this.central = central;
    }

    public boolean isFiliale() {
        return filiale;
    }

    public void setFiliale(boolean filiale) {
        this.filiale = filiale;
    }

}
