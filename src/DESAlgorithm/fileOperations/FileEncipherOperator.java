package DESAlgorithm.fileOperations;

import DESAlgorithm.cipherComponents.EncipherTool;
import P2PFileTransfer.Packet.Message;

import java.io.*;

/**
 * To encrypt and transfer a file block by block.
 * The file comes from local place, flushed to the socket stream.
 */
public class FileEncipherOperator extends FileOperator{

    // The output stream to the receiver
    private ObjectOutputStream oos;

    // The uploaded file for encryption and transmission
    private File plainTextFile;

    // The constructor
    public FileEncipherOperator(File plainTextFile, EncipherTool encipherTool, ObjectOutputStream oos) {
        super(encipherTool);
        this.oos = oos;
        this.plainTextFile = plainTextFile;
    }

    /**
     * Encipher the file using DES algorithm, a cipher text file would then be generated.
     * After each block being encrypted, it would be sent to the receiver.
     */
    public void encipherAndSendBlocks(){
        // How many 8-byte blocks are there in this file. (the file length can be surely divided by 8)
        long blockNumber = this.plainTextFile.length() / 8 + (this.plainTextFile.length() % 8 != 0 ? 1 : 0);

        // start a stream for reading in the plain text file
        try (
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(this.plainTextFile))
        ){
            // set the buffer size as 8 bytes (a 64-bit block)
            byte[] block = new byte[8];

            // how many blocks has been read
            long blockNumSoFar = 0;

            // read in the file as blocks
            while((bis.read(block)) != -1){
                // update block number
                blockNumSoFar++;

                // encipher this block
                byte[] cipherBlock = ((EncipherTool) this.cipherTool).encipher(block);

                // pack the cipher block into the message
                Message msg;
                // determine whether this is the last block
                if (blockNumSoFar == blockNumber){
                    // the last block
                    // encapsulate the cipher block into a message
                    msg = new Message(Message.ENCRYPTED_DATA_BLOCK_FINAL, cipherBlock);

                }else{
                    // not the last block
                    // encapsulate the cipher block into a message
                    msg = new Message(Message.ENCRYPTED_DATA_BLOCK, cipherBlock);

                }

                // write the block into the cipher text file
                this.oos.writeObject(msg);
                this.oos.flush();

                // refresh the buffer block (if the total bytes cannot be divided by 8, the last block would be filled with 0s at the end)
                block = new byte[8];
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
