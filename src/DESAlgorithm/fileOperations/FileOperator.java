package DESAlgorithm.fileOperations;

import DESAlgorithm.cipherComponents.CipherTool;

import java.io.File;

public class FileOperator {

    // the tool used for encipher or decipher
    protected CipherTool cipherTool;

    // the constructor
    public FileOperator(CipherTool cipherTool){
        this.cipherTool = cipherTool;
    }

}
