package esame_federico_marra_7025997_os_2023_06_20;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * @author Federico Marra 7025997
 * @exam Sistemi Operativi
 * @date 20/06/2021
 * @valutation 25
 */
public class Esame_federico_marra_7025997_os_2023_06_20 {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        // cambiati i parametri per il testing
        int N = 9;
        int M = 7;
        int X = 150;
        int L = 30;
        // aggiunti T e D che erano stati scordati
        int T = 230;
        int D = 70;

        Queue q = new Queue(L);
        OutputManager om = new OutputManager(M);

        // faccio partire i thread
        Generator[] g = new Generator[N];
        for (int i = 0; i < N; i++) {
            g[i] = new Generator(i, X, q);
            g[i].start();
        }
        Worker[] w = new Worker[M];
        for (int j = 0; j < M; j++) {
            w[j] = new Worker(j, N, T, D, q, om); // aggiunti T e D
            w[j].start();
        }
        OutputThread[] ot = new OutputThread[2];
        for (int k = 0; k < 2; k++) {
            ot[k] = new OutputThread(k, M, om);
            ot[k].start();
        }

        // aspetto 10 secondi
        Thread.sleep(10000);

        // interrompo i thread (mi ero dimenticato di scrivere gli interrupt)
        for (int i = 0; i < N; i++) {
            g[i].interrupt();
            g[i].join();
        }
        for (int j = 0; j < M; j++) {
            w[j].interrupt();
            w[j].join();
        }
        for (int k = 0; k < 2; k++) {
            ot[k].interrupt();
            ot[k].join();
        }

        // stampo i risultati
        System.out.println("\nResults:");
        int nMessTot = 0;
        for (int i = 0; i < N; i++) {
            System.out.println("G" + g[i].id + " generated: " + g[i].nMess + " messages");
            nMessTot += g[i].nMess;
        }
        System.out.println("Total messages generated: " + nMessTot + "\n");
        for (int j = 0; j < M; j++) {
            System.out.println("W" + w[j].id + " elaborated: " + w[j].nElab + " messages");
        }
        System.out.println();
        for (int k = 0; k < 2; k++) {
            System.out.println("OT" + ot[k].id + " printed: " + ot[k].nStampe + " times");
        }
    }

}

// aggiunta classe Msg (prima veniva passato solo il data)
class Msg {
    int id;
    int data;

    public Msg(int id, int data) {
        this.id = id;
        this.data = data;
    }

}


class Queue {
    ArrayList<Msg> q;   // cambiata il tipo di lista: int->Msg
    int L;
    // ho messo nel posto giusto i semafori
    Semaphore mutex = new Semaphore(1);
    Semaphore piene = new Semaphore(0);
    Semaphore vuote;

    public Queue(int L) {
        this.q = new ArrayList<>();
        this.L = L;
        this.vuote = new Semaphore(L);
    }

    public void add(Msg m) throws InterruptedException {        // aggiunto throws InterruptedException
        vuote.acquire();    // semafori spostati nel posto giusto
        mutex.acquire();
        q.add(m);           // tolta verifica di lista piena, perché la verifica è già svolta dall'acquire
        mutex.release();
        piene.release();
    }

    public Msg[] remove(int N) throws InterruptedException {  // è resa atomica l'operazione di getN al posto del get singolo
        piene.acquire(N);   // semafori spostati nel posto giusto analogamente all'add
        mutex.acquire();
        Msg[] ms = new Msg[N];  // si salvano le rimozioni in un array il tipo di array: int->Msg
        for (int i = 0; i < N; i++)
            ms[i] = q.remove(0);
        mutex.release();
        vuote.release(N);
        return ms;              // poi ritornato per riferimento
    }

}

class OutputManager {
    ArrayList<Integer> om;  // fatto unboxing: (cambiato il tipo di lista: int->Integer) e errata corrige "data"->"om"
    int M;

    // ho messo nel posto giusto i semafori
    public Semaphore mutexout = new Semaphore(1);
    Semaphore pieneout = new Semaphore(0);
    Semaphore[] vuoteout;

