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
    private Atleta a1 = new Atleta(st);
    private Atleta a2 = new Atleta(st);
    private Atleta a3 = new Atleta(st);
    private Atleta a4 = new Atleta(st);
    
    public void avviaGara(){
        a1.start();
        aspettaSoglia(a1, 90);
        
        a2.start();
        aspettaSoglia(a1, 100);
        aspettaSoglia(a2, 90);
        
        a3.start();
        aspettaSoglia(a2, 100);
        aspettaSoglia(a3, 90);
        
        a4.start();
        aspettaSoglia(a3, 100);
        aspettaSoglia(a4, 100);
        
    }
    
    public void aspettaSoglia(Atleta a, int soglia){
        while(a.getMetriPercorsi() < soglia){
            try{
                Thread.sleep(10);
            }
            catch(InterruptedException e){
                System.out.println("Errore");
            }
        }
    }
}
