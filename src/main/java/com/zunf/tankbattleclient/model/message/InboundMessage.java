package com.zunf.tankbattleclient.model.message;

public class InboundMessage {

    private byte type;
    private byte version;
    private byte[] body;

    public InboundMessage(byte type, byte version, byte[] body) {
        this.type = type;
        this.version = version;
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
}
