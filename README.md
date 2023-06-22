# exam-os

![GitHub last commit](https://img.shields.io/github/last-commit/federico-marra/exam-os?style=flat-square)
![GitHub repo size](https://img.shields.io/github/repo-size/federico-marra/exam-os?style=flat-square)
![GitHub](https://img.shields.io/github/license/federico-marra/exam-os?style=flat-square)
![Java](https://img.shields.io/badge/Java-15.0.2-ED8B00?style=flat-square&logo=java)

Operative Systems exam

## Exercise

Si vuole realizzare il seguente sistema:

Sono presenti N thread Generator che generano ognuno un valore e inseriscono in una coda un messaggio con id del
generatore ed li valore quindi attendono per X millisecondi. La coda è limitata a L messaggi.

Sono presenti M thread Worker dove ognuno iterativamente preleva atomicamente N messaggi dalla coda li elabora in un
tempo variabile in [T,T+D) e inserisce il risultato nel OutputManager aspettando se il risultato precedente non e
stato acquisito.

Sono presenti due OutputThread dove ognuno preleva in modo atomico dal OutputManager gli M risultati prodotti dagli
Worker e li stampano.

Per facilitare il testing i Generator generano tutti la sequenza dei numeri interi partendo da 1 e gli Worker calcolano
la somma dei valori presenti negli N messaggi.

Il programma principale deve far partire i thread necessari e dopo 10 secondi terminare tutti i thread e stampare: per
ogni Generator il numero di messaggi generati ed il totale dei messaggi generati da tutti i Generator, per ogni Worker
li numero di elaborazioni fatte e per ogni OutputThread li numero di stampe fatte.

Realizzare in java li sistema descritto usando i **semafori** per la sincronizzazione tra i thread (sarà considerato
errore usare polling o attesa attiva).

## Solution

### import

```java

package esame_federico_marra_7025997_os_2023_06_20;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

```

### Main

```java

public class Esame_federico_marra_7025997_os_2023_06_20 {

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
            System.out.println("OT" + ot[k].id + " printed: " + ot[k].nStampe + " messages");
        }
    }

}

```

### Msg

```java

// aggiunta classe Msg (prima veniva passato solo il data)
class Msg {
    int id;
    int data;

    public Msg(int id, int data) {
        this.id = id;
        this.data = data;
    }

}

```

### Queue

```java

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

```

### OutputManager

```java

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

```

### Generator

```java


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

```

### Worker

```java

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

```

### OutputThread

```java

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

```

### Input for testing
```java
int N = 9;   // number of generators
int M = 7;   // number of workers
int X = 150; // ms in delay between each generation
int L = 30;  // length of the Queue
int T = 230; // ms in delay between each elaboration
int D = 70;  // max ms to add to the delay T
```
### Output
```
OT0: [9 18 27 36 45 54 63]
OT1: [72 81 90 99 108 117 126]
OT0: [135 144 153 162 171 180 189]
OT1: [198 207 216 225 234 243 252]
OT0: [261 270 279 288 297 306 315]
OT1: [324 333 342 351 360 369 378]
OT0: [387 396 405 414 423 432 441]
OT1: [450 459 468 477 486 495 504]
OT0: [513 522 531 540 549 558 567]

Results:
G0 generated: 66 messages
G1 generated: 66 messages
G2 generated: 66 messages
G3 generated: 66 messages
G4 generated: 66 messages
G5 generated: 66 messages
G6 generated: 66 messages
G7 generated: 66 messages
G8 generated: 66 messages
Total messages generated: 594

W0 elaborated: 90 messages
W1 elaborated: 90 messages
W2 elaborated: 90 messages
W3 elaborated: 81 messages
W4 elaborated: 81 messages
W5 elaborated: 81 messages
W6 elaborated: 81 messages

OT0 printed: 5 messages
OT1 printed: 4 messages

Process finished with exit code 0
