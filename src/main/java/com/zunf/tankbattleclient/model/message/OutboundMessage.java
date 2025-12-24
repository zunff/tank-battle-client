package com.zunf.tankbattleclient.model.message;

public class OutboundMessage {

    private int type;
    private byte version;
    private int requestId;
    private byte[] body;

    public OutboundMessage(int type, byte version, int requestId, byte[] body) {
        this.type = type;
        this.version = version;
        this.requestId = requestId;
        this.body = body;
    }

    public int getType() {
        return type;
    }

    public byte getVersion() {
        return version;
    }

    public byte[] getBody() {
        return body;
    }

    public int getRequestId() {
        return requestId;
    }
}
