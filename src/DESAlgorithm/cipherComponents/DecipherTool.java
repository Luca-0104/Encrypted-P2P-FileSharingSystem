package DESAlgorithm.cipherComponents;

public class DecipherTool extends CipherTool{

    // The constructor
    public DecipherTool(Key key) {
        super(key);
    }

    /**
     * Decipher a 64-bit block of cipher text back to the 64-bit block of plain text
     * @param blockInByte An 8-byte block of the cipher text
     * @return An 8-byte block of plain text
     */
    public byte[] decipher(byte[] blockInByte){
        // the 8-byte block should be transformed into the 64-bit block first
        int[] block = blockByteToBit(blockInByte, false);

        // perform the initial permutation using IP, getting the result "permuted input"
        int[] permutedInputBlock = performPermute(block, Constants.IP);

        // doing the inverted key dependent computation
        int[] preOutput = invertedKeyDependentComputation(permutedInputBlock, this.key);

        // perform the final permutation using IP-1, getting the final enciphered result of this block
        int[] outputInBit = performPermute(preOutput, Constants.IP_INVERSE);

        // change the 64-bit block into an 8-byte block
        return blockBitToByte(outputInBit);
    }

    /**
     * Perform the 16 iterations in an inverted order to perform the description.
     * @param permutedInputBlock The R16L16
     * @param key The key object, which is generated with the according secrete key string
     * @return The bit block of preoutput
     */
    private int[] invertedKeyDependentComputation(int[] permutedInputBlock, Key key){
        // separate the permuted input block into 32-bit R16 and 32-bit L16
        int[] l = new int[32];
        int[] r = new int[32];

        for(int i = 0; i < 32; i++){
            r[i] = permutedInputBlock[i];
            l[i] = permutedInputBlock[i + 32];
        }

        // perform the 16-time iterations in the inverted order
        // during the iteration of index i,
        // the K(16-i) is needed
        // L(i) -> L(i+1), R(i) -> R(i+1)   (NOTICE: i here means the index, which starts from 0)
        for (int i = 0; i < Constants.ITERATION_TIMES; i++){

            // calculate the former R: R = L'
            int[] formerR = l;
            //calculate the former L: L = R' (+) f(L', K)
            int[] outputOfFuncF = cipherFuncF(l, key.get48BitKnFromSet((16 - i) - 1));
            int[] formerL = bitByBitXOR(r, outputOfFuncF);

            // update the R and L
            r = formerR;
            l = formerL;

        }

        // concatenate the L0 with R0 to form the preoutput (L0R0)
        return concatenateBlock(new int[][]{l, r});
    }
}