    public OutputManager(int M) {
        //this.om = new int[M];
        this.om = new ArrayList<>();
        this.M = M;
        // inizializzo i semafori per i dati elaborati dai worker
        this.vuoteout = new Semaphore[M];
        for (int j = 0; j < M; j++)
            this.vuoteout[j] = new Semaphore(1);
    }

    public void add(int value, int id) throws InterruptedException {
        vuoteout[id].acquire();             // semafori nel posto giusto
        mutexout.acquire();
        om.add(value);
        mutexout.release();
        pieneout.release();
    }

    public int[] remove(int M) throws InterruptedException { // è resa atomica l'operazione di getN al posto del get singolo
        pieneout.acquire(M);                  // semafori nel posto giusto
        mutexout.acquire();
        int[] data = new int[M];
        for (int j = 0; j < M; j++)
            data[j] = om.remove(0);    // tolta verifica di lista piena, perché la verifica è già svolta dall'acquire
        for (int j = 0; j < M; j++)
            vuoteout[j].release();            // rilascio il semaforo per il dato elaborato dal worker j-esimo
        mutexout.release();
        return data;                          // poi ritorna l'array dei dati per riferimento
    }
}

class Generator extends Thread {    // rimosso il throws
    int id;     // split delle declarations
    int value;
    int X;
    int nMess;
    Queue q;

    public Generator(int id, int X, Queue q) {  // specificato il public
        this.id = id;
        this.value = 0;
        this.X = X;
        this.nMess = 0;
        this.q = q;
        // tolto "return this;"
    }

    public void run() { // specificato il public
        try {
            while (true) {  // rimossi i semafori della queue
                this.value++;

                q.add(new Msg(this.id, this.value));    // aggiungo un messaggio, non solo il value

                this.nMess++;
                sleep(X);
            }
        } catch (InterruptedException e) {
            // System.out.println("G" + id + " interrotto");
        }
    }
}

class Worker extends Thread {       // rimosso il throws
    int id;
    int N;
    int nElab;
    int T;      // aggiunte declaration di T e D
    int D;
    Queue q;
    OutputManager om;

    public Worker(int id, int N, int T, int D, Queue q, OutputManager om) {     // specificato il public
        this.id = id;
        this.N = N;
        this.nElab = 0;
        this.T = T;    // aggiunte inizializzazioni di T e D
        this.D = D;
        this.q = q;
        this.om = om;
        // tolto "return this;"
    }

    public void run() { // specificato il public
        try {
            while (true) {  // rimossi i semafori della queue e dell'output manager
                Msg[] ms = q.remove(N); // salvato il risultato della remove in un array di messaggi

                int sum = 0;
                for (int i = 0; i < N; i++) {
                    sum += ms[i].data;  // sommo iterativamente il data del Msg i-esimo
                    nElab++;
                }

                // aspetto un tempo casuale tra T e T+D, aggiunto perché mancante
                sleep(T + (int) (Math.random() * D));

                om.add(sum, this.id);   // aggiunto il worker id per l'output manager
            }
        } catch (InterruptedException e) {
            // System.out.println("W" + id + " interrotto");
        }
    }

}

class OutputThread extends Thread {
    int id;     // split delle declarations
    int M;
    int nStampe;
    OutputManager om;

    public OutputThread(int id, int M, OutputManager om) {  // specificato il public
        this.id = id;
        this.M = M;
        this.nStampe = 0;
        this.om = om;
        // tolto "return this;"
    }

    public void run() { // specificato il public
        try {
            while (true) {      // rimossi i semafori dell'output manager
                int[] data = om.remove(M);  // salvato il risultato della remove in un array di interi

                System.out.print("OT" + this.id + ": [");
                for (int j = 0; j < M; j++) {   // cambiato k con j (uso poi k per il numero di OutputThread nel main)
                    System.out.print(data[j] + ((j!=M-1)?" ":""));
                }
                System.out.println("]");
                nStampe++;
            }
        } catch (InterruptedException e) {
            // System.out.println("OT" + id + " interrotto");
        }
    }

}