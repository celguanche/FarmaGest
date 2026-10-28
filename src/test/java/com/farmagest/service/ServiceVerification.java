package com.farmagest.service;

import com.farmagest.exception.BusinessException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone checks: no database, credentials or extra test dependencies required.
 */
public final class ServiceVerification {
    public static void main(String[] args) throws Exception {
        State success = new State();
        check(Transactions.execute(success.connection(), c -> 42) == 42);
        check(success.calls.equals(List.of("auto:false", "commit", "auto:true")));

        State business = new State();
        BusinessException failure = new BusinessException("failure");
        try {
            Transactions.execute(business.connection(), c -> {
                throw failure;
            });
            throw new AssertionError("Expected business failure");
        } catch (BusinessException e) {
            check(e == failure);
        }
        check(business.calls.equals(List.of("auto:false", "rollback", "auto:true")));

        State commit = new State();
        commit.failCommit = true;
        try {
            Transactions.execute(commit.connection(), c -> null);
            throw new AssertionError("Expected commit failure");
        } catch (SQLException expected) {
            check(expected.getMessage().equals("commit"));
        }
        check(commit.calls.equals(List.of("auto:false", "commit", "rollback", "auto:true")));

        State rollback = new State();
        rollback.failRollback = true;
        BusinessException original = new BusinessException("original");
        try {
            Transactions.execute(rollback.connection(), c -> {
                throw original;
            });
            throw new AssertionError("Expected rollback failure");
        } catch (BusinessException e) {
            check(e == original && e.getSuppressed().length == 1);
        }
        check(rollback.calls.equals(List.of("auto:false", "rollback")));

        check(Checks.tax(new BigDecimal("0.05"), new BigDecimal("10")).equals(new BigDecimal("0.01")));
        rejects(() -> Checks.subtotal(BigDecimal.ONE, 0));
        rejects(() -> Checks.money(new BigDecimal("100000000.00"), 10));
        rejects(() -> Checks.vat(new BigDecimal("100.01")));
        Object item = new Object();
        rejects(() -> Checks.items(List.of(item, item)));

        char[] password = "verification-password".toCharArray();
        String hash = PasswordHasher.hash(password);
        check(PasswordHasher.verify(password, hash));
        check(!PasswordHasher.verify("incorrect".toCharArray(), hash));
        check(!PasswordHasher.verify(password, "unknown-format"));
        check(!hash.equals(PasswordHasher.hash(password)));
        System.out.println("Service verification passed: transactions, rollback failures, amounts, duplicate instances and password hashing.");
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Verification failed");
    }

    private static void rejects(Runnable operation) {
        try {
            operation.run();
        } catch (BusinessException expected) {
            return;
        }
        throw new AssertionError("Expected validation failure");
    }

    private static final class State {
        final List<String> calls = new ArrayList<>();
        boolean failCommit;
        boolean failRollback;

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getAutoCommit":
                                return true;
                            case "setAutoCommit":
                                calls.add("auto:" + args[0]);
                                return null;
                            case "commit":
                                calls.add("commit");
                                if (failCommit) throw new SQLException("commit");
                                return null;
                            case "rollback":
                                calls.add("rollback");
                                if (failRollback) throw new SQLException("rollback");
                                return null;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }
    }
}
