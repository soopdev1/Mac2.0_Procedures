/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.riallinea;

import static rc.soop.riallinea.Util.fd;
import static rc.soop.riallinea.Util.roundDoubleandFormat;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author rcosco
 */
public class AllineaOSP_last {

//    public static void main(String[] args) {
//
//        Db_Master db1 = new Db_Master();
//
//        List<Office_sp> lista_1 = db1.getofficeSP_valorinegativi();
//
//        lista_1.forEach(osp -> {
////            db1.getC()
//
//            double real = fd(osp.getTotal_grand()) - fd(osp.getTotal_fx());
//            String real_value = roundDoubleandFormat(real, 2);
//            String update = "UPDATE office_sp SET total_cod = '" + real_value + "' WHERE codice = '" + osp.getCodice() + "'";
//
//            try {
//                boolean es = db1.getC().createStatement().executeUpdate(update) > 0;
//
////            System.out.println("com.seta.mac_riallinea.AllineaOSP_last.main() "+osp.getTotal_fx());
//                System.out.println(real_value + " () " + osp.getTotal_cod() + " -- " + es);
////            System.out.println("com.seta.mac_riallinea.AllineaOSP_last.main() "+osp.getTotal_grand());
//            } catch (SQLException ex) {
//                ex.printStackTrace();
//            }
//
//        });
//        db1.closeDB();
//    }

}
