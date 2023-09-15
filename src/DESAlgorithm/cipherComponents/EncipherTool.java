package DESAlgorithm.cipherComponents;

public class EncipherTool extends CipherTool{

    public EncipherTool(Key key){
        super(key);
    }

    /**
     * Encipher a 64-bit block of the plain text
     * @param blockInByte An 8-byte input block
     * @return An 8-byte block represents the cipher text of the input block
     */
    public byte[] encipher(byte[] blockInByte){
        // the 8-byte block should be transformed into the 64-bit block first
        int[] block = blockByteToBit(blockInByte, false);

        // perform the initial permutation using IP, getting the result "permuted input"
        int[] permutedInput = performPermute(block, Constants.IP);

        // use the permuted input as the input of a "complex key-dependent computation", getting the result "preoutput"
        int[] preOutput = keyDependentComputation(permutedInput, key);

        // perform the final permutation using IP-1, getting the final enciphered result of this block
        int[] outputInBit = performPermute(preOutput, Constants.IP_INVERSE);

        // change the 64-bit block into an 8-byte block
        return blockBitToByte(outputInBit);

    }

    /**
     * Start that "complex key-dependent computation".
     * The 16 iterations should be performed here.
     * @param permutedInputBlock The input of this computation should be the "permuted input block" (64-bit).
     * @param key The key object, which is generated with the according secrete key string
     * @return The bit block called "preoutput"
     */
    private int[] keyDependentComputation(int[] permutedInputBlock, Key key){
        // separate the permuted input block into 32-bit L0 and 32-bit R0
        int[] l = new int[32];
        int[] r = new int[32];

        for(int i = 0; i < 32; i++){
            l[i] = permutedInputBlock[i];
            r[i] = permutedInputBlock[i + 32];
        }

        // perform the 16-time iteration,
        // during the iteration of index i,
        // the K(i+1) is needed.
        // L(i) -> L(i+1), R(i) -> R(i+1)
        for (int i = 0; i < Constants.ITERATION_TIMES; i++){

            // calculate the next L:  L' = R
            int[] nextL = r;
            // calculate the next R:  R' = L (+) f(R, K)
            int[] outputOfFuncF = cipherFuncF(r, key.get48BitKnFromSet(i));
            int[] nextR = bitByBitXOR(l, outputOfFuncF);

            // update the L and R
            l = nextL;
            r = nextR;

        }

        // concatenate the R16 with L16 to form the preoutput (R16L16)
        return concatenateBlock(new int[][]{r, l});
    }

}
