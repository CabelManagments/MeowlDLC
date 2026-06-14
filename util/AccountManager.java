package com.yourcheat.util;

import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class AccountManager {

    public static final AccountManager INSTANCE = new AccountManager();
    private final List<String> accounts = new ArrayList<>();
    private final Path file = MinecraftClient.getInstance()
            .runDirectory.toPath().resolve("meowldlc_accounts.txt");

    private AccountManager() { load(); }

    public List<String> getAccounts() { return accounts; }

    public void addAccount(String name) {
        if (!accounts.contains(name)) { accounts.add(name); save(); }
    }

    public void removeAccount(String name) { accounts.remove(name); save(); }

    public void clearAll() { accounts.clear(); save(); }

    public void loginAccount(String name) {
        // Оффлайн смена ника через reflection (работает без authlib)
        try {
            var mc = MinecraftClient.getInstance();
            var sessionField = mc.getClass().getDeclaredField("session");
            sessionField.setAccessible(true);
            var sessionClass = Class.forName("net.minecraft.client.util.Session");
            var constructor = sessionClass.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    java.util.Optional.class, java.util.Optional.class,
                    sessionClass.getDeclaredClasses()[0]);
            constructor.setAccessible(true);
            // Тип аккаунта — OFFLINE
            var typeField = sessionClass.getDeclaredClasses()[0];
            var offline = typeField.getField("OFFLINE").get(null);
            var session = constructor.newInstance(
                    name, "00000000000000000000000000000000",
                    "MeowlDLC", java.util.Optional.empty(),
                    java.util.Optional.empty(), offline);
            sessionField.set(mc, session);
        } catch (Exception e) {
            // Fallback — просто логируем
            System.err.println("[MeowlDLC] AltManager: failed to login as " + name + ": " + e.getMessage());
        }
    }

    private void save() {
        try { Files.write(file, accounts); } catch (IOException e) { e.printStackTrace(); }
    }

    private void load() {
        try {
            if (Files.exists(file)) accounts.addAll(Files.readAllLines(file));
        } catch (IOException e) { e.printStackTrace(); }
    }
}

