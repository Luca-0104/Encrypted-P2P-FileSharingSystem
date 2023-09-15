package P2PFileTransfer.peer;


import DESAlgorithm.cipherComponents.DecipherTool;
import DESAlgorithm.cipherComponents.EncipherTool;
import DESAlgorithm.cipherComponents.Key;
import DESAlgorithm.fileOperations.FileDecipherOperator;
import DESAlgorithm.fileOperations.FileEncipherOperator;
import P2PFileTransfer.Packet.Message;
import P2PFileTransfer.mainServer.MainServer;
import presentation.DownloadDirChoosingUI;
import presentation.PeerStarterUI;
import presentation.PeerUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Peer implements ActionListener {

    // the port of this peer
    private int myPort;

    // the user map recording the online users (this should always be synchronized with the UHPT at the main server)
    // key: uuid, value: username
    // the username should be unique in our system
    private Map<String, String> userMap = new HashMap<>();
    private boolean userMapInitialized = false; // we should make sure the user list is firstly initialized before advertising the connection

    // a Bean object encapsulating basic info of this peer (port, guid, ...)
    public PeerBean peerBean;

    // IO streams of this peer --> main server
    public ObjectOutputStream oos;
    private ObjectInputStream ois;

    // The user interfaces
    private PeerStarterUI peerStarterUI;
    private PeerUI peerUI;

    // info about the file being transmitted (regarding the sender peer)
    private File theFile;
    private String absolutFileName;
    // other cipher components
    private String keyString;
    private String receiverGUID;
    private String receiverName;


    // the constructor
    public Peer(){

        // start a new thread for initializing the client socket
        new Thread(new Runnable() {
            @Override
            public void run() {
                // initialize the client socket
                initClientSocket();
            }
        }).start();

        //initialize the GUI
        initGUI();
    }

    /**
     * Initialize the GUI for the peer portal
     */
    private void initGUI(){
        // new PeerUI()...
        // initialize the user interface and use this class as the action listener
        // therefore the actionPerformed method can be defined here and using the variables conveniently
        this.peerStarterUI = new PeerStarterUI(this);
    }

    /**
     * show a specific text on the JTextArea
     */
    private void showOnScreen(String text){
        // call the showOnScreen() method in the UI class
        this.peerUI.showMSG(text);
    }

    /**
     * Define the action after the buttons are clicked.
     * (Buttons in both the starter window and the main peer window)
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        // the join btn on the starter window
        if (e.getSource() == peerStarterUI.mJbJoin){

            /* Initialize the peerBean with the username */
            // check whether the user list is firstly gotten from the main server
            if (this.userMapInitialized){


                // check if the username already exist
                String username = peerStarterUI.mJtfUsername.getText();
                if (!userMap.containsValue(username)){
                    // initialize the peerBean using this username
                    this.peerBean = new PeerBean(peerStarterUI.mJtfUsername.getText());

                    // after connecting, the peer should advertise its info to the main server
                    // i.e. sending the peerBean obj to the main sever as the first message
                    advertiseConnection();

                    /* switch the window frame */
                    // initialize the peer interface
                    this.peerUI = new PeerUI(this);
                    // close the starting interface
                    this.peerStarterUI.dispose();

                    showOnScreen("--- connected to main server successfully ---");

                }else{
                    // username already exist, change to another one
                    JOptionPane.showMessageDialog(null, "The username already exists, please change to another one", "Username Already Exists", JOptionPane.ERROR_MESSAGE);
                }


            }else{
                JOptionPane.showMessageDialog(null, "User list is still synchronizing, try it again later", "User list is synchronizing", JOptionPane.WARNING_MESSAGE);
                System.out.println("initialization error");
            }


        // the select file btn on the peer ui
        }else if (e.getSource() == peerUI.mJbFileChosen){
            // make the file chooser visible
            int state = peerUI.mJfcFileChooser.showOpenDialog(peerUI);

            // if the "confirm" in the dialog is clicked
            if (state == JFileChooser.APPROVE_OPTION){
                // update the file obj
                this.theFile = peerUI.mJfcFileChooser.getSelectedFile();
                this.absolutFileName = theFile.getPath();
                // update the text field
                peerUI.mJtfFileChosen.setText(this.absolutFileName);
            }

        // the select receiver btn on the peer ui
        }else if (e.getSource() == peerUI.mJbReceiverChosen){

            // create an array of usernames
            String[] options = new String[userMap.values().size() - 1];
            int i = 0;
            for (String username : userMap.values()){
                // except this peer itself username
                if (!username.equals(this.peerBean.getName())){
                    options[i] = username;
                    i++;
                }
            }

            // a selection dialog
            String receiverName = (String) JOptionPane.showInputDialog(null,"Select the receiver of this transmission", "Receiver Selection", JOptionPane.PLAIN_MESSAGE, new ImageIcon("xxx.png"), options, "xxx");

            // show the username on the text field
            peerUI.mJtfReceiverChosen.setText(receiverName);

            // record the receiver name
            this.receiverName = receiverName;

        // the sending btn on the peer ui
        }else if (e.getSource() == peerUI.mJbConfirm){
            // check if all the required info are given
            if (peerUI.mJtfFileChosen.getText().isEmpty()){
                showOnScreen("You should choose a file!");

            }else if (peerUI.mJtfReceiverChosen.getText().isEmpty()){
                showOnScreen("You should choose receiver!");

            }else if (peerUI.mJtfKeyInput.getText().isEmpty()){
                showOnScreen("You should input a secret key!");

            }else{  // every thing is OK
                // disable the buttons
                peerUI.enableButtons(false);

                // update the key
                this.keyString = peerUI.mJtfKeyInput.getText();
                peerUI.mJtfKeyInput.setText("");

                // update the absolut file name
                this.absolutFileName = peerUI.mJtfFileChosen.getText();
                peerUI.mJtfFileChosen.setText("");

                // update the file chosen
                this.theFile = peerUI.mJfcFileChooser.getSelectedFile();

                // update the receiver guid
                String receiverName = peerUI.mJtfReceiverChosen.getText();
                for (String key : userMap.keySet()){
                    if (receiverName.equals(userMap.get(key))){
                        this.receiverGUID = key;
                    }
                }
                peerUI.mJtfReceiverChosen.setText("");

                // send TRANSFER_REQUEST to the main server
                sendTransferRequestToServer();
            }

        }
    }

    /**
     * Initialize the socket connection of this peer,
     * which means connecting to the main server.
     */
    private void initClientSocket(){

        try {
            //create a server socket
            Socket socket = new Socket(MainServer.MAIN_SERVER_IP, MainServer.MAIN_SERVER_PORT);
            System.out.println("--- connected to main server ---");

            // initialize the streams between this peer and the main server
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());

            // after connecting, the peer should ask for the user list immediately
            requestUserList();

            // keep reading input messages
            while (true){
                readInMessage();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tell the main server, we are going to send a file to a specific peer we selected.
     */
    private void sendTransferRequestToServer(){
        // the guid of the receiver should be contained in the msg
        Message msg = new Message(Message.TRANSFER_REQUEST, this.receiverGUID);
        try {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is to ask for synchronizing the user list with the main server.
     * The main server will send back the latest user list according to its UHPT.
     */
    private void requestUserList(){
        Message msg = new Message(Message.SYNC_REQUEST_USER_LIST, "sync_request");
        try {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tell the main server, this peer is leaving
     */
    public void sendACKPeerLeave(){
        Message msg = new Message(Message.ACK_PEER_LEAVE, "leave");
        try {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * After connecting, the peer should advertise itself to the server immediately
     * by sending the peerBean object to the main server, which represents itself
     */
    private void advertiseConnection(){
        try {
            // send the peerBean object to the server
            Message msg = new Message(Message.CONNECT_REQUEST, this.peerBean);
            oos.writeObject(msg);
            oos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read in the messages from client, and deal with different types of message
     */
    private void readInMessage(){
        try {
            // read in the message
            Message msg = (Message) ois.readObject();
            String type = msg.getType();

            // giving different responds according to the type
            if (type.equals(Message.PORT_YOUR)){
                /*
                    (Becoming a potential receiver)
                    If the type is "port_your", the content must be a String of port for this client to become a server.
                    This message can only be gotten a single time at the beginning of the connection to server.
                 */
                String port = (String) msg.getContent();
                // update the port info
                myPort = Integer.parseInt(port);
                peerBean.setPort(port);

                // Everytime connected to the server,
                // we let the peer start another thread to be a server (waiting for being a receiver) according to the port number,
                // waiting for another peer to connect with
                new Thread(new ReceivingHandler()).start();

            }else if (type.equals(Message.PORT_RECEIVER)){
                /*
                    (Becoming a sender connecting to the receiver)
                    if the type is "PORT_RECEIVER", the content must be a string of the port of the receiver peer
                 */
                String receiverPortStr = (String) msg.getContent();
                int receiverPort = Integer.parseInt(receiverPortStr);

                // start a new thread to connect to the receiver peer
                showOnScreen(">> NOTICE: connecting to the receiver peer...");
                new Thread(new SendingHandler(receiverPort)).start();

            }else if(type.equals(Message.SYNC_USERS)){
                /*
                      This is the msg from the mean server to tell every peer to update their user list
                      The msg content the latest UHPT of the main server
                */
                Map<String, String> newUserMap = (Map<String, String>) msg.getContent();
                // update the userMap using the new one
                this.userMap = newUserMap;
                // set the flag
                this.userMapInitialized = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * An inner class for becoming a sender to request transferring file to receiver peer
     * (a thread for connecting to the receiver peer, acting as a client)
     */
    class SendingHandler implements Runnable{

        // the socket of the request peer
        private Socket requestSocket;

        // the port number of receiver peer
        private int receiverPort;

        // IO streams between this peer and the receiver peer
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        // encryption components
        private FileEncipherOperator fileEncipherOperator;

        // this is used to control the endless loop. if the sharing done, this should be false
        private boolean isRunning = true;

        // calculate the time cost
        private long start;
        private long end;

        /**
         * as soon as the handler created we will do the following things
         */
        public SendingHandler(int receiverPort) {
            // init the port number
            this.receiverPort = receiverPort;

            try {
                // connect to the receiver (the receiver is acting as the socket server)
                this.requestSocket = new Socket(MainServer.MAIN_SERVER_IP, this.receiverPort);

                // initialize the IO streams between this peer and the receiver peer
                ois = new ObjectInputStream(requestSocket.getInputStream());
                oos = new ObjectOutputStream(requestSocket.getOutputStream());

                // screen notification
                showOnScreen(">> NOTICE: The connection with the receiver established successfully!");

            } catch (IOException e) {
                e.printStackTrace();
            }

            // send the file transfer request to the receiver
            System.out.println("--- sending request to the receiver peer ---");
            showOnScreen(">> NOTICE: sending request to the receiver peer...");
            sendTransferRequest();
        }

        /**
         * read in the messages from receiver peer, giving different responds by type
         */
        private void readInMessage(){
            try {
                // get the message and type
                Message msg = (Message) this.ois.readObject();
                String type = msg.getType();

                // giving different responds by message types
                if (type.equals(Message.ACK_ACCEPT_TRANSFER)){
                    /*
                        if the type is "ACK_ACCEPT_TRANSFER", this sender can start sending encrypted blocks
                    */
                    sendEncryptedDataBlocks();

                    // sending finished,
                    showOnScreen(">> NOTICE: File sent successfully!");

                    // enable the buttons again
                    peerUI.enableButtons(true);

                    // transmission done, stop listening to the receiver peer
                    isRunning = false;

                }else if (type.equals(Message.ACK_REJECT_TRANSFER)){
                    /*
                        if the type is "ACK_REJECT_TRANSFER", the transfer request is turned down by the receiver
                    */
                    // give the system notification
                    showOnScreen(">> NOTICE: Your transfer request has been rejected!");
                    // enable the buttons again
                    peerUI.enableButtons(true);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Break the file into blocks, then encapsulate each encrypted block into a message and send them to the receiver.
         * Message type should be ENCRYPTED_DATA_BLOCK
         */
        private void sendEncryptedDataBlocks(){
            // initialize the file encryption operator
            this.fileEncipherOperator = new FileEncipherOperator(theFile, new EncipherTool(new Key(keyString)), this.oos);
            showOnScreen(">> NOTICE: Start sending file...");

            // start encryption and file transmission
            this.fileEncipherOperator.encipherAndSendBlocks();
        }


        /**
         * Send the file transformation request to the receiver peer (Sender name + File name).
         * Tell the receiver that I am going to start an encrypted transmission to you.
         */
        private void sendTransferRequest(){
            // send the transfer request to the receiver peer, (0: sender name, 1: file name)
            Message msg = new Message(Message.TRANSFER_REQUEST_TO_RECEIVER, new String[]{peerBean.getName(), theFile.getName()});
            try {
                oos.writeObject(msg);
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * as soon as the thread created we will do the following things
         */
        @Override
        public void run() {
            // keep reading the msg from input stream until the sending is done
            while(isRunning){
                readInMessage();
            }
        }
    }

    /**
     * An inner class for becoming a server to receive resources from other peers
     * (a thread for waiting connections, acting as a server)
     */
    class ReceivingHandler implements Runnable{

        private ObjectOutputStream oosServerPeer;

        @Override
        public void run() {
            try {
                // initialize a server socket
                ServerSocket serverSocket = new ServerSocket(myPort);

                // keep receiving other peers (senders), for each one, we will start a thread to handle with it
                while(true){
                    // receiving a requesting peer
                    Socket socket = serverSocket.accept();

                    // init the output stream
                    /* Attention!! We must let the oos be initialized here and let ois be initialized inside the new thread, otherwise, the statement of getting the streams will be blocked */
                    oosServerPeer = new ObjectOutputStream(socket.getOutputStream());

                    // show notice on screen
                    showOnScreen(">> NOTICE: a sender peer connected");

                    // start a new thread to deal with this peer
                    new Thread(new SendingRequestHandler(socket, oosServerPeer)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * An inner class for dealing with the sending request
         * Define how to deal with each single sender peer
         * (a thread for dealing with a connection)
         */
        class SendingRequestHandler implements Runnable, ActionListener{

            //a socket of requesting peer
            private Socket requestPeerSocket;

            //IO streams with this peer
            private ObjectInputStream ois;
            private ObjectOutputStream oos;

            // The components used for decryption of the data between receiver and this sender
            private String keyStr;
            private String outputFileName; // (with dir)
            private String downloadDir; // (without filename)
            private FileDecipherOperator fileDecipherOperator;
            private BufferedOutputStream bos;   // used to write the download file

            // user interface used in this event
            private DownloadDirChoosingUI downloadDirChoosingUI;

            //this is used to control the endless loop. if the sharing done, this should be false
            private boolean isRunning = true;

            // the constructor
            public SendingRequestHandler(Socket requestPeerSocket, ObjectOutputStream oos) {
                //init the socket
                this.requestPeerSocket = requestPeerSocket;
                //init the output stream from parameter
                this.oos = oos;
                //init the input stream
                try {
                    this.ois = new ObjectInputStream(this.requestPeerSocket.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * read in the messages from the sender peer, and deal with different types of messages
             */
            private void readInMessage(){
                try {
                    //get the message and type
                    Message msg = (Message) this.ois.readObject();
                    String type = msg.getType();

                    if (type.equals(Message.TRANSFER_REQUEST_TO_RECEIVER)){
                        /*
                            if the type is "TRANSFER_REQUEST_TO_RECEIVER", the content must be a string representing the file name
                        */
                        // get the package of requesting
                        String senderName = ((String[]) msg.getContent())[0];
                        this.outputFileName = ((String[]) msg.getContent())[1];

                        // tell this receiver, a sender wants to send you a file:..., would you like to accept the transmission
                        int option = JOptionPane.showConfirmDialog(null, "User " + senderName + " wants to send you a file: " + this.outputFileName + ", do you accept it?", "File transmission request", JOptionPane.YES_NO_OPTION);

                        // if this receiver accepts,
                        if(option == JOptionPane.OK_OPTION){
                            // ask this receiver the secret key
                            this.keyStr = JOptionPane.showInputDialog(null, "Please enter the secret key of this transmission", "Secret key for decipher", JOptionPane.PLAIN_MESSAGE);
                            // check if the key is inputted
                            while(this.keyStr.isEmpty()){
                                // if not, we let the receiver input the key again
                                this.keyStr = JOptionPane.showInputDialog(null, "Please enter the secret key of this transmission", "Secret key for decipher", JOptionPane.PLAIN_MESSAGE);
                            }

                            // show another window to ask for the download dir
                            this.downloadDirChoosingUI = new DownloadDirChoosingUI(this);

                        // The receiver does not accept
                        }else {
                            // send a reject message back to the sender
                            sendRejectMessage();
                        }


                    }else if (type.equals(Message.ENCRYPTED_DATA_BLOCK)){
                        /*
                            if the type is "ENCRYPTED_DATA_BLOCK", the content must be a 64-bit block of encrypted data
                        */
                        // get the encrypted data block from message packet
                        byte[] cipherBlock = (byte[]) msg.getContent();

                        // decipher the data block and write it into the local place
                        downloadDataBlock(cipherBlock, false);

                    }else if (type.equals(Message.ENCRYPTED_DATA_BLOCK_FINAL)){
                        /*
                            if the type is "ENCRYPTED_DATA_BLOCK_FINAL", the content must be a 64-bit block of encrypted data
                            and this is the last block of data in the transmission.
                        */
                        // get the encrypted data block from message packet
                        byte[] cipherBlock = (byte[]) msg.getContent();

                        // decipher the data block and write it into the local place
                        downloadDataBlock(cipherBlock, true);

                        // transmission done, stop listening to the sender peer
                        isRunning = false;

                        showOnScreen(">> NOTICE: Download finished!");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * Send a message to tell the sender, I rejected your sending request.
             */
            private void sendRejectMessage(){
                Message msg = new Message(Message.ACK_REJECT_TRANSFER, "reject");
                try {
                    oos.writeObject(msg);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * For a cipher block received from the sender, we decipher it into the new data block,
             * then write this decrypted data block into the local place.
             * @param cipherBlock A 64-bit (8-byte) encrypted block received from the sender.
             * @param isFinal Whether this block is the last one of this file transmission.
             */
            private void downloadDataBlock(byte[] cipherBlock, boolean isFinal) throws IOException {
                fileDecipherOperator.decipherAndDownloadBlock(cipherBlock, isFinal, this.bos);
            }

            /**
             * Send the ACK of the transmission acceptation to the sender
             */
            private void sendACKAcceptTransfer(){
                Message msg = new Message(Message.ACK_ACCEPT_TRANSFER, "transfer accepted");
                try {
                    oos.writeObject(msg);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void run() {
                //keep listening the messages from sender peer, until finish the resource downloading
                while(isRunning){
                    readInMessage();
                }
            }

            /**
             * define the actions in the  downloadDirChoosingUI
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                // file selection btn
                if (e.getSource() == downloadDirChoosingUI.mJbDownloadDirChosen){
                    // make the file chooser visible
                    int state = downloadDirChoosingUI.mJfcDownloadDirChooser.showOpenDialog(downloadDirChoosingUI);

                    // if the "confirm" in the dialog is clicked
                    if (state == JFileChooser.APPROVE_OPTION){
                        // save the chosen directory
                        this.downloadDir = downloadDirChoosingUI.mJfcDownloadDirChooser.getSelectedFile().getAbsolutePath() + "\\";
                        // update the text field
                        downloadDirChoosingUI.mJtfDownloadDirChosen.setText(this.downloadDir);
                    }

                // confirm btn
                }else if (e.getSource() == downloadDirChoosingUI.mJbConfirm){
                    // concat the download dir and the filename to get the absolute filename
                    String absoluteFilename = this.downloadDir + "deciphered-" + this.outputFileName;
                    // close this dialog window
                    downloadDirChoosingUI.dispose();
                    // initialize the file decipher operator
                    this.fileDecipherOperator = new FileDecipherOperator(new DecipherTool(new Key(keyStr)), new File(absoluteFilename));
                    // initialize the file output stream to write in the download file
                    try {
                        this.bos = new BufferedOutputStream(new FileOutputStream(absoluteFilename));
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                    }
                    // then an ACK should be sent to the sender
                    sendACKAcceptTransfer();
                    showOnScreen(">> NOTICE: file download started: " + absoluteFilename);
                }
            }
        }
    }
}
