package xarxa;

import model.LamportRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class X_LWB extends Thread {
    private static int INCOME_PORT;
    private static int OUTGOING_PORT;

    private DataOutputStream outcomeDoStream;
    private DataInputStream outcomeDiStream;

    private DataOutputStream incomeDoStream;
    private DataInputStream incomeDiStream;
    private Socket outSocket;

    private final static String LWA1 = "LWA1";
    private final static String LWA2 = "LWA2";
    private final static String LWA3 = "LWA3";

    /** Constants per al algoritme de lamport **/
    private final static String LAMPORT_REQUEST = "LamportRequest";
    private final static String RESPONSE_REQUEST = "ResponseRequest";
    private final static String REMOVE_REQUEST = "RemoveRequest";
    private final static String PORT = "PORT";
    private static final String WORK = "WORK";

    /** Dades de la peticio de lamport actual **/
    private final String process;
    private final int id;

    private final ArrayList<LamportRequest> lamportQueue;
    private LamportRequest lamportRequest;

    public X_LWB(String process, int myPort, int id, int xarxaPort){
        this.process = process;
        INCOME_PORT = myPort;
        OUTGOING_PORT = xarxaPort;
        this.id = id;
        lamportRequest = new LamportRequest(0, process, id);
        lamportQueue = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            createOutcomeConnection();
            outcomeDoStream.writeUTF(PORT);
            outcomeDoStream.writeInt(INCOME_PORT);
            outcomeDoStream.writeUTF(process);
            createIncomeConnection();
            System.out.println("Waiting for everyone to be connected...");

            while (true){
                String request = incomeDiStream.readUTF();
                readRequest(request);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createOutcomeConnection() {
        boolean wait = true;
        while (wait) {
            // Averiguem quina direccio IP hem d'utilitzar
            InetAddress iAddress;
            try {
                iAddress = InetAddress.getLocalHost();
                String IP = iAddress.getHostAddress();

                outSocket = new Socket(String.valueOf(IP), OUTGOING_PORT);
                outcomeDoStream = new DataOutputStream(outSocket.getOutputStream());
                outcomeDiStream = new DataInputStream(outSocket.getInputStream());
            } catch (ConnectException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outSocket != null) {
                    wait = false;
                }
            }
        }
    }

    private void createIncomeConnection() {
        try {
            //creem el nostre socket
            ServerSocket serverSocket = new ServerSocket(INCOME_PORT);
            //esperem a la conexio de la xarxa de comunicacions
            Socket incomeSocket = serverSocket.accept();
            incomeDiStream = new DataInputStream(incomeSocket.getInputStream());
            incomeDoStream = new DataOutputStream(incomeSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readRequest(String request) throws IOException {
        switch (request){
            case WORK:
                System.out.println("Got work in " + process);
                break;

            case LAMPORT_REQUEST:

                break;
            case RESPONSE_REQUEST:

                break;
            case REMOVE_REQUEST:

                break;
        }
    }
}
