package dev.loukylor.aiko;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.MapType;
import dev.loukylor.aiko.entities.Birthday;
import dev.loukylor.aiko.entities.GuildConfig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ObjectManager {
    private static final String configPath = "configs";
    public static final ObjectMapper mapper = JsonMapper
            .builder()
            // Builds date module (i think?)
            .findAndAddModules()
            .build()
            // Pretty print pls
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            // Prevents LocalDates from being serialized really weirdly
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    public static HashMap<Long, Birthday> birthdays = load("birthdays", Long.class, Birthday.class);
    public static HashMap<Long, GuildConfig> guildConfigs = load("guildConfigs", Long.class, GuildConfig.class);

    public static <T> List<T> load(String fileName, Class<T> tClass) {
        File file = Paths.get(configPath, fileName + ".json").toFile();
        try {
            if (!file.exists()){
                file.createNewFile();
                Files.write(file.toPath(), Collections.singletonList("[]"));
            }
            return (List<T>) Arrays.asList(mapper.readValue(file, Array.newInstance(tClass, 0).getClass()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static <K, V> HashMap<K, V> load(String fileName, Class<K> tKeyClass, Class<V> tValueClass) {
        File file = Paths.get(configPath, fileName + ".json").toFile();
        try {
            if (!file.exists()) {
                file.createNewFile();
                Files.write(file.toPath(), Collections.singletonList("{}"));
            }
            MapType mapType = mapper.getTypeFactory().constructMapType(HashMap.class, tKeyClass, tValueClass);
            return mapper.readValue(file, mapType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public static void save(Object obj, String fileName) {
        File file = Paths.get(configPath, fileName + ".json").toFile();
        try {
            mapper.writeValue(file, obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
