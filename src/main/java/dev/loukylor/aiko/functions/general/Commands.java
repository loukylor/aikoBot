package dev.loukylor.aiko.functions.general;

import dev.loukylor.aiko.Main;
import dev.loukylor.aiko.hooks.Command;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class Commands {
    @Command(name = "ping", description = "A command that shows the bot's ping")
    public static void handlePing(SlashCommandEvent event) {
        event.reply(String.format("%sms!", Main.getApi().getGatewayPing())).queue();
    }
}
