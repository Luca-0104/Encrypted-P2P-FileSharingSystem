package P2PFileTransfer.peer;

import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PeerBean implements Serializable {

    private String port;
    private String name;
    private ObjectOutputStream oos; // The output stream from the main server to this peer. Therefore, all the oos of connected peers can be recorded by main server.

    public PeerBean(String name){
        this.name = name;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }

    public void setOos(ObjectOutputStream oos) {
        this.oos = oos;
    }
}
