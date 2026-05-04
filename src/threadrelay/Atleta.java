/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package threadrelay;

import java.util.*;

/**
 *
 * @author onorati.nicolo
 */
public class Atleta extends Thread implements Subject {
    private Staffetta st;
    private int metriPercorsi=0;
    private int runnerIndex; 
    private final List<Observer> observers = new ArrayList<>();
    private CampoDaCorsa campo;
    private int delayMillis;
    
    public Atleta(Staffetta st, int runnerIndex, CampoDaCorsa campo, int delayMillis){
        this.st=st;
        this.runnerIndex=runnerIndex;
        this.campo=campo;
        this.delayMillis = delayMillis;
    }
    
    @Override
    public synchronized void addObserver(Observer o) {
        if (!observers.contains(o)) observers.add(o);
    }

    @Override
    public synchronized void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public synchronized void notifyObservers() {
        List<Observer> copia = new ArrayList<>(observers);
        for (Observer o : copia) {
            o.update(runnerIndex, metriPercorsi);
        }
    }

    @Override
    public void run(){
        while(metriPercorsi < 100){
        synchronized (campo.pauseLock) {
            while (campo.paused && !campo.stopRequested) {
                try { campo.pauseLock.wait(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
        if (campo.stopRequested) return;
        
        metriPercorsi++;
        notifyObservers();
        try{
            Thread.sleep(delayMillis);
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
