import java.util.*;
import java.io.*;
import java.net.*;

//static
class Output{
    static PrintWriter out = null;

    public static void init(){
        FileWriter report = null;
        try{
            String file = "request"+Start.processNum+".log";
            report = new FileWriter(file, false);
        }
        catch(IOException h){

        }
       out = new PrintWriter(report);
    }

    public static void println(String s){
        out.println(s);
        System.out.println(s);
    }
    public static void close(){
        out.close();
    }
}


class SequenceVector {
    int[] v;
    int n;
    //have buffer for.

    SequenceVector(int n){
        this.n = n;
        v = new int[Start.clientNum()];
        for (int i = 0; i< Start.clientNum();i++)
            v[i] = 0;
    }

    public synchronized void update(String msg){
        String[] vecs = msg.split(" ");
        int[] vec = new int[Start.clientNum()+1];
        for(int i = 0; i < Start.clientNum()+1; i++)
            vec[i] = Integer.parseInt(vecs[i].trim());
        int k =  vec[Start.clientNum()] - 1;
        int accept = 1;
        if(vec[k] == v[k]+1){
            accept = 1;
        }
        else if (vec[k] > v[k]){
            accept = 2;//buffered
        }
        else{
            accept = 3; //reject
            Output.println("Message passed test condition 3" + "\nMessage rejected");
            return;
        }
        for(int i = 0; i < Start.clientNum(); i++){
            if (i != k && vec[i] > v[i]){
                Output.println("Message passed test condition 3" + "\nMessage rejected");
                return;
            }
        }
        if(accept == 1)
        Output.println("Message passed test condition "+accept + "\nMessage accepted");
        else
        Output.println("Message passed test condition "+accept + "\nMessage buffered");
        v[k] = vec[k];
        //Output.println("Message a/b/r");
    }

    public synchronized String incrementAndGetVector(){
        v[n-1] += 1;
        String s = "";
        for(int i = 0; i< Start.clientNum(); i++)
            s += v[i] + " ";
        return s;
    }
    public synchronized String getVector(){
        String s = "";
        for(int i = 0; i< Start.clientNum(); i++)
            s += v[i] + " ";
        return s;
    }
}

class VectorUpdateThread extends Thread {
    //blocking queue
    public void run(){
        //SequenceVector.update(msg);
        //print out and to file.
    }
}

class AckSender extends Thread {
    public AckSender(){}
    int k;

