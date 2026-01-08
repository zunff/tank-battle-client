package com.zunf.tankbattleclient.util;

/**
 * 启动参数工具类
 * 用于从IDEA启动配置的VM options中读取参数
 * 
 * 使用方法：
 * 在IDEA的Run Configuration中，VM options添加：
 * -Dlogin.username=your_username -Dlogin.password=your_password
 */
public class LaunchArgsUtil {

    /**
     * 登录用户名参数名
     */
    private static final String LOGIN_USERNAME_KEY = "login.username";

    /**
     * 登录密码参数名
     */
    private static final String LOGIN_PASSWORD_KEY = "login.password";

    /**
     * 获取登录用户名（从VM options中读取）
     * 
     * @return 用户名，如果未设置则返回null
     */
    public static String getLoginUsername() {
        return System.getProperty(LOGIN_USERNAME_KEY);
    }

    /**
     * 获取登录密码（从VM options中读取）
     * 
     * @return 密码，如果未设置则返回null
     */
    public static String getLoginPassword() {
        return System.getProperty(LOGIN_PASSWORD_KEY);
    }
}
