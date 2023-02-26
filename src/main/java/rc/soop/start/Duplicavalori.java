/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.start;

import rc.soop.maintenance.Db_Master;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import static rc.soop.start.Utility.rb;

/**
 *
 * @author rcosco
 */
public class Duplicavalori {

    private static final String hostIT = rb.getString("db.ip") + "/maccorp";
    //  private static final String hostCZ = rb.getString("db.ip") + "/maccorpczprod";
    //  private static final String hostUK = rb.getString("db.ip") + "/maccorpuk";

    public static void main(String[] args) {
//        ArrayList<String> listafiliali = new ArrayList<>();
        
//        DBHost dbc1 = new DBHost(hostIT);
//        ArrayList<String> listafiliali = dbc1.list_cod_branch_enabled();
//        dbc1.closeDB();

//        listafiliali.remove("000");
        ArrayList<String> listafiliali = new ArrayList<>();
        listafiliali.add("900");
//        listafiliali.add("198");
//        listafiliali.add("322");
//        listafiliali.add("319");
//        listafiliali.add("190");
//        listafiliali.add("191");
//        
        duplica(hostIT, listafiliali);
                        
//        listafiliali.forEach(br1 -> {
//
//            String ins = "INSERT INTO valute VALUES (null, '" + br1 + "', 'EGP', '', 'Egyptian Pound', '1', '0.00', '22.29970000', '', '0', '1', '1', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0');";
//            if (!br1.equals("000")) {
//                System.out.println(ins);
//
//                String dtoper = new DateTime().toString(Util.patternsqldate);
//                String dt_val = Util.formatStringtoStringDate(dtoper, Util.patternsqldate, Util.patternnormdate);
//
//                DBHost dbc = new DBHost(hostIT);
//
//                dbc.insert_aggiornamenti_mod(new Aggiornamenti_mod(
//                        Util.generaId(50), "000", dt_val, "0",
//                        "ST", ins, "setaser", dtoper));
//
//                dbc.insert_aggiornamenti_mod(new Aggiornamenti_mod(
//                        Util.generaId(50), br1, dt_val, "0",
//                        "ST", ins, "setaser", dtoper));
//
//                dbc.closeDB();
//
//            }
//
//        });

    }

    private static void duplica(String hostcentrale, ArrayList<String> listafiliali) {

        ArrayList<String> tabelle = new ArrayList<>();

        tabelle.add("commissione_fissa");
        tabelle.add("kind_commissione_fissa");
        tabelle.add("carte_credito");
        tabelle.add("nc_causali");
        tabelle.add("nc_causali_pay");
        tabelle.add("nc_tipologia");
        tabelle.add("rate_range");
        tabelle.add("supporti");
        tabelle.add("supporti_valuta");
        tabelle.add("till");
        tabelle.add("valute");
        tabelle.add("valute_tagli");

        listafiliali.forEach(nuovafiliale -> {
            if (!nuovafiliale.equals("000")) {
                tabelle.forEach(table -> {
                    try {
                        Db_Master dbc = new Db_Master(hostcentrale);
                        ResultSet rsold = dbc.getDatiPerFiliale(table, nuovafiliale);
                        if (rsold.next()) {
                            String deelte = "DELETE FROM " + table + " WHERE filiale = '" + nuovafiliale + "'";
                            dbc.getC().createStatement().execute(deelte);
                            System.out.println("mac2install.Duplicavalori.duplica() "+deelte);
                            Thread.sleep(1000);
                        }
                        ResultSet rs = dbc.getDatiPerFiliale(table);
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int numerocolonne = rsmd.getColumnCount();
                        String startva = "(";
                        String startpi = "(";
                        for (int x = 1; x <= numerocolonne; x++) {
                            if (!rsmd.getColumnName(x).equals("id")) {
                                startpi = startpi + "?,";
                                startva = startva + rsmd.getColumnName(x) + ",";
                            }
                        }
                        String fineva = startva.substring(0, startva.length() - 1) + ")";
                        String finepi = startpi.substring(0, startpi.length() - 1) + ")";
                        while (rs.next()) {
                            String ins = "INSERT INTO " + table + " " + fineva + " VALUES " + finepi;
                            PreparedStatement ps = dbc.getC().prepareStatement(ins);
                            int nc1 = 0;
                            for (int x = 1; x <= numerocolonne; x++) {
                                String name = rsmd.getColumnName(x);
                                if (!name.equals("id")) {
                                    nc1++;
                                    if (name.equalsIgnoreCase("filiale")) {
                                        ps.setString(nc1, nuovafiliale);
                                    } else {
                                        ps.setString(nc1, rs.getString(rsmd.getColumnName(x)));
                                    }
                                }
                            }
                            int x = ps.executeUpdate();
                            boolean esito = x > 0;
                            System.out.println(ps.toString() + " : " + esito);
                        }
                        dbc.closeDB();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });
    }

}
