package com.zunf.tankbattleclient.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置管理器 单例
 *
 * @author zunf
 * @date 2025/12/12 23:07
 */
public final class ConfigManager {

    private static volatile ConfigManager INSTANCE;
    private final Properties props = new Properties();

    private ConfigManager() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("未找到配置文件 application.properties，使用默认配置");
            }
        } catch (IOException e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
        }
    }

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ConfigManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConfigManager();
                }
            }
        }
        return INSTANCE;
    }

    public String getString(String key, String def) {
        return props.getProperty(key, def);
    }

    public int getInt(String key, int def) {
        String v = props.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public String getServerHost() {
        return getString("server.host", "localhost");
    }

    public int getServerPort() {
        return getInt("server.port", 8888);
    }

    public int getProtocolVersion() {
        return getInt("protocol.version", 1);
    }
}

