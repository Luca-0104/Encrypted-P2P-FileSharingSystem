package DESAlgorithm.fileOperations;

import DESAlgorithm.cipherComponents.DecipherTool;

import java.io.*;
import java.util.Arrays;

/**
 * To decrypt and download cipher blocks.
 */
public class FileDecipherOperator extends FileOperator {

    // The file for downloading the blocks into
    private File outputFile;

    // the constructor
    public FileDecipherOperator(DecipherTool decipherTool, File outputFile) {
        super(decipherTool);
        this.outputFile = outputFile;
    }

    /**
     * Decipher a data block using DES algorithm, then writing it into the local place.
     */
    public void decipherAndDownloadBlock(byte[] cipherBlock, boolean isFinal, BufferedOutputStream bos) throws IOException {

        // decipher this block
        byte[] decipheredBlock = ((DecipherTool) this.cipherTool).decipher(cipherBlock);

        // if this is the last block
        if (isFinal) {
            // find the index of 0 in this byte array
            int index0 = indexOf(decipheredBlock, 0);
            // if the array contains 0
            if (index0 != -1) {
                // remove all the 0s at the end of this 8 bytes
                decipheredBlock = Arrays.copyOfRange(decipheredBlock, 0, indexOf(decipheredBlock, 0));
            }
        }

        // write in the file
        bos.write(decipheredBlock);
        bos.flush();

        // if the last block, we close the stream
        if(isFinal){
            bos.close();
        }

    }

    /**
     * Find the index of a specific number in an int array.
     * If the array contains this number multiple times, the index of the first one would be returned
     * @param array The array to go through
     * @param num The number we will find in this array
     * @return The index of the first time this number appearing
     */
    private int indexOf(byte[] array, int num){
        for (int i = 0; i < array.length; i++){
            // check if this byte is 0
            String s = Integer.toBinaryString(array[i]);
            if (s.equals(String.valueOf(num))){
                return i;
            }
        }

        // -1 means this array does not contain this number
        return -1;
    }


}