package com.auction.server.db;

import com.auction.shared.model.Auction;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class DataStore {

    private DataStore() {
        // Không cho khởi tạo
    }

    public static void saveAuctions(List<Auction> auctions, String filePath) throws IOException {
        if (auctions == null) {
            throw new IllegalArgumentException("Auction list không được null");
        }
        if (Paths.get(filePath).getParent() != null) {
            Files.createDirectories(Paths.get(filePath).getParent());
        }
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(filePath))) {
            out.writeObject(new ArrayList<>(auctions));
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Auction> loadAuctions(String filePath) throws IOException, ClassNotFoundException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn file không được rỗng");
        }
        if (!Files.exists(Paths.get(filePath))) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(filePath))) {
            Object result = in.readObject();
            return result instanceof List ? (List<Auction>) result : new ArrayList<>();
        }
    }
}
