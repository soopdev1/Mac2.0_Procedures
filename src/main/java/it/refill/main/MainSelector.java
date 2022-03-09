/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.main;

import it.refill.esolver.Atlante;
import it.refill.esolver.ESolver;
import it.refill.rilasciofile.SftpSIA;
import java.io.File;

/**
 *
 * @author raf
 */
public class MainSelector {

    public static void main(String[] args) {

        int scelta;

        try {
            scelta = Integer.parseInt(args[0]);
        } catch (Exception e) {
            scelta = 0;
        }

        switch (scelta) {
            case 1: //ESOLVER
                ESolver.main(args);
                break;
            case 2: //ATLANTE
                Atlante.main(args);
                break;
            case 3: //NEXI - FILE TRIMESTRALE ESOLVER
                new SftpSIA().rilasciaFIle(new File(args[1]), true);
                break;
            default:
                break;
        }

    }
}
