import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;


public class Site {
    public static BlockingQueue<String> q = new LinkedBlockingQueue<String>();
    public static PrintWriter output = null;
    public static void main (String [] args)
    {
        int node_number = 1;
        if(args.length > 0)
            node_number = Integer.parseInt(args[0]);

        HashMap<String, String> config = new HashMap<String, String>();
        config.put("num", Integer.toString(node_number));
        Scanner s = new Scanner(System.in);
        try{
            s = new Scanner(new BufferedReader(new FileReader("system.properties")));
        }
        catch(IOException e){
            e.printStackTrace();
        }
        s.useDelimiter("\\n|=");
        s.next();
        //int n=s.nextInt();
        config.put("n", s.next());
        s.next();
        //int m=s.nextInt();
        config.put("m", s.next());
        s.next();
        config.put("stime", s.next());
        //int[] stime = new int[n];
        //int[] otime = new int[n];
        int k = 1;
        while(s.hasNext())
        {
            s.next();
            config.put(Integer.toString(k), s.next());
            //stime[k] = s.nextInt();
            //s.next();
            //otime[k] = s.nextInt();
            k++;

        }

        if(node_number == 1){
            FileWriter report = null;
            try{
                report = new FileWriter("output.log", false);
            }
            catch(IOException h){

            }
            Site.output = new PrintWriter(report);
        }



        Node n = new Node(config);

        Site.output.close();

    }
}


class Node{
    // represents process
    public final static int DEFAULT_PORT = 5779;
    Server receiver;
    String parent = null;
    String left_child = null;
    String right_child = null;
    Clock c;
    int port = 0;
    int m;
    int n;
    int num;
    int stime;
    String leftHost;
    String RightHost;
    HashMap<String, String> config;

    public Node(HashMap<String, String> config){
        //intialize
        this.config = config;
        num = Integer.parseInt(config.get("num"));
        n = Integer.parseInt(config.get("n"));
        m = Integer.parseInt(config.get("m"));
        stime = Integer.parseInt(config.get("stime"));
        c = new Clock(this);
        if(!isRoot())
            parent = config.get(Integer.toString(num/2));
        if(!isLeaf()){
            left_child = config.get(Integer.toString(2*num));
            right_child = config.get(Integer.toString(2*num+1));
        }
        receiver = new Server(this);
        setupConnection();
    }

    public void setupConnection(){
        //setupServer();
        spawnChild(2*num);
        connectChild(2*num);
        spawnChild(2*num+1);
        connectChild(2*num+1);
        connectParent();
        runServer();
        c.start(); //runClockThread();
        try{
            receiver.join();
        }
        catch(Exception e){}

        //clock.join();
    }

    /*public Socket leftChild(){
      return new Socket(left_child, DEFAULT_PORT);
    }
    public Socket rightChild(){
    return new Socket(right_child, DEFAULT_PORT);
    }*/

    public void spawnChild(int x){
        if (x > n)
            return;
        //System.out.println("spawned child" + x);

        String child = config.get(Integer.toString(x));
        try{
            Runtime.getRuntime().exec("ssh "+child + " cd /cise/homes/gduvuru/;"+" java Site "+x);
        }
        catch(Exception e){

        }
    }

    public void connectChild(int x){
        //System.out.println("connected child" + x);
        if (isLeaf())
            return;
        Socket connection = null;
        try {
            connection = receiver.accept();
            InputStream is = connection.getInputStream();
            Scanner scanner = new Scanner(new BufferedInputStream(is));
            String line;
            try                 { line = scanner.nextLine(); }
            catch (Exception e) { line = null;               }
            connection.close();
            c.update(line);
            //return line;
        }
        catch (IOException ioe) {
            //System.err.println("Could not open " + socket);
            //return null;
            //sendClock(null);

        }

    }

    public void connectParent(){
        if (!isRoot())
        {
            //try {
            //Thread.sleep(10);
            //new Socket(parent, DEFAULT_PORT);
            sendParent("");
            //System.out.println("connect "+ parent);
            //}
            //catch (IOException ex) {}
            //catch (Exception c){}
        }
    }

    public void sendParent(String msg){
        sendNode(parent, msg);
    }

    public void sendRightChild(String msg){
        sendNode(right_child, msg);
    }

    public void sendLeftChild(String msg){
        sendNode(left_child, msg);
    }

    public void sendNode(String s, String msg){
        msg = c.toString();
        //send line code
        try {
            Socket connection = new Socket(s, DEFAULT_PORT);
            OutputStream os        = connection.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            PrintWriter out        = new PrintWriter(osw, true);
            out.write(msg);
            out.write("\r\n");
            out.flush();
            out.close();
            connection.close();
        }
        catch (IOException e) { e.printStackTrace(); } 
    }

    public void receiveNode(){
        try {
            Socket connection = receiver.accept();
            InputStream is = connection.getInputStream();
            Scanner scanner = new Scanner(new BufferedInputStream(is));
            String line;
            try                 { line = scanner.nextLine(); }
            catch (Exception e) { line = null;               }
            connection.close();
            sendClock(line);
            //return line;
        }
        catch (IOException ioe) {
            //System.err.println("Could not open " + socket);
            //return null;
            sendClock(null);

        }
    }


