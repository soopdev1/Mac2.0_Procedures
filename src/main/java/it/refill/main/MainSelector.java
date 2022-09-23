/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.main;

import static it.refill.cora.MacCORA.generaannuale;
import static it.refill.cora.MacCORA.generamensile;
import static it.refill.esolver.Atlante.rilasciaAtlante;
import static it.refill.esolver.ESolver.rilascia;
import static it.refill.gs.Client.invia2022;
import it.refill.gs.DatiInvio;
import it.refill.gs.Db_Master;
import it.refill.gs.Filiale;
import static it.refill.maintenance.Monitor.exe;
import it.refill.rilasciofile.GeneraFile;
import it.refill.rilasciofile.SftpSIA;
import it.refill.start.Central_Branch;
import static it.refill.start.Utility.createLog;
import static it.refill.start.Utility.pattern4;
import static it.refill.start.Utility.rb;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import static rc.soop.aggiornamenti.VerificaAggiornamenti.ultimo_Aggiornamento22;
import static rc.soop.aggiornamenti.VerificaAggiornamenti.verifica_Aggiornamenti22;
import static rc.soop.crm.Engine.recap_greenNumber;
import static rc.soop.crm.Engine.refresh_branch;
import static rc.soop.crm.Engine.set_expired_noshow_NEW;
import static rc.soop.crm.Engine.updateSpreadSito;

/**
 *
 * @author raf
 */
public class MainSelector {

    public static void main(String[] args) {

        int scelta;
        String repvalue;
        try {
            scelta = Integer.parseInt(args[0]);
            repvalue = args[1];
        } catch (Exception e) {
            scelta = 0;
            repvalue = "TCH";
        }

        switch (scelta) {
            case 1: //  ESOLVER
                rilascia();
                break;
            case 2: //  ATLANTE
                rilasciaAtlante();
                break;
            case 3: //  NEXI - FILE TRIMESTRALE ESOLVER
                new SftpSIA().rilasciaFIle(new File(args[1]), true);
                break;
            case 4: //  GRANDI STAZIONI
                Db_Master db1 = new Db_Master();
                List<Filiale> input = db1.getConfList();
                List<DatiInvio> dati = db1.query_datiinvio(input, new DateTime());
                db1.closeDB();
                invia2022(dati);
                break;
            case 5: //  RILASCIO FILE
                GeneraFile gf = new GeneraFile();
                gf.rilasciafile(gf, repvalue);
                break;
            case 6: //  CORA MENSILE
                generamensile();
                break;
            case 7: //  CORA ANNUALE
                generaannuale();
                break;
            case 8: //  ALLINEA CENTRALE
                break;
            case 9: // SITO ENGINE
                recap_greenNumber();
                refresh_branch();
                updateSpreadSito();
                break;
            case 10: // SITO VERIFICA SCADUTE
                set_expired_noshow_NEW();
                break;
            case 11: // AGGIORNAMENTI CENTRALE
                Logger log = createLog("Mac2.0_AGG_CENTRAL_" + "000", rb.getString("path.log"), pattern4);
                log.warning("START...");
                Central_Branch cb = new Central_Branch("000");
                if (!cb.updateCentral(log)) {
                    log.warning("AGGIORNAMENTO CENTRALE COMPLETATO");
                } else {
                    log.severe("ERRORE AGGIORNAMENTO CENTRALE");
                }
                break;
            case 12: //MONITOR
                exe();
                break;
            case 13: //VERIFICA AGGIORNAMENTI
                ultimo_Aggiornamento22();
                verifica_Aggiornamenti22();
                break;
            default:
                break;
        }

    }
}
