package DESAlgorithm.cipherComponents;

public class CipherTool extends BitByteOperation {

    // The key object, which wraps the 64-bit key
    protected Key key;

    // The constructor
    public CipherTool(Key key){
        this.key = key;
    }

    /**
     * The Cipher function F, used to calculate the f(R, K)
     * @param r The 32-bit R of a specific iteration
     * @param k The 48-bit K of a specific iteration (chosen from the 64-bit KEY)
     * @return A 32-bit block as the output, which should be used to perform the "bit-by-bit addition modulo 2" operation with the L in the same iteration.
     */
    protected int[] cipherFuncF(int[] r, int[] k){
        // perform the bit selection function E on the R
        int[] outputE = bitSelectionFuncE(r);

        // perform the bit-by-bit addition modulo 2 (XOR) on the output of E and 48-bit K
        int[] outputXOR = bitByBitXOR(outputE, k);

        // split the output of XOR into 8 blocks of 6 bits
        int[][] blocksOf6bits = splitBlock(outputXOR, 8);

        // The array used to contain that 8 blocks of 4 bits
        int[][] blocksOf4bits = new int[8][4];
        // perform corresponding S(n) function to make each of the 6-bit block into a 4-bit block, (1 <= n <= 8)
        for (int i = 0; i < 8; i++){
            // perform the S(n) function on this 6-bit block
            int[] outputS4bits = selectionFuncS(blocksOf6bits[i], Constants.SELECTION_FUNCTIONS[i]);
            // record this 4-bit block into the array
            blocksOf4bits[i] = outputS4bits;
        }

        // concatenate the 8 blocks of 4 bits into a single block of 32 bits
        int[] outputS32bits = concatenateBlock(blocksOf4bits);

        // perform the permutation function P on the concatenated outputs of those 4-bit blocks, which is a 32-bit block.
        return performPermute(outputS32bits, Constants.P);
    }

    /**
     * The bit-by-bit addition modulo 2 (XOR) on two same sized int arrays
     * @param a One of the int array
     * @param b The other one of the int array
     * @return The same-sized int array carrying the results of bit-by-bit addition modulo 2
     */
    protected int[] bitByBitXOR(int[] a, int[] b){
        // check whether the size of a and b are the same
        if(a.length != b.length){
            return null;
        }

        // the result bit array
        int[] output = new int[a.length];

        // loop through both a and b
        for (int i = 0; i < a.length; i++){
            // check if a and b have the same bit at this position
            if(a[i] == b[i]){
                // the result for this bit should be 0
                output[i] = 0;
            }else{
                // the result for this bit should be 1
                output[i] = 1;
            }
        }

        return output;
    }

    /**
     * The function E used in the cipher function F, which takes a block of 32 bits to produce a block of 48 bits.
     * @param r The 32-bit R in the 16 times iteration.
     * @return A 48-bit block
     */
    private int[] bitSelectionFuncE(int[] r){
        return performPermute(r, Constants.E_BIT_SELECTION_TABLE);
    }

    /**
     * The selection function S used in the cipher function F, which transform a 6-bit block into a 4-bit block.
     * @param block One of the 6-bit blocks gotten from the splitBlock method
     * @param s The S table chosen from the constant repository, which should be one of the S1...S8
     * @return A 4-bit block
     */
    private int[] selectionFuncS(int[] block, int[] s){
        // The input block here must be a 6-bit one
        if (block.length != 6){
            return null;
        }

        // calculate the row number by concatenating the first and the last bit in the input block.
        int firstBit = block[0];
        int lastBit = block[block.length - 1];
        // 00 -> row 0
        // 01 -> row 1
        // 10 -> row 2
        // 11 -> row 3
        int row = -1;
        if (firstBit == 0){
            if (lastBit == 0){
                row = 0;    // 00
            } else if (lastBit == 1){
                row = 1;    // 01
            }

        } else if (firstBit == 1){
            if (lastBit == 0){
                row = 2;    // 10

            } else if (lastBit == 1){
                row = 3;    // 11
            }
        }
        // check whether the row is initialized successfully
        if (row == -1){
            return null;
        }

        // calculate the column number by using the middle 4 bits in the input block.
        // put the middle 4 bits of this block into a string
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 1; i < 5; i++){
            strBuilder.append(block[i]);
        }
        String middle4BitsStr = strBuilder.toString();
        // parse this binary string into a decimal number representing the column, which should range from 0-15
        int column = Integer.parseInt(middle4BitsStr, 2);

        // get the decimal number from the selection function table S(n) according to the row and column number
        int resultDecimal = s[(row * 16) + column];
        // change the decimal number into a binary string
        // Some results may less than 4 bit, so we need to fill 0 at its left side to make sure all of them are in 4-bit manner
        String resultBinaryStr = String.format("%4s", Integer.toBinaryString(resultDecimal)).replace(" ", "0");

        // The final result should be an int array contains that 4 bits
        int[] output = new int[4];
        for (int i = 0; i < 4; i++){
            // pares each bit in the string into an integer and record it into the output array
            output[i] = Integer.parseInt(resultBinaryStr.substring(i, i + 1));
        }
        return output;
    }

}