    public boolean isRoot(){
        return (num == 1);
    }

    public boolean isLeaf(){
        return (2*num > n);
    }

    public void runServer(){
        if(isRoot())
            message_start();
        receiver.start();
    }

    public void message_start(){
        sendLeftChild(c.toString());
        sendRightChild(c.toString());
    }

    public void sendClock(String msg){
        try{
            //System.out.println(msg);
            Site.q.put(msg);
        }
        catch(Exception e){}
    }




    public int getPort(){
        if(port != 0)
            return port;
        else
            return DEFAULT_PORT;
    }

}

class Server extends Thread {
    public ServerSocket server = null;
    Node node;
    public Server(Node n){
        node     = n;
        try {
            server = new ServerSocket(n.getPort());
        }
        catch (IOException ex) {
            System.err.println("Port not avialable");
        } // end catch

    }


    public Socket accept() throws IOException{
        return server.accept();
    }

    public  void run() {
        Socket connection = null;
        int i = 0;
        if(node.isRoot()){
            i++;
            Site.output.println("Increment rates:");
            for(int j = 1; j<=node.n; j++)
                //Site.output.println("P"+j +":"+node.c.aclocks[j]);
                Site.output.println("P"+j +":"+node.c.aclocks[j]);
            Site.output.println("");
            Site.output.println("");
        }
        while (i < node.m) {
            try {
                //if root print stats
                //
                // Recive from parent it not root
                //connection = server.accept();
                //System.out.println(i);
                if (node.isRoot())
                    try{
                        //System.out.println(node.stime);
                        sleep(node.stime);
                    }catch(Exception e){};
                node.receiveNode();
                //update_clock();
                //send to children
                //n.left_child.write(now.toString( ) +"\r\n");
                //n.left_child.write(now.toString( ) +"\r\n");
                node.sendLeftChild("lclock" + i);
                node.sendRightChild("rclock" + i);

                //recieve from left
                //connection = server.accept();
                node.receiveNode();
                //update_clock();
                //recieve from right
                //connection = server.accept();
                node.receiveNode();
                //update_clock();

                node.sendParent("pclock"+i);//n.parent.write(now.toString( ) +"\r\n");

                //out.flush( );
                //connection.close( );
                //Site.output.println(node.c.report());
                if(node.isRoot()){
                    Site.output.println("Clocks After run "+i+":");
                    Site.output.println(node.c.report());
                }
                i++;
            }
            finally {
                try {
                    if (connection != null) connection.close( );
                }
                catch (IOException ex) {}
            }
        }
        node.sendClock("quit");
        if(node.isRoot()){
            Site.output.println("Clocks After run "+i+":");
            Site.output.println(node.c.report());
        }
        // end while
    } // end try
} // end DaytimeServer

class Clock extends Thread{
    int[] bclocks;
    int[] aclocks;
    Node node;
    int n;
    int num;
    int clockSpeed;

    public Clock(Node n1){
        node = n1;
        n = n1.n;
        num = n1.num;
        int j = 1+(int)(Math.random()*10);
        bclocks = new int[n+1];
        aclocks = new int[n+1];
        for(int i = 1; i <= n; i++){
            aclocks[i] = 0;
            bclocks[i] = 0;
        }
        aclocks[num] = j;
        bclocks[num] = j;
        clockSpeed = j;
    }

    public synchronized void reinitializeClocks(){
        for(int i = 1; i <= n; i++)
            bclocks[i] = aclocks[i];
    }

    public synchronized void update(String msg){
        if (msg == null) return;
        String[] clock= msg.split(" ");
        for(int i = 1; i <=n ; i++){
            int sending = Integer.parseInt(clock[i]);
            if(sending != 0)
                aclocks[i] = sending;
        }
        if (num == 1)
            reinitializeClocks();
    }

    public synchronized void update_clock(String msg){
        aclocks[num] += clockSpeed;
        //System.out.println(msg);
        if (msg == null) return;
        String[] clock= msg.split(" ");
        int sending = Integer.parseInt(clock[n+1]);
        int x  = aclocks[num];
        if (sending > num){
            updateChildren(aclocks, clock , sending);
        }
        aclocks[num] = Math.max(x, Integer.parseInt(clock[sending])+1);
    }

    public void updateChildren(int[] a, String[] s, int y){

        a[y] = Integer.parseInt(s[y]);

        if (2*y < n){
            updateChildren(a, s, 2*y);
            updateChildren(a, s, 2*y+1);
        }

    }

    public synchronized String toString(){
        String s = "0 ";
        for(int i = 1; i <=n ; i++)
            s += aclocks[i]+" ";
        s += num;
        return s.trim();
    }

    public synchronized String report(){
        String s = "";
        for(int i = 1; i <= n; i++){
            s += "P" + i + " Updated:" + aclocks[i]+"\n";
            s += "P" + i + " Original:" + bclocks[i]+"\n";
        }
        reinitializeClocks();
        return s;
    }
    public void run(){
        while(true){
            try{
                String msg = Site.q.take();
                //System.out.println("taken");
                if(msg.trim().compareTo("quit") == 0)
                    break;
                this.update_clock(msg);
            }
            catch(InterruptedException e ){}
        }

    }


}


