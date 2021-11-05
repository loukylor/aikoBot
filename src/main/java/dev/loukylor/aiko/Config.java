package dev.loukylor.aiko;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private static final String path = "configs/config.json";
    private static final Config instance = load();

    private String botToken;

    private static Config load() {
        try {
            File file = new File(path);

            // If the file doesn't exist, make it, then return a new config
            if (!file.exists()) {
                // If the directory doesn't exist, make it
                Path directory = Paths.get(path).getParent();
                if (!Files.isDirectory(directory))
                    Files.createDirectory(directory);
                file.createNewFile();

                Config config = new Config();
                ObjectManager.mapper.writeValue(file, config);
                return config;
            } else {
                return ObjectManager.mapper.readValue(file, Config.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @JsonGetter("botToken")
    public String getBotToken() {
        return this.botToken;
    }

    @JsonSetter("botToken")
    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public static Config getInstance() {
        return instance;
    }
}
