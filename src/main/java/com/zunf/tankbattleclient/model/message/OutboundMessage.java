package com.zunf.tankbattleclient.model.message;

public class OutboundMessage {

    private byte type;
    private byte version;
    private int requestId;
    private byte[] body;

    public OutboundMessage(byte type, byte version, int requestId, byte[] body) {
        this.type = type;
        this.version = version;
        this.requestId = requestId;
        this.body = body;
    }

    public byte getType() {
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
