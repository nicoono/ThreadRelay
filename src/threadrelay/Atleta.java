/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package threadrelay;

/**
 *
 * @author onorati.nicolo
 */
public class Atleta extends Thread {
    private Staffetta st;
    private int metriPercorsi=0;
    
    public Atleta(Staffetta st){
        this.st=st;
    }
    
    @Override
    public void run(){
        while(metriPercorsi < 100){
            metriPercorsi++;
            try{
                Thread.sleep(50);
                System.out.println(metriPercorsi);
            }
            catch(InterruptedException e){
                System.out.println("Errore");
            }
        }  
    }

    public int getMetriPercorsi() {
        return metriPercorsi;
    }
    
    
}