    public AckSender(String s){
        String[] d = s.split(" ");
        //pass it to thread to update.
        k = Integer.parseInt(d[Start.clientNum()].trim());
    }
    public static void send(int n){
        int port = Start.getPort(n);
        byte[] buffer = new byte[8192];
        try {
            InetAddress server = InetAddress.getByName(Start.getClient(n));
            DatagramSocket theSocket = new DatagramSocket();
            String theLine = Start.seq.incrementAndGetVector() + Start.processNum;
            byte[] data = theLine.getBytes("UTF-8");
            DatagramPacket theOutput = new DatagramPacket(data, data.length, server, port);
            theSocket.send(theOutput);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void run(){

        if(k != Start.processNum){
            send(k);
            //System.out.println("ack sent to "+d[Start.clientNum()]);
            //System.out.println(s);
        }
    }
}

class AckReciver extends Thread {

    int n;
    int port;
    public int i;
    //DatagramSocket server;

    public AckReciver(){
        n = Start.processNum;
        port = Start.getPort(n);
        i = 0;
        //try {server = new DatagramSocket(port);}
        //catch (Exception e){e.printStackTrace();}


    }

    public void recv(){
        byte[] buffer = new byte[8192];
        try {
            DatagramSocket server = new DatagramSocket(port);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (i < Start.clientNum() -1 ) {
                try {
                    server.receive(packet);
                    //String s = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    //System.out.println("ack recieved "+packet.getAddress() + " at port " + packet.getPort() + " says " + s);
                    //packet.setLength(buffer.length);
                    i++;
                }
                catch (Exception e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Interrupted via InterruptedIOException");
                    break;
                }
            }
            server.close();

            //System.out.println("All acks recieved");
        } // end try

        catch (Exception e) {
            e.printStackTrace();
        } // end cat//ch
    }
    //
    public void end(){


    }
    public  void run(){
        // recv ack //
        try{        recv();    } //
        catch (Exception e) {e.printStackTrace();}

    }
}
class Server extends Thread{
    int n;

    public Server(){
        this.n = Start.processNum;
    }

    public void setup(){
        (new AckReciver()).recv();
    }

    public void sleep(){
        try {Thread.sleep(Start.getSleepTime(Start.processNum));}
        catch (Exception e) {e.printStackTrace();}
    }

    public void send() {

        int port = Start.mcastPort();
        InetAddress ia = null;
        byte ttl = (byte)1;

        try                     { ia = InetAddress.getByName(Start.mcastAddress()); }
        catch (Exception e)    { e.printStackTrace(); }

        String theLine = Start.seq.incrementAndGetVector() + Start.processNum;
        byte[] data = theLine.getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ia, port);
        try {
            MulticastSocket ms = new MulticastSocket();
            ms.joinGroup(ia);
            ms.send(dp, ttl);
            //System.out.println("message sent");
            ms.leaveGroup(ia);
            ms.close();
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }
    public void run(){
        //send multicast
        int i = 0;
        while(i < Start.requestNum())
        {
            sleep();
            send();
            //AckReciver ack = new AckReciver();
            //ack.start();
            try {Thread.sleep(500); }
            catch (Exception e) {e.printStackTrace();}
            //if (ack.i == Start.clientNum()-1)
                i++;
            //ack.interrupt();
            //System.out.println("DoneServer " + i);
        }
        //System.out.println("DoneServer ");
    }
}

class Client extends Thread {
    int n;

    public Client(){
        n = Start.processNum;
    }

    public void recv(){

        int port = Start.mcastPort();
        //recv multicast;
        MulticastSocket ms = null;
        InetAddress group = null;

        try                 { group = InetAddress.getByName(Start.mcastAddress());}
        catch (Exception e) { e.printStackTrace(); }

        try {
            ms = new MulticastSocket(port);
            ms.joinGroup(group);
            byte[] buffer = new byte[8192];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            ms.receive(dp);
            String s = new String(dp.getData());
            //AckSender a = new AckSender(s);
            //a.start();
        String[] d = s.split(" ");
        //pass it to thread to update.
            int k = Integer.parseInt(d[Start.clientNum()].trim());
            String m = "";
            for(int i = 0; i < Start.clientNum(); i++)
                m += d[i] +  " ";
            if(k!=Start.processNum){
            Output.println("Process "+Start.processNum+" Recived from ID "+d[Start.clientNum()].trim());
            Output.println("Sequence Vector Recived "+m);
            int r = (int)(Math.random()*10);
            Output.println("Random number generated: " + r);
            if (r < Start.getProb(Start.processNum))
                System.out.println("Probability Test failed. Message lost");
            else{
                Output.println("Probability Test passed. Message not lost");
                Start.seq.update(s);
                Output.println("Current seq vector "+Start.seq.getVector());
            }
            }
            ms.leaveGroup(group);
            ms.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void run(){
        int i = 0;
        while(i < Start.requestNum()*Start.clientNum())
        {
            //System.out.println("In run client " + i);
            recv();
            i++;
        }

    }
}



public class Start{

    public static SequenceVector seq;
    public static Properties config = new Properties();
    public static int processNum;

    public static String getProperty(String property){
        return config.getProperty(property);
    }

    public static int getPort(int num){
        String x = "Client"+num+".port";
        return Integer.parseInt(config.getProperty(x).trim());
    }

    public static int getSleepTime(int num){
        String x = "Client"+num+".sleepTime";
        return Integer.parseInt(config.getProperty(x).trim());
    }

    public static int getProb(int num){
        String x = "Client"+num+".RejectProbability";
        return 0;//Integer.parseInt(config.getProperty(x).trim());
    }

    public static String getClient(int num){
        String x = "Client"+num;
        return config.getProperty(x);
    }

    public static String mcastAddress(){
        return config.getProperty("Multicast.address");
    }

    public static int mcastPort(){
        return Integer.parseInt(config.getProperty("Multicast.port").trim());
    }

    public static int clientNum(){
        return Integer.parseInt(config.getProperty("ClientNum").trim());
    }

    public static int requestNum(){
        return Integer.parseInt(config.getProperty("numberOfRequests"));
    }

    public static void main(String[] args){
        try {
            //load a properties file
            Start.config.load(new FileInputStream("system.properties"));
            //System.out.println(Start.mcastPort());
            //System.out.println(Start.mcastAddress());
            Start.processNum = Integer.parseInt(args[0]);
            Start.seq = new SequenceVector(Integer.parseInt(args[0]));
            Output.init();

            //get the property value and print it out
            for(int i = 1; i<5; i++){
                //System.out.println(Start.getPort(i));
                //System.out.println(Start.getClient(i));
                //System.out.println(Start.getProb(i));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Output.println("Process" + Start.processNum+" Started");
        System.out.println("Inital sequence Vector: "+Start.seq.getVector());

        AckSender acks = new AckSender();
        Client n = new Client();
        Server s = new Server();
        if(Start.processNum == 1){
            s.setup();
            s.send();
        }
        else{
            acks.send(1);
            n.recv();
        }
        n.start();
        s.start();

        try {s.join();}
        catch (Exception e) {e.printStackTrace();}
        try {n.join();}
        catch (Exception e) {e.printStackTrace();}
        Output.close();


    }
}