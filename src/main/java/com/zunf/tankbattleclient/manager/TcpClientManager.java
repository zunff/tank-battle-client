package com.zunf.tankbattleclient.manager;

import com.zunf.tankbattleclient.model.message.InboundMessage;
import com.zunf.tankbattleclient.model.message.OutboundMessage;
import com.zunf.tankbattleclient.handler.ProtocolEncoder;
import com.zunf.tankbattleclient.handler.ProtocolFrameReader;
import javafx.application.Platform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP连接管理器
 *
 * @author zunf
 * @date 2025/12/12 22:08
 */
public class TcpClientManager {

    private final String host;
    private final int port;

    private final BlockingQueue<OutboundMessage> sendQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Socket socket;
    private Thread readerThread;
    private Thread writerThread;

    public TcpClientManager(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && running.get();
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);

        running.set(true);

        InputStream in = new BufferedInputStream(socket.getInputStream());
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());

        startWriter(out);
        startReader(in);
    }

    public void send(byte type, byte version, byte[] body) {
        if (!running.get()) {
            return;
        }
        boolean offer = sendQueue.offer(new OutboundMessage(type, version, body));
        if (!offer) {
            System.out.println("消息队列已满，丢弃消息");
        }
    }

    public void close() {
        running.set(false);
        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (writerThread != null) {
            writerThread.interrupt();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private void startWriter(OutputStream out) {
        writerThread = new Thread(() -> {
            try {
                System.out.println("启动写入线程");
                while (running.get()) {
                    OutboundMessage m = sendQueue.take(); // 阻塞
                    System.out.println("发送消息");
                    byte[] packet = ProtocolEncoder.encode(m.getType(), m.getVersion(), m.getBody());
                    out.write(packet);
                    out.flush();
                }
            } catch (InterruptedException e) {
                // 退出
                System.out.println("写入线程退出");
            } catch (IOException e) {
                running.set(false);
                // 这里可以通知 UI：断线
                Platform.runLater(() -> onDisconnected(e));
            } finally {
                close();
            }
        }, "tcp-writer");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    private void startReader(InputStream in) {
        readerThread = new Thread(() -> {
            ProtocolFrameReader fr = new ProtocolFrameReader();
            try {
                System.out.println("启动读取线程");
                while (running.get()) {
                    fr.readFrom(in); // 阻塞读
                    for (;;) {
                        InboundMessage msg = fr.tryDecodeOne();
                        if (msg == null) break;

                        // 回到 UI 线程处理
                        Platform.runLater(() -> onMessage(msg));
                    }
                }
            } catch (IOException e) {
                running.set(false);
                Platform.runLater(() -> onDisconnected(e));
            } finally {
                close();
            }
        }, "tcp-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * ====== 在 UI 层实现这两个回调 ======
     */
    protected void onMessage(InboundMessage msg) {
        // UI 更新：显示聊天、房间信息等
    }

    protected void onDisconnected(Exception e) {
        // UI 更新：提示断线
    }
}
