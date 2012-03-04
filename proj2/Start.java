import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;


public class Start {
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

        try{
            Runtime.getRuntime().exec("ssh "+ config.get("1") + " cd /cise/homes/gduvuru/;"+" java Site");
        }
        catch(Exception e){

        }

        //Node n = new Node(config);
        //Start.output.close();

    }
}


