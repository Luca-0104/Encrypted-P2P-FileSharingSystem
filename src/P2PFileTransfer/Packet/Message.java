package P2PFileTransfer.Packet;

import java.io.Serializable;

/**
 * All the message packets transferred in our p2p secure communication system must be "Message" type
 */
public class Message implements Serializable {

    /*
        Possible message types
    */
    public static final String CONNECT_REQUEST = "conn_request";
    public static final String PORT_YOUR = "port_your";
    public static final String ACK_PEER_LEAVE = "ack_peer_leave";
    public static final String TRANSFER_REQUEST = "transfer_request";
    public static final String TRANSFER_REQUEST_TO_RECEIVER = "transfer_request_to_receiver";
    public static final String PORT_RECEIVER = "port_receiver";
    public static final String ACK_ACCEPT_TRANSFER = "ack_accept_transfer";
    public static final String ACK_REJECT_TRANSFER = "ack_reject_transfer";
    public static final String ENCRYPTED_DATA_BLOCK = "encrypted_date_block";
    public static final String ENCRYPTED_DATA_BLOCK_FINAL = "encrypted_date_block_final";
    public static final String SYNC_USERS = "sync_users";
    public static final String SYNC_REQUEST_USER_LIST = "sync_request_user_list";


    private String type;
    private Object content;

    public Message(String type, Object content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}

