package presentation;

import P2PFileTransfer.peer.Peer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class PeerUI extends JFrame {

    // message section
    public JTextArea mJtaMessageWindow;
    public JScrollPane mJspShowingPanel;   // contains mJtaMessageWindow

    // operation sections
    public JPanel mJpInputPanel;           // contains mJpKeyPanel, mJpFilePanel, mJpReceiverSelectPanel and mJpConfirmPanel
    public JPanel mJpKeyPanel;
    public JPanel mJpFilePanel;
    public JPanel mJpReceiverSelectPanel;
    public JPanel mJpConfirmPanel;

    // operation section - key section
    public JLabel mJlKey;
    public JTextField mJtfKeyInput;

    // operation section - file chosen section
    public JButton mJbFileChosen;
    public JFileChooser mJfcFileChooser;
    public JTextField mJtfFileChosen;

    // operation section - receiver selection section
    public JButton mJbReceiverChosen;
    public JTextField mJtfReceiverChosen;

    // operation section - confirm section
    public JButton mJbConfirm;


    // The action listener
    private Peer peer;

    // The constructor
    public PeerUI(Peer peer){
        this.peer = peer;
        initGUI();
        initWindowListener();
    }

    /**
     * The method to initialize the graphic user interface
     */
    private void initGUI(){

        // Panel of information
        mJtaMessageWindow = new JTextArea();
        mJtaMessageWindow.setEditable(false);
        mJtaMessageWindow.setFont(new Font(null, Font.PLAIN, 18));
        mJspShowingPanel = new JScrollPane(mJtaMessageWindow);

        // Panel of secret key input
        mJpKeyPanel = new JPanel();
        mJlKey = new JLabel("Secret Key: ");
        mJtfKeyInput = new JTextField(20);
        mJpKeyPanel.add(mJlKey);
        mJpKeyPanel.add(mJtfKeyInput);

        // Panel of file chosen
        mJpFilePanel = new JPanel();
        mJbFileChosen = new JButton("Select a File");
        mJbFileChosen.setPreferredSize(new Dimension(140, 25));
        mJfcFileChooser = new JFileChooser();
        mJfcFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        mJtfFileChosen = new JTextField(20);
        mJtfFileChosen.setEditable(false);
        mJpFilePanel.add(mJbFileChosen);
        mJpFilePanel.add(mJtfFileChosen);

        // Panel for choosing the receiver peer
        mJpReceiverSelectPanel= new JPanel();
        mJbReceiverChosen= new JButton("Select Receiver");
        mJbReceiverChosen.setPreferredSize(new Dimension(140, 25));
        mJtfReceiverChosen = new JTextField(20);
        mJtfReceiverChosen.setEditable(false);
        mJpReceiverSelectPanel.add(mJbReceiverChosen);
        mJpReceiverSelectPanel.add(mJtfReceiverChosen);

        // Panel of confirmation
        mJpConfirmPanel = new JPanel();
        mJbConfirm = new JButton("Encrypted Transmit");
        mJpConfirmPanel.add(mJbConfirm);

        // The input panel contains all the operation sections
        mJpInputPanel = new JPanel();
        mJpInputPanel.setLayout(new GridLayout(0, 1));
        mJpInputPanel.add(mJpKeyPanel);
        mJpInputPanel.add(mJpFilePanel);
        mJpInputPanel.add(mJpReceiverSelectPanel);
        mJpInputPanel.add(mJpConfirmPanel);

        // add components into the window frame
        this.add(mJspShowingPanel, BorderLayout.CENTER);
        this.add(mJpInputPanel, BorderLayout.SOUTH);

        // setting the window frame
        this.setTitle("Username: " + this.peer.peerBean.getName());
        this.setSize(500, 500);
        this.setLocation(800, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        // setting the action listener of buttons
        mJbFileChosen.addActionListener(this.peer);
        mJbReceiverChosen.addActionListener(this.peer);
        mJbConfirm.addActionListener(this.peer);
    }

    /**
     * Shows the given message on the panel.
     * @param msg The given message
     */
    public void showMSG(String msg){
        this.mJtaMessageWindow.append(msg + "\n");
    }

    /**
     * Set all the buttons enabled or not
     * @param isEnable Whether enable the buttons
     */
    public void enableButtons(boolean isEnable){
        this.mJbConfirm.setEnabled(isEnable);
        this.mJbReceiverChosen.setEnabled(isEnable);
        this.mJbFileChosen.setEnabled(isEnable);
    }

    /**
     * Define a window listener, we only need the method of window closing, (for advertise the leaving of this peer to the main server)
     */
    private void initWindowListener(){
        //set the window listener
        this.addWindowListener(new WindowListener() {
            //when close the window, the connection of the server stopped,
            //this peer should advertise the leaving to the main server
            @Override
            public void windowClosing(WindowEvent e) {
                // if the peer has connected to the server
                if (peer.oos != null){
                    peer.sendACKPeerLeave();
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowClosed(WindowEvent e) {}

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) {}

            @Override
            public void windowActivated(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {}
        });
    }


}
