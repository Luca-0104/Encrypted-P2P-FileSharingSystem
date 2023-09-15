package presentation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DownloadDirChoosingUI extends JFrame {

    // operation sections
    private JPanel mJpDownloadDirPanel;
    private JPanel mJpConfirmPanel;

    // operation section - download directory section
    public JButton mJbDownloadDirChosen;
    public JFileChooser mJfcDownloadDirChooser;
    public JTextField mJtfDownloadDirChosen;

    // operation section - confirm section
    public JButton mJbConfirm;

    // The action listener
    private ActionListener mActionListener;

    // constructor
    public DownloadDirChoosingUI(ActionListener al){
        this.mActionListener = al;
        initUI();
    }

    /**
     * Initialize the user interface
     */
    private void initUI(){

        // Panel for choosing the download directory
        mJpDownloadDirPanel= new JPanel();
        mJbDownloadDirChosen= new JButton("Download Dir");
        mJbDownloadDirChosen.setPreferredSize(new Dimension(110, 25));
        mJfcDownloadDirChooser = new JFileChooser();
        mJfcDownloadDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        mJtfDownloadDirChosen = new JTextField(30);
        mJtfDownloadDirChosen.setEditable(false);
        mJpDownloadDirPanel.add(mJbDownloadDirChosen);
        mJpDownloadDirPanel.add(mJtfDownloadDirChosen);

        // Panel of confirmation
        mJpConfirmPanel = new JPanel();
        mJbConfirm = new JButton("Download");
        mJpConfirmPanel.add(mJbConfirm);

        // add components into the window frame
        this.add(mJpDownloadDirPanel, BorderLayout.CENTER);
        this.add(mJpConfirmPanel, BorderLayout.SOUTH);

        // setting the window frame
        this.setTitle("Select your download dir");
        this.setSize(490, 130);
        this.setLocation(800, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        // setting the action listener of buttons
        mJbDownloadDirChosen.addActionListener(mActionListener);
        mJbConfirm.addActionListener(mActionListener);

    }

}
