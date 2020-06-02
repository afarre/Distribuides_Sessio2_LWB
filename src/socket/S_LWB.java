package socket;

import model.LamportRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class S_LWB extends Thread {
    private DataInputStream diStreamHWB;
    private DataOutputStream doStreamHWB;

    private ArrayList<LamportRequest> queue;
    private ArrayList<LamportRequest> pendingQueue;

    private SingleNonBlocking singleNonBlocking;

    private String process;
    private int parentPort;
    private int myPort;
    private int brotherPort;
    private int id;
    private int clock;

    public S_LWB(String process, int parentPort, int myPort, int brotherPort, int id){
        this.process = process;
        this.parentPort = parentPort;
        this.myPort = myPort;
        this.brotherPort = brotherPort;
        this.id = id;
        queue = new ArrayList<>();
        pendingQueue = new ArrayList<>();
    }

    @Override
    public synchronized void run() {
        clock = 0;
        try {
            connectToParent();
            doStreamHWB.writeUTF("ONLINE");
            doStreamHWB.writeUTF(process);
            String msg = diStreamHWB.readUTF();
            if (msg.equals("CONNECT")){
                System.out.println("Setting up server with port: " + myPort);
                singleNonBlocking = new SingleNonBlocking(this, clock, myPort, brotherPort, id, process);
            }
            boolean flag = diStreamHWB.readBoolean();
            if (!flag){
                assert singleNonBlocking != null;
                singleNonBlocking.start();
            }
        } catch (ConnectException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void useScreen() {
        parentAllowance();
        for (int i = 0; i < 10; i++){
            System.out.println("\tSoc el procés lightweight " + process);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("-- Iteració acabada --\n");
    }

    private void parentAllowance() {
        try {
            doStreamHWB.writeUTF("RUN STATUS");
            boolean childsDone = diStreamHWB.readBoolean();
            if (childsDone){
                diStreamHWB.readUTF();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToParent() throws IOException {
        InetAddress iAddress = InetAddress.getLocalHost();
        String IP = iAddress.getHostAddress();

        System.out.println(process + " connecting to parent");
        Socket socketHWA = new Socket(String.valueOf(IP), parentPort);
        doStreamHWB = new DataOutputStream(socketHWA.getOutputStream());
        diStreamHWB = new DataInputStream(socketHWA.getInputStream());
    }

    public void addRequest(LamportRequest lamportRequest) {
        if (!queue.contains(lamportRequest)){
            queue.add(lamportRequest);
        }
    }

    public boolean checkQueue() {
        LamportRequest toBeExecuted = null;
        for (int i = 0; i < queue.size(); i++){
            toBeExecuted = queue.get(i);
            for (int j = 1; j < queue.size(); j++){
                if (queue.get(j).getClock() < toBeExecuted.getClock()){
                    toBeExecuted = queue.get(j);
                }else if (queue.get(j).getClock() == toBeExecuted.getClock() && queue.get(j).getId() < toBeExecuted.getId()){
                    toBeExecuted = queue.get(j);
                }
            }
            if (toBeExecuted.equals(queue.get(i))){
                break;
            }
        }
        return toBeExecuted.getProcess().equals(process);
    }

    public void removeQueueRequest(LamportRequest lamportRequest) {
        queue.remove(lamportRequest);
    }

    public void communicateDone(String process) throws IOException {
        doStreamHWB.writeUTF("LWB DONE");
        doStreamHWB.writeUTF(process);
    }

    public void addPendingRequest(LamportRequest lamportRequest) {
        pendingQueue.add(lamportRequest);
    }

    public void queryPendingQueue() {
        if (pendingQueue.size() > 0){
            for (LamportRequest lr : pendingQueue) {
                singleNonBlocking.answerPendingRequest(lr);
            }
            pendingQueue.clear();
        }
    }
}
