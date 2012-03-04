import java.io.*;
import java.util.*;


public class Start {
    public static long start_time = System.currentTimeMillis();
    public static PrintWriter out  = null;
    public static String runtimes = "";

    public static void main (String [] args)
    {
        Scanner s = new Scanner(System.in);
        try{
            s = new Scanner(new BufferedReader(new FileReader("system.properties")));
        }
        catch(IOException e){
            e.printStackTrace();
        }
        s.useDelimiter("\\n|=");
        s.next();
        int n=s.nextInt();
        s.next();
        int m=s.nextInt();
        int[] stime = new int[n];
        int[] otime = new int[n];
        int k = 0;
        while(s.hasNext())
        {
            s.next();
            stime[k] = s.nextInt();
            s.next();
            otime[k] = s.nextInt();
            k++;

        }

        FileWriter report = null;
        try{
            report = new FileWriter("output.log", false);
        }
        catch(IOException h){

        }
        FileWriter event = null;
        try{
            event = new FileWriter("event.log", false);
        }
        catch(IOException g){

        }
        Start.out = new PrintWriter(report);
        PrintWriter ev = new PrintWriter(event);
        Tree t = new Tree(n);
        TreeVisitor[] p = new TreeVisitor[n];
        String times = "";
        for(int i = 0; i < n; i++)
            p[i] = new TreeVisitor(n+i,m, t, stime[i], otime[i]);

        for(int i=0; i< n;i++)
            p[i].start();
        for(int i=0; i< n;i++){
            try{
                p[i].join();
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        ev.println("sequence:\n");
        ev.print(Start.runtimes);
        Start.out.close();
        ev.close();
    }
}

class TreeVisitor extends Thread {
    int node;
    Tree tree;
    int sleeptime;
    int optime;
    int m;

    TreeVisitor(int n, int m, Tree t, int s, int o){
        node      = n;
        this.m    = m;
        tree      = t;
        sleeptime = s;
        optime    = o;
    }


    void critical(){
        try{
            this.sleep(optime);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    void noncritical(){
        try{
            this.sleep(sleeptime);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public void run() {
        for (int i=0;i<m;i++){

            noncritical();
            tree.traverseUpFrom(node, i);
            critical();
            Start.out.print(tree);
            long now = System.currentTimeMillis();
            Start.out.println("UnixTime="+(now-Start.start_time));
            Start.runtimes = Start.runtimes + "p["+(node-tree.size()+1)+"-"+i+"]:"+(now-Start.start_time) + "\n";
            tree.traverseDownto(node/2);
        }
    }
}

class Semaphore{
    int sid;
    int acquired_process;
    int process_count;
    int waiting_process;
    int waiting_count;
    int mutex;
    Semaphore(int id){
        this.sid         = id;
        acquired_process = 0;
        waiting_process  = 0;
        waiting_count    = 0;
        process_count    = 0;
        mutex            = 1;
    }
    public String toString(){
        return  "(s"+sid+", "+mutex+", p["+acquired_process+","+process_count+"], p["+waiting_process+","+waiting_count + "])";
    }
    public synchronized void P(int id, int c){
        while(mutex == 0){
            waiting_process = id;
            waiting_count = c;
            try{
                wait();
            }
            catch(InterruptedException e){
            }
            waiting_process = 0;
            waiting_count = 0;
        }
        acquired_process = id;
        process_count = c;
        mutex = 0;
    }
    public synchronized void V(){
        mutex = 1;
        acquired_process = 0;
        process_count = 0;
        notifyAll();
        try{
        this.wait(2);
        }catch(InterruptedException e){}
    }
}

class Tree {
    int size;
    Semaphore[] lock;

    Tree(int n){
        lock = new Semaphore[n];
        size = n ;
        for(int i=0; i<n; i++){
            lock[i] = new Semaphore(i);
        }
    }

    int size(){
        return size;
    }

    void traverseUpFrom(int n, int time){
        int i = n;
        //sleep
        while(i/2 != 0){
            lock[i/2].P(n-size+1, time);
            i = i/2;
        }
    }

    public String toString(){
        int j = 1, k=1;
        String s = "";
        for (int i = 1; i < size; i++){
            s += lock[i].toString();
            if(i == j){
                j += Math.pow(2, k);
                k++;
                s+= "\n";
            }
            else
                s+= "         ";

        }
        return s;
    }

    void traverseDownto(int n){
        if (n == 0)
            return;
        traverseDownto(n/2);
        lock[n].V();
    }
}