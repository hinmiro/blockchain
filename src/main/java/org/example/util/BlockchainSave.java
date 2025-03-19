package org.example.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Block;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
public class BlockchainSave {
    private FileWriter writer;
    private Gson gson;
    private final String filePath = "src/main/resources/data";

    public void saveBlockchainToFile(List<Block> blockchain) {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-ms");
            String formattedDate = now.format(formatter);
            String fileName = filePath + "/blockchain_" + formattedDate + ".json";

            writer = new FileWriter(fileName);
            gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(blockchain, writer);
            writer.flush();
            writer.close();
            log.info("Blockchain saved to file: {}", filePath);
        } catch (IOException e) {
            log.error("Error saving blockchain to file: {}", e.getCause().getMessage());
        }
    }

    public List<Block> loadBlockchainFromFile() {
        List<Block> loadedBlockchain = new ArrayList<>();
        File directory = new File(filePath);

        if (!directory.exists()) {
            directory.mkdirs();
            log.info("Created directory: {}", filePath);
            return loadedBlockchain;
        }

        File[] files = directory.listFiles((dir, name) -> name.startsWith("blockchain_") && name.endsWith(".json"));

        if (files == null || files.length == 0) {
            log.info("No blockchain files found in {}", filePath);
            return loadedBlockchain;
        }

        File latestFile = files[0];
        for (File file : files) {
            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }

        try {
            TypeToken<ArrayList<Block>> typeToken = new TypeToken<ArrayList<Block>>() {};
            Reader reader = new FileReader(latestFile);
            gson = new GsonBuilder().create();
            loadedBlockchain = gson.fromJson(reader, typeToken.getType());
            reader.close();

            log.info("Loaded latest blockchain from file: {}", latestFile.getName());
        } catch (IOException e) {
            log.error("Error on loading blockchain file {}", e.getCause().getMessage());
        }
        return loadedBlockchain;
    }
}
