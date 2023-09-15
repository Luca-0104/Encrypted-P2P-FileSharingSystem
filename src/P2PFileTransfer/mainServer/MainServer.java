package P2PFileTransfer.mainServer;

import P2PFileTransfer.Packet.Message;
import P2PFileTransfer.peer.PeerBean;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MainServer {

    //socket connection information
    public static final String MAIN_SERVER_IP = "127.0.0.1";
    public static final int MAIN_SERVER_PORT = 5000;

    // Universal Hashed Peer Table (UHPT) recording the connected peers
    private Map<String, PeerBean> uhpt = new HashMap<>();

    //IO
    private ObjectOutputStream oos;

    // The constructor
    public MainServer(){
        //initialize the server socket
        initServerSocket();
    }

    /**
     * Initializing the server socket
     */
    private void initServerSocket(){
        //create a server socket
        try {
            ServerSocket serverSocket = new ServerSocket(MAIN_SERVER_PORT);
            System.out.println("--- server started ---");

            // keep receiving clients, for each one, we will start a thread to handle with it
            while (true){
                // receive a client (peer)
                Socket socket = serverSocket.accept();
                System.out.println("--- a peer is connected ---");

                //init the output stream
                oos = new ObjectOutputStream(socket.getOutputStream());

                //start a new thread for this handling this client
                new Thread(new PeerHandler(socket, oos)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * (an inner class) For handling a client (peer), which is connected to the main server
     */
    class PeerHandler implements Runnable{
        // socket connection between the main server and a specific peer
        private Socket socket;

        // streams between the main server and a specific peer
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        // the port number for this peer
        private String port;

        // the GUID of this peer
        private String guid;

        //random for generating the port numbers, put it here can make all the port number unique
        private final Random random = new Random();

        // this is used to control the endless loop. if this peer leaves the session, this should be false
        private boolean isRunning = true;

        // The constructor
        public PeerHandler(Socket socket, ObjectOutputStream oos){
            // init the socket
            this.socket = socket;

            // get the output stream obj from the main server class
            this.oos = oos;

            // init the input stream of this socket connection
            try {
                ois = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // generate the GUID for this peer
            genGUID();
            // generate port number of this peer
            genPort();
        }

        /**
         * for generating the GUID
         */
        private void genGUID(){
            this.guid = UUID.randomUUID().toString();
        }

        /**
         * generate the port number for this peer
         */
        private void genPort(){
            int port = random.nextInt(1000) + 2000;
            this.port = String.valueOf(port);
        }

        /**
         * add this peer into the UHPT table
         */
        private void updateUHPT(PeerBean peerBean){
            // assign port for this peer
            peerBean.setPort(this.port);

            // record the oos (stream) to this peer
            peerBean.setOos(this.oos);

            // record this peer in the UHPT
            uhpt.put(guid, peerBean);
        }

        /**
         * Delete a row of a peer in the UHPT
         * @param pGUID the GUID of the peer
         */
        private void deletePeerFromUHPT(String pGUID){
            //remove the row of peer with this GUID in the UHPT hashmap
            uhpt.remove(pGUID);
        }

        /**
         * send the generated port number back to this peer
         */
        private void sendYourPort(){
            Message msg = new Message(Message.PORT_YOUR, port);
            try {
                oos.writeObject(msg);
                oos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * send the port number of the receiver peer to the sender peer
         * @param port port number of resource sharing peer
         */
        private void sendReceiverPort(String port){
            //send the port number of receiver peer to the sender peer
            Message msg = new Message(Message.PORT_RECEIVER, port);
            try {
                oos.writeObject(msg);
                oos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * create a user map according to the current UHPT
         * @return A hash map. key: guid, value: username
         */
        private Map<String, String> createUserMap(){
            // create the userMap (key: guid, value: username)
            Map<String, String> userMap = new HashMap<>();
            for(String key : uhpt.keySet()){
                PeerBean peerBean = uhpt.get(key);
                userMap.put(key, peerBean.getName());
            }
            return userMap;
        }

        /**
         * synchronize the user list at each peer according to the UHPT at main server.
         * The message contains a hashmap using the same format with the userMap at each peer.
         */
        private void syncPeerUserMap(){
            // create a user map according to the current UHPT
            Map<String, String> userMap = createUserMap();

            // create a new message containing the new userMap
            Message msg = new Message(Message.SYNC_USERS, userMap);

            // loop through the UHPT to get the oos to each peer
            // then send the msg using each of the oos
            for (String key : uhpt.keySet()){
                // get a online peer
                PeerBean peerBean = uhpt.get(key);
                // send msg to this peer
                try {
                    peerBean.getOos().writeObject(msg);
                    peerBean.getOos().flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }



        /**
         * read in the messages from peer,
         * and deal with different type message with different way
         */
        private void readInMessage(){
            try {
                //read in the message
                Message msg = (Message) ois.readObject();
                String type = msg.getType();

                //check the type of message
                if (type.equals(Message.CONNECT_REQUEST)){
                    /*
                        if the type is "connecting to the server",
                        the content must be a PeerBean obj representing this peer
                     */
                    PeerBean peerBean = (PeerBean) msg.getContent();

                    //add the new peer info into the UHPT (we must do this here)
                    updateUHPT(peerBean);

                    // tell the peer its port number
                    sendYourPort();

                    // tell all the peers to update their user lists
                    syncPeerUserMap();


                }else if (type.equals(Message.TRANSFER_REQUEST)){
                    /*
                        if the type is "TRANSFER_REQUEST", the content must be a string of the receiver's GUID
                     */

                    // get the receiver's GUID
                    String receiverGUID = (String) msg.getContent();

                    if (receiverGUID != null){
                        // get the receiver peer obj from the UHPT
                        PeerBean receiver = uhpt.get(receiverGUID);

                        // tell sender the port of the receiver, therefore, the sender can then connect to the receiver
                        sendReceiverPort(receiver.getPort());
                    }

                }else if (type.equals(Message.ACK_PEER_LEAVE)){
                    /*
                        if the type is "ack_peer_leave", this means this peer is leaving, we should update our UHPT
                     */
                    //delete the peer from the UHPT
                    deletePeerFromUHPT(guid);

                    //stop the endless loop for listening to this peer
                    this.isRunning = false;

                    // tell all the peers to update their user lists
                    syncPeerUserMap();

                }else if (type.equals(Message.SYNC_REQUEST_USER_LIST)){
                    /*
                        if the type is "SYNC_REQUEST_USER_LIST", this means this peer is asking for the latest user list
                     */
                    // create the current user map
                    Map<String, String> userMap = createUserMap();

                    // packet the userMap into a msg
                    Message msgBack = new Message(Message.SYNC_USERS, userMap);

                    // tell this peer the latest user list
                    oos.writeObject(msgBack);
                    oos.flush();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while(isRunning){
                //keep reading from input (listening to this peer)
                readInMessage();
            }
        }
    }

}
