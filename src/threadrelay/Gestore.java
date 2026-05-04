/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package threadrelay;

/**
 *
 * @author onorati.nicolo
 */
public class Gestore {

    private Staffetta st = new Staffetta();
    private Atleta a1, a2, a3, a4;
    private CampoDaCorsa campo; // ← aggiungi questo

    public Gestore(CampoDaCorsa campo) {
        this.campo = campo;
        int delay = campo.getDelayBySelection();  // ← leggi la velocità scelta
        a1 = new Atleta(st, 0, campo, delay);
        a2 = new Atleta(st, 1, campo, delay);
        a3 = new Atleta(st, 2, campo, delay);
        a4 = new Atleta(st, 3, campo, delay);
        a1.addObserver(campo);
        a2.addObserver(campo);
        a3.addObserver(campo);
        a4.addObserver(campo);
    }

    public void avviaGara() {
        a1.start();
        aspettaSoglia(a1, 90);
        if (campo.stopRequested) {
            return;  // ← aggiungi
        }
        a2.start();
        aspettaSoglia(a1, 100);
        aspettaSoglia(a2, 90);
        if (campo.stopRequested) {
            return;  // ← aggiungi
        }
        a3.start();
        aspettaSoglia(a2, 100);
        aspettaSoglia(a3, 90);
        if (campo.stopRequested) {
            return;  // ← aggiungi
        }
        a4.start();
        aspettaSoglia(a3, 100);
        aspettaSoglia(a4, 100);
    }

    public void aspettaSoglia(Atleta a, int soglia) {
        while (a.getMetriPercorsi() < soglia && !campo.stopRequested) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("Errore");
            }
        }
    }
}
