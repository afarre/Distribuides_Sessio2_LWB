package socket;

import com.google.gson.Gson;
import model.LamportRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class SingleNonBlocking extends Thread{
    /** Classes necesaries per als NIO sockets **/
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final SocketChannel socketChannel;

    /** Variables per al control de la comunicacio **/
    private String responder;
    private boolean response;

    /** Constants per al algoritme de lamport/agrawala **/
    private final static String LAMPORT_REQUEST = "LamportRequest";
    private final static String RESPONSE_REQUEST = "ResponseRequest";
    private final static String REMOVE_REQUEST = "RemoveRequest";

    /** Variables relacionades amb Lamport i la comunicacio entre classes **/
    private LamportRequest lamportRequest;
    private int clock;
    private final String process;
    private final int id;
    private final S_LWB s_lwb;

    public SingleNonBlocking(S_LWB s_lwb, int clock, int myPort, int brotherPort, int id, String process) throws IOException {
        this.s_lwb = s_lwb;
        this.clock = clock;
        this.process = process;
        this.id = id;
        lamportRequest = new LamportRequest(clock, process, id);

        // Creo el servidor
        InetAddress host = InetAddress.getByName("localhost");
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(host, myPort));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // Creo el primer client
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("localhost"), brotherPort);
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(addr);
        socketChannel.register(selector, SelectionKey.OP_CONNECT |SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        response = false;
        System.out.println("Client i servidors generats.");
    }


    @Override
    public void run() {
        try {
            while (true){
                // Esperem a que s'activi algun flag d'alguna key del selector
                selector.select(500);

                // Iterem sobre aquelles keys que tenen algun flag activat
                Iterator iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()){
                    SelectionKey key = (SelectionKey)iterator.next();
                    // Eliminem la key del Set per evitar queryejar-la quan ja no tingui cap flag activat
                    iterator.remove();

                    // Acceptable: Flag atribuit nomes al servidor (ServerSocketChannel) per a conexions entrants.
                    if (key.isAcceptable()){
                        SocketChannel sc = serverSocketChannel.accept();
                        sc.configureBlocking(false);
                        sc.register(selector, SelectionKey.OP_READ);
                        key.attach(sc.getRemoteAddress());
                    }

                    // Connectable: Key atribuida a clients, on es valida la conexio.
                    if (key.isConnectable()){
                        Boolean connected = processConnect(key);
                        key.attach("CLIENT: " + ((SocketChannel)key.channel()).getLocalAddress());
                        if (!connected) {
                            return;
                        }
                    }

                    // Readable: La key te algun missatge per a ser llegit
                    if (key.isReadable()){
                        SocketChannel sc = (SocketChannel) key.channel();
                        ByteBuffer bb = ByteBuffer.allocate(1024);
                        bb.clear();
                        sc.read(bb);
                        String result = new String(bb.array()).trim();
                        System.out.println("Result contains: " + result);

                        // En base al missatge llegit, ens comportem/contestem d'una forma o una altra
                        if (result.contains(RESPONSE_REQUEST) && result.contains(LAMPORT_REQUEST)) {
                            String[] aux = result.split(LAMPORT_REQUEST);
                            Gson gson = new Gson();
                            LamportRequest lamportRequest = gson.fromJson(aux[0].replace(RESPONSE_REQUEST, ""), LamportRequest.class);
                            assignResponder(lamportRequest.getProcess(), RESPONSE_REQUEST);
                            if (response){
                                done();
                            }

                            gson = new Gson();
                            lamportRequest = gson.fromJson(aux[1], LamportRequest.class);
                            assignResponder(lamportRequest.getProcess(), LAMPORT_REQUEST);
                            s_lwb.addRequest(lamportRequest);

                            ricartAgrawala(bb, sc, lamportRequest);

                        }else if (result.contains(RESPONSE_REQUEST)) {
                            Gson gson = new Gson();
                            LamportRequest lamportRequest = gson.fromJson(result.replace(RESPONSE_REQUEST, ""), LamportRequest.class);
                            assignResponder(lamportRequest.getProcess(), RESPONSE_REQUEST);
                            if (response){
                                done();
                            }

                        }else if(result.contains(REMOVE_REQUEST) && result.contains(LAMPORT_REQUEST)) {
                            String[] aux = result.split(LAMPORT_REQUEST);
                            Gson gson = new Gson();
                            LamportRequest lamportRequest = gson.fromJson(aux[0].replace(REMOVE_REQUEST, ""), LamportRequest.class);
                            s_lwb.removeQueueRequest(lamportRequest);
                            assignResponder(lamportRequest.getProcess(), REMOVE_REQUEST);

                            gson = new Gson();
                            lamportRequest = gson.fromJson(aux[1], LamportRequest.class);
                            assignResponder(lamportRequest.getProcess(), LAMPORT_REQUEST);
                            s_lwb.addRequest(lamportRequest);

                            ricartAgrawala(bb, sc, lamportRequest);

                        }else if (result.contains(LAMPORT_REQUEST)) {
                            Gson gson = new Gson();
                            LamportRequest lamportRequest = gson.fromJson(result.replace(LAMPORT_REQUEST, ""), LamportRequest.class);
                            assignResponder(lamportRequest.getProcess(), LAMPORT_REQUEST);
                            s_lwb.addRequest(lamportRequest);

                            ricartAgrawala(bb, sc, lamportRequest);

                        }else if (result.contains(REMOVE_REQUEST)){
                            Gson gson = new Gson();
                            LamportRequest lamportRequest = gson.fromJson(result.replace(REMOVE_REQUEST, ""), LamportRequest.class);
                            s_lwb.removeQueueRequest(lamportRequest);
                            assignResponder(lamportRequest.getProcess(), REMOVE_REQUEST);

                            s_lwb.queryPendingQueue();
                        }
                    }

                    // Writable: La key te el flag d'escriptura activat
                    if (key.isWritable()){
                        String msg = lamportRequest.toString();
                        SocketChannel sc = (SocketChannel) key.channel();
                        ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
                        sc.write(bb);
                        s_lwb.addRequest(lamportRequest);
                        sc.register(selector, SelectionKey.OP_READ);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ricartAgrawala(ByteBuffer bb, SocketChannel sc, LamportRequest lr) throws IOException {
        String result = null;
        if (lamportRequest.getClock() > lr.getClock()){
            //System.out.println("My clock is greater than the other's, so the other has priority. Answering.");
            result = lamportRequest.toString().replace(LAMPORT_REQUEST, RESPONSE_REQUEST);
            bb.clear();
            bb.put (result.getBytes());
            bb.flip();
            sc.write (bb);
        }else if (lamportRequest.getClock() == lr.getClock() && lamportRequest.getId() > lr.getId()){
            //System.out.println("My clock is equal but my ID is greater than the other's, so the other has priority. Answering.");
            result = lamportRequest.toString().replace(LAMPORT_REQUEST, RESPONSE_REQUEST);
            bb.clear();
            bb.put (result.getBytes());
            bb.flip();
            sc.write (bb);
        }else {
            //System.out.println("My clock is smaller than the other's. Adding to pending queue.");
            s_lwb.addPendingRequest(lr);
        }
    }

    private void assignResponder(String process, String ops) {
        if (responder == null){
            responder = process;
        }

        if (ops.equals(RESPONSE_REQUEST)){
            if (process.equals(responder)){
                response = true;
            }
        }
    }


    private void done() throws IOException {
        if (s_lwb.checkQueue()){
            s_lwb.useScreen();

            //Send remove
            String msg = lamportRequest.toString().replace(LAMPORT_REQUEST, REMOVE_REQUEST);
            ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
            socketChannel.write(bb);
            s_lwb.removeQueueRequest(lamportRequest);
            s_lwb.communicateDone(process);
            s_lwb.queryPendingQueue();

            //Send newly updated model.LamportRequest
            clock++;
            lamportRequest = new LamportRequest(clock, process, id);
            bb = ByteBuffer.wrap(lamportRequest.toString().getBytes());
            socketChannel.write(bb);
            s_lwb.addRequest(lamportRequest);
        }
    }

    public void answerPendingRequest(LamportRequest lr) {
        String msg = this.lamportRequest.toString().replace(LAMPORT_REQUEST, RESPONSE_REQUEST);
        ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
        bb.clear();
        bb.put (msg.getBytes());
        bb.flip();
        try {
            socketChannel.write(bb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Boolean processConnect(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            while (sc.isConnectionPending()) {
                sc.finishConnect();
            }
        } catch (IOException e) {
            key.cancel();
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
