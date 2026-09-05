package com.erp.module.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory token store with an absolute expiry time. Replace with Redis/JWT for multi-instance deployment. */
@Component
public class TokenStore {

    /** Login user snapshot, exposed to controllers through a request attribute. */
    public record LoginUser(Long userId, String username, String realName) {}

    private static final ThreadLocal<LoginUser> currentUser = new ThreadLocal<>();

    public static void setCurrentLoginUser(LoginUser user) { currentUser.set(user); }
    public static LoginUser getCurrentLoginUser() { return currentUser.get(); }
    public static void clear() { currentUser.remove(); }

    private record TokenEntry(LoginUser user, Instant expiresAt) {}

    private final Map<String, TokenEntry> store = new ConcurrentHashMap<>();
    private final Duration tokenTtl;

    public TokenStore(@Value("${erp.auth.token-ttl:PT8H}") Duration tokenTtl) {
        if (tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalArgumentException("erp.auth.token-ttl must be positive");
        }
        this.tokenTtl = tokenTtl;
    }

    public String create(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, new TokenEntry(user, Instant.now().plus(tokenTtl)));
        return token;
    }

    /** Returns null for an unknown or expired token and removes expired entries eagerly. */
    public LoginUser get(String token) {
        if (token == null) return null;
        TokenEntry entry = store.get(token);
        if (entry == null) return null;
        if (!entry.expiresAt().isAfter(Instant.now())) {
            store.remove(token, entry);
            return null;
        }
        return entry.user();
    }

    public void remove(String token) {
        if (token != null) store.remove(token);
    }

    @Scheduled(fixedDelayString = "${erp.auth.token-cleanup-delay:PT15M}")
    public void removeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    public Duration tokenTtl() { return tokenTtl; }
}
