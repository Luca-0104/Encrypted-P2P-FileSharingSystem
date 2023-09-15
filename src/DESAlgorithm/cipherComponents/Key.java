package DESAlgorithm.cipherComponents;

import java.nio.charset.StandardCharsets;

public class Key extends BitByteOperation{

    private final String keyStr;   // The user input of the key
    private int[] keyBits;         // After transforming into the bits
    private int[][] keySet;       // An array contains 48-bit K1, K2, ..., K16

    // The constructor
    public Key(String keyStr){
        this.keyStr = keyStr;
        // initialize the key in the bit form
        this.generateKeyBit();
        // initialize the 48-bit K1, K2, ..., K16
        this.generateKeySet();
    }

    /* Getters */

    public String getKeyStr() {
        return keyStr;
    }

    public int[] getKeyBits() {
        return keyBits;
    }

    public int[][] getKeySet() {
        return keySet;
    }

    /**
     * Get a specific Kn from the key set.
     * e.g. K1, K2, ...
     * @param index The index of the K(n) in the key set, which should range from 0 to 15. The index is always n-1
     * @return An array represent K(n)
     */
    public int[] get48BitKnFromSet(int index){
        if (index >= 0 && index <= 15){
            return this.keySet[index];
        }else{
            return null;
        }
    }

    /**
     * Transform the user inputted key string into an array of bits.
     * This should be used in the constructor to initialize the field of keyBit.
     * This method would ensure the keyBit is 64-bit, if the user input less than 64-bit, it would be filled with 0s at the end.
     * On the other hand, if the user input is more than 64-bit, it would be truncated from the 64th bit.
     */
    private void generateKeyBit(){
        // transform the key string into the bytes
        byte[] keyBytes = this.keyStr.getBytes(StandardCharsets.UTF_8);
        // the 8-byte array after transformation
        byte[] keyBytes8 = new byte[8];

        // transform the user input into 8 bytes array
        for (int i = 0; i < 8; i++){
            // check if reach the end of keyBytes
            if(i < keyBytes.length){
                // if not, we move this byte from keyBytes to keyBytes8
                keyBytes8[i] = keyBytes[i];
            }else{
                // if the end is reached, we will fill 0 in the rest of the keyBytes8
                keyBytes8[i] = 0;
            }
        }

        // transform the 8-byte array into 64-bit array, then initialize the field of keyBit
        this.keyBits = blockByteToBit(keyBytes8, true);
    }

    /**
     * Generate 48-bit K1, K2, ..., K16 from the keyBit using 16 times iterations.
     * This should be used in the constructor to initialize the field of keySet.
     */
    private void generateKeySet(){
        // initialize the keySet (48-bit K1, K2, ..., K16)
        this.keySet = new int[16][48];

        // separate the 48-bit key into 28-bit C0 and 28-bit D0
        int[][] cd = permutedChoice1();
        int[] c = cd[0];
        int[] d = cd[1];

        // perform the 16-time iteration, during the iteration of index i, the K(i+1) would be generated.
        for(int i = 0; i < Constants.ITERATION_TIMES; i++){

            // calculate the next C
            int[] nextC = leftShift(c, Constants.LEFT_SHIFT_NUMBERS[i]);
            // calculate the next D
            int[] nextD = leftShift(d, Constants.LEFT_SHIFT_NUMBERS[i]);

            // perform the permuted choice 2 to generate the 48-bit K for this iteration
            this.keySet[i] = permutedChoice2(nextC, nextD);

            // update the C and D
            c = nextC;
            d = nextD;

        }
    }

    /**
     * Perform the permuted choice 1 on the complete 64-bit key to generate the 28-bit C0 and 28-bit D0
     * @return An array contains 2 arrays, which are 28-bit C0 and 28-bit D0 respectively.
     */
    private int[][] permutedChoice1(){
        // perform the permutation of PC_1 on the keyBits block
        int[] c0d0 = performPermute(this.keyBits, Constants.PC_1);

        // split the c0d0 into 28-bit c0 and 28-it d0
        return splitBlock(c0d0, 2);
    }

    /**
     * Perform the permuted choice 2 on the combination of C and D to generate the 48-bit K
     * @param c The 28-bit C (Left part of the key iteration)
     * @param d The 28-bit D (Right part of the key iteration)
     * @return The 48-bit K, which should be used in the cipher function F of a specific iteration.
     */
    private int[] permutedChoice2(int[] c, int[] d){
        // concatenate c and d into cd
        int[] cd = concatenateBlock(new int[][]{c, d});

        // perform the permutation on cd using PC-2 table
        return performPermute(cd, Constants.PC_2);
    }

    /**
     * Perform the left shift on a 28-bit half key, which should be a specific C or D.
     * @param halfK A specific 28-bit C or 28-bit D
     * @param shiftNum The number of the left shift.
     * @return The next C or the next D.
     */
    private int[] leftShift(int[] halfK, int shiftNum){
        // the 28-bit block after leftShifting
        int[] lShiftResult = new int[28];

        // The shiftNum would be used to separate the key in to original left and right side.
        // loop through the halfK to generate the left shift result.
        for (int i = 0; i < halfK.length; i++){
            // For first several bits, which should be moved to the end
            if (i < shiftNum){
                // e.g. if shiftNum = 2, then
                // halfK: 1, 2, ... -> right part of the result: ..., 1, 2
                lShiftResult[(lShiftResult.length - shiftNum) + i] = halfK[i];
            }else{
                // For the rest of the bits, which should be placed from the beginning of the result array
                // e.g. if shiftNum = 2, then
                // halfK: 1, 2, 3, 4, 5 -> right part of the result: 3, 4, 5, 1, 2
                lShiftResult[i - shiftNum] = halfK[i];
            }
        }

        return lShiftResult;
    }
}
