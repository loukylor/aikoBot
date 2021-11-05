package dev.loukylor.aiko;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;

import javax.security.auth.login.LoginException;

public class Main {
    private static JDA api;

    public static void main(String[] arguments) {
        try {
            api = JDABuilder.createDefault(Config.getInstance().getBotToken())
                    .addEventListeners(new EventHandler())
                    // Should cache for me, so no need to do intelligent fetching
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public static JDA getApi() {
        return api;
    }
}