package DESAlgorithm.cipherComponents;

import java.util.Arrays;

public class BitByteOperation {

    /**
     * Transform an 8-byte block into a 64-bit block.
     * @param blockInByte A block of plain text, which is represented in byte array (8-byte)
     * @param turnOnParity when filling the 0s, whether we add a bit at last for odd parity. (If this method is called from a key generator, this should be true)
     * @return The 64-bit block from the corresponding 8-byte block
     */
    protected int[] blockByteToBit(byte[] blockInByte, boolean turnOnParity){
        // this should be the 64-bit block result
        int[] blockInBit = new int[64];
        // the pointer for operating the result array (index)
        int pointer = 0;

        // loop through each byte in the array
        for (byte b : blockInByte){
            // transform this byte into a string representing the bits
            // we use (b & 0xff), because the byte b might be a negative number, we should add 256 to it.
            String bitString = Integer.toBinaryString(b & 0xff);

            // to add the parity bit at the end of this bitString
            if (turnOnParity && bitString.length() < 8){
                // determine whether add 0 or 1 (making an odd number of '1's in this byte)
                if (bitString.chars().filter(c -> c == '1').count() % 2 != 0){
                    // so far odd number of '1's, so add 0
                    bitString += '0';
                }else{
                    // so far even number of '1's, so add 1
                    bitString += '1';
                }
            }

            // the bitString might shorter than 8-bit, we should fill in 0s at the beginning of each byte
            int fillBitNum = 8 - bitString.length();

            // fill in the result with the proper number of 0s
            for (int i = 0; i < fillBitNum; i++){
                blockInBit[pointer] = 0;
                pointer++;
            }

            // fill in the result with bits in string of this byte
            for (char c : bitString.toCharArray()){
                // cast the char (only 1 or 0) into int
                int bit = Integer.parseInt(String.valueOf(c));
                // put it into the result
                blockInBit[pointer] = bit;
                pointer++;
            }
        }

        return blockInBit;
    }

    /**
     * Transform a bit block into a byte block. e.g. the 64-bit block -> 8 byte block
     * @param blockInBit A block represented in bits.
     * @return  a block represented in bytes.
     */
    protected byte[] blockBitToByte(int[] blockInBit){
        // the number of bits should be divided by 8
        if (blockInBit.length % 8 != 0){
            return null;
        }

        // the result block
        byte[] blockInByte = new byte[blockInBit.length / 8];
        // the pointer used to operate the result block
        int pointer = 0;

        // break the bits into blocks with 8 bits for each
        int[][] blocks = splitBlock(blockInBit, blockInBit.length / 8);
        // loop through all the 8-bit blocks
        for (int[] block : blocks){
            // change the bits in this block into a string
            String bitString = Arrays.toString(block).substring(1, Arrays.toString(block).length() - 1).replace(", ", "");
            // change the bit string into byte and record it into the result
            blockInByte[pointer] = (byte) Integer.parseInt(bitString, 2);
            pointer++;
        }

        return blockInByte;
    }

    /**
     * To perform the permutation on a specific block using a selected permutation function.
     * @param block An input block, this could be the 64-bit input block, concatenated S1...S8, CnDn etc.
     * @param permutationFunc A permutation function for this transmission, which could be IP, IP-1, P, PC-2...
     * @return A block of bits.
     */
    protected int[] performPermute(int[] block, int[] permutationFunc){
        // the permuted result block
        int[] permutedBlock = new int[permutationFunc.length];

        // loop through the permutation table
        for(int i = 0; i < permutationFunc.length; i++){
            // the element in permutation table - 1 would be the index of the element in the input block.
            permutedBlock[i] = block[permutationFunc[i] - 1];
        }

        return permutedBlock;
    }

    /**
     * Split a block evenly into several sub-blocks with the specific length.
     * This should be used in the cipher function F to split the result of XoR(R(R), K) into 8 blocks of 6-bit.
     * Or be used in anywhere needs to split the block.
     * @param block The whole block to be split
     * @param splitNum How many sub-blocks would be evenly split into
     * @return An array contains all the sub-blocks
     */
    protected int[][] splitBlock(int[] block, int splitNum){
        // check if the block length can be divided by the number of sub-blocks wanted
        if (block.length % splitNum != 0){
            return null;
        }

        // the length of each sub-block
        int lenEach = block.length / splitNum;

        // the result, which is an array of blocks
        int[][] resultBlocks = new int[splitNum][lenEach];

        // generate splitNum sub-blocks
        for (int i = 0; i < splitNum; i++){
            // create a sub-block
            int[] subBlock = new int[lenEach];
            // calculate the offset if this subBlock
            int offset = i * lenEach;

            // for generating this sub-block, we loop through a specific lenEach-bit block in the input block
            for(int j = 0; j < lenEach; j++){
                subBlock[j] = block[j + offset];
            }

            // record this sub-block into the result array
            resultBlocks[i] = subBlock;
        }

        return resultBlocks;
    }

    /**
     * This should be used in cipher function F to concatenate the 8 blocks of 4-bit into a single 32-bit block.
     * This can also be used in the any situations to connect multiple blocks into a single one.
     * @param blocks An array of blocks, which would be concatenated into a single one.
     * @return A single block.
     */
    protected int[] concatenateBlock(int[][] blocks){
        // calculate the size of the result block
        int size = 0;
        for (int[] block : blocks){
            size += block.length;
        }

        // define the result block
        int[] resultBlock = new int[size];

        // the pointer (index) used to operate the result block
        int pointer = 0;

        // loop through all the blocks to copy them into the resultBlock in this order
        for (int[] block : blocks){
            // go through every bit in this block
            for (int i = 0; i < block.length; i++){
                resultBlock[pointer] = block[i];
                pointer++;
            }
        }

        return resultBlock;
    }
}
