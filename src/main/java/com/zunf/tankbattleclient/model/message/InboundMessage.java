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
}
