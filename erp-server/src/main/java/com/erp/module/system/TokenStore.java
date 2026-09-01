package com.erp.module.system;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Token 存储(骨架实现)
 * TODO 接入 Redis 或 JWT,并增加过期时间;多实例部署前必须替换
 */
@Component
public class TokenStore {

    /** 登录用户快照,供 Controller 通过 request attribute 获取 */
    public record LoginUser(Long userId, String username, String realName) {
    }

    /** 线程存储当前登录用户 */
    private static final ThreadLocal<LoginUser> currentUser = new ThreadLocal<>();

    public static void setCurrentLoginUser(LoginUser user) {
        currentUser.set(user);
    }

    public static LoginUser getCurrentLoginUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }

    private final Map<String, LoginUser> store = new ConcurrentHashMap<>();

    public String create(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, user);
        return token;
    }

    public LoginUser get(String token) {
        return token == null ? null : store.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            store.remove(token);
        }
    }
}
