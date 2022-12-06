/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.riallinea;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class Filiali {

    public static void mainsingole() {
        String filiale = "040";
        List<Filiale> lista2 = new ArrayList<>();

        try {
            Db_Master db1 = new Db_Master();
            Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet rs2 = st1.executeQuery("SELECT * FROM maccorpita.dbfiliali WHERE filiale = '" + filiale + "'");
            if (rs2.next()) {
                lista2.add(new Filiale(rs2.getString("filiale"), rs2.getString("ip")));
            }
            rs2.close();
            st1.close();
            db1.closeDB();

            lista2.forEach(fil1 -> {
                //FASE1
                DateTime start = new DateTime(2018, 9, 3, 0, 0).withMillisOfDay(0);
                
                Db_Master fil = new Db_Master(true, fil1.getIp());
                if (fil.getC() != null) {
                    System.out.println(fil1.getCod() + " CONNESSA");
                    fil.closeDB();
                    ReloadingDati.fase1_dbfiliale(fil1.getCod(), start, fil1.getIp());
                } else {
                    System.out.println(fil1.getCod() + " ERRORE");
                }
                
                //FASE 2
//                 Db_Master fil = new Db_Master(true, fil1.getIp());
//                 if (fil.getC() != null) {
//                    System.out.println(fil1.getCod() + " CONNESSA");
//                    fil.closeDB();
//                    ReloadingDati.fase2_dbfiliale(fil1.getCod(), fil1.getIp());
//                } else {
//                    System.out.println(fil1.getCod() + " ERRORE");
//                }
                
                
                
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    public static void main(String[] args) {
//        mainsingole();
//    }
}

class Filiale {

    String cod, ip;

    public Filiale(String cod, String ip) {
        this.cod = cod;
        this.ip = ip;
    }

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
