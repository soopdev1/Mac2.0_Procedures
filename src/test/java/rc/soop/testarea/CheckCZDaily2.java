/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rc.soop.testarea;

import java.sql.Statement;
import java.util.ArrayList;
import org.joda.time.DateTime;
import rc.soop.riallinea.BranchStockInquiry_value;
import rc.soop.riallinea.Db_Master;
import rc.soop.riallinea.OfficeStockPrice_value;
import rc.soop.riallinea.Office_sp;
import static rc.soop.riallinea.Util.fd;
import static rc.soop.riallinea.Util.patternsql;
import static rc.soop.riallinea.Util.roundDoubleandFormat;

/**
 *
 * @author Administrator
 */
public class CheckCZDaily2 {

    public static void main(String[] args) {

        String fil_cod = "307";
        DateTime start = new DateTime(2023, 3, 18, 0, 0);
        DateTime end = new DateTime(2023, 3, 29, 0, 0);

//        Db_Master db = new Db_Master(true, "192.168.9.19");
        Db_Master db = new Db_Master(true, false);

        while (start.isBefore(end)) {
            String stdate = start.toString(patternsql) + " 23:59:59";
            String fil[] = {fil_cod, fil_cod};
            ArrayList<BranchStockInquiry_value> dati = db.list_BranchStockInquiry_value(fil, stdate, "CH");
            if (!dati.isEmpty()) {
                Office_sp sp = db.list_query_officesp2(fil[0], stdate.substring(0, 10)).get(0);
                ArrayList<OfficeStockPrice_value> last = db.list_OfficeStockPrice_value(
                        sp.getCodice(), fil[0]);
                //1053398.00
                for (int i = 0; i < dati.size(); i++) {

//                    boolean czk = dati.get(i).getCurrency().equals("CZK");
                    String valuta = dati.get(i).getCurrency();
                    boolean czk = dati.get(i).getCurrency().equals(valuta);
                    
                    if (czk) {

                        OfficeStockPrice_value od = last.stream().filter(f1 -> f1.getCurrency().equals(valuta)).findAny().orElse(null);
                        if (od != null) {

                            
                            double q_ok = fd(String.valueOf(dati.get(i).getDati().get(0)));

                            double n_cv = q_ok * fd(od.getMedioacq());
                            
                            String upd1 = "UPDATE office_sp SET total_cod='" + dati.get(i).getDati().get(0) + "' WHERE codice='" + sp.getCodice() + "'";
//                            String upd2 = "UPDATE office_sp_valori SET quantity='" + dati.get(i).getDati().get(0) +
//                                    "',controv='" + dati.get(i).getDati().get(0) + "' WHERE cod='" + sp.getCodice() + "'";
                            
                            String upd2 = "UPDATE office_sp_valori SET quantity='" + roundDoubleandFormat(q_ok, 2) + "',controv='"
                                    + roundDoubleandFormat(n_cv, 2) + "' WHERE cod='" + sp.getCodice() + "' AND currency='" + valuta + "'";
                            try {
                                try (Statement st1 = db.getC().createStatement()) {
                                    st1.executeUpdate(upd1);
                                }
                                try (Statement st2 = db.getC().createStatement()) {
                                    st2.executeUpdate(upd2);
                                }

                                System.out.println(stdate + " (BSI) " + dati.get(i).getDati().get(0) + " OSP1 " + sp.getTotal_cod() + " OSP2 "
                                        + od.getQtaSenzaFormattazione() + "--" + od.getControvalore());

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
            start = start.plusDays(1);
        }
        db.closeDB();

    }
}
