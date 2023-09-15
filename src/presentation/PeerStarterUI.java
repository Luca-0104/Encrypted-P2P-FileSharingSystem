package presentation;

import P2PFileTransfer.peer.Peer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class PeerStarterUI extends JFrame {

    // The UI components
    public JLabel mJlInstruction;
    public JPanel mJpUsernamePanel;
    public JLabel mJlUsername;
    public JTextField mJtfUsername;
    public JPanel mJpJoinPanel;
    public JButton mJbJoin;

    // The action listener
    private Peer peer;

    // the constructor
    public PeerStarterUI(Peer peer){
        this.peer = peer;
        initGUI();
        initWindowListener();
    }

    /**
     * Initialize the graphic user interface
     */
    private void initGUI(){
        // the instruction
        mJlInstruction = new JLabel("Secure Communication Session");
        mJlInstruction.setHorizontalAlignment(SwingConstants.CENTER);
        mJlInstruction.setFont(new Font(null, Font.BOLD, 24));

        // username input section
        mJpUsernamePanel = new JPanel();
        mJlUsername = new JLabel("Username: ");
        mJtfUsername = new JTextField(20);
        mJpUsernamePanel.add(mJlUsername);
        mJpUsernamePanel.add(mJtfUsername);

        // join (confirm) section
        mJpJoinPanel = new JPanel();
        mJbJoin = new JButton("Join the session");
        mJpJoinPanel.add(mJbJoin);

        // add components to this JFrame
        this.setLayout(new GridLayout(0, 1));
        this.add(mJlInstruction);
        this.add(mJpUsernamePanel);
        this.add(mJpJoinPanel);

        // setting the window frame
        this.setTitle("DES Algorithm");
        this.setSize(500, 500);
        this.setLocation(800, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        // add listeners for buttons
        mJbJoin.addActionListener(this.peer);
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
