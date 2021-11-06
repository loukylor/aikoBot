package dev.loukylor.aiko.functions.birthdays;

import dev.loukylor.aiko.ObjectManager;
import dev.loukylor.aiko.entities.Birthday;
import dev.loukylor.aiko.entities.GuildConfig;
import dev.loukylor.aiko.hooks.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.time.DateTimeException;
import java.time.LocalDate;

@Command(name = "birthday", description = "Includes all the commands for managing birthdays")
public class Commands {
    @Subcommand(name = "add", description = "Adds your birthday to the bot")
    public static void handleBirthdayAdd(SlashCommandEvent event,
                                   @CommandParameter(name = "day", description = "The day of your birthday") long day,
                                   @CommandParameter(name = "month", description = "The month of your birthday") long month,
                                   @CommandParameter(name = "year", description = "The year of your birthday") long year)
    {
        LocalDate birthdayDate;
        try {
            birthdayDate = LocalDate.of((int) year, (int) month, (int) day);
        } catch (DateTimeException ex) {
            event.reply("Please check the date you inputted, it was invalid!").queue();
            return;
        }

        LocalDate now = LocalDate.now();
        if (birthdayDate.isAfter(now)) { // Don't accept input if it is in the future
            event.reply("The birthday cannot be in the future!").queue();
        }

        Birthday birthday = new Birthday();
        birthday.user = event.getUser().getIdLong();
        birthday.date = birthdayDate;

        // If we set the birthday's year to the current one, and it is before, then we know that the
        // whenCelebrate should be next year, else current year
        if (birthdayDate.withYear(now.getYear()).isBefore(now))
            birthday.whenCelebrate = birthdayDate.withYear(now.getYear() + 1);
        else
            birthday.whenCelebrate = birthdayDate.withYear(now.getYear());

        // Replace the bday if it already exists within the db
        if (ObjectManager.birthdays.containsKey(birthday.user)) {
            birthday.customMessages = ObjectManager.birthdays.get(birthday.user).customMessages;
            ObjectManager.birthdays.replace(birthday.user, birthday);
        } else {
            ObjectManager.birthdays.put(event.getUser().getIdLong(), birthday);
        }
        ObjectManager.save(ObjectManager.birthdays, "birthdays");
        event.reply("Birthday added successfully!").queue();
    }

    @Subcommand(name = "addmessage", description = "Adds a message that your friend will see on their birthday")
    public static void handleMessageAdd(SlashCommandEvent event,
                                        @CommandParameter(name = "friend", description = "The friend you want to see the message") User friend,
                                        @CommandParameter(name = "message", description = "The description to show the friend") String message)
    {
        if (event.getUser().getIdLong() == friend.getIdLong()){
            event.reply("You can't add a message to your own birthday!").queue();
            return;
        }

        // Determine if the user's share any guilds, yes it's slow but it doesn't really matter
        if (friend.getMutualGuilds().stream().noneMatch(guild -> event.getUser().getMutualGuilds().contains(guild))) {
            event.reply("You do not share any guilds with that user!").queue();
            return;
        }

        if (message.length() > 256) {
            event.reply("Your message is too long!").queue();
            return;
        }

        if (!ObjectManager.birthdays.containsKey(friend.getIdLong())) {
            event.reply("Your friend does not have a birthday registered!").queue();
            return;
        }

        Birthday birthday = ObjectManager.birthdays.get(friend.getIdLong());
        birthday.customMessages.put(friend.getIdLong(), message);
        ObjectManager.save(ObjectManager.birthdays, "birthdays");
        event.deferReply(true).setContent("Message added successfully!").queue();
    }

    @RequiredPermissions(permissions = Permission.ADMINISTRATOR)
    @Subcommand(name = "birthdaychannel", description = "Sets the channel to send birthday messages to")
    public static void handleBirthdayChannel(SlashCommandEvent event,
                                       @CommandParameter(name = "channel", description = "The channel to send birthday messages to") GuildChannel birthdayChannel)
    {
        if (!(birthdayChannel instanceof TextChannel)) {
            event.reply("That channel isn't a text channel!").queue();
            return;
        }


        if (!PermissionUtil.checkPermission(birthdayChannel, birthdayChannel.getGuild().getSelfMember(), Permission.MESSAGE_WRITE)) {
            event.reply("I do not have permission to speak in that channel!").queue();
            return;
        }

        if (ObjectManager.guildConfigs.containsKey(birthdayChannel.getGuild().getIdLong())) {
            GuildConfig config = ObjectManager.guildConfigs.get(birthdayChannel.getGuild().getIdLong());
            config.birthdayChannel = birthdayChannel.getIdLong();
            ObjectManager.guildConfigs.replace(birthdayChannel.getGuild().getIdLong(), config);
        } else {
            GuildConfig config = new GuildConfig();
            config.birthdayChannel = birthdayChannel.getIdLong();
            ObjectManager.guildConfigs.put(birthdayChannel.getGuild().getIdLong(), config);
        }

        ObjectManager.save(ObjectManager.guildConfigs, "guildConfigs");
        event.reply("Birthday channel successfully set!").queue();
    }

    @RequireOutOfDMs
    @RequiredPermissions(permissions = Permission.ADMINISTRATOR)
    @Subcommand(name = "pingeveryone", description = "Whether or not the bot should ping everyone on someone's birthday")
    public static void handlePingEveryone(SlashCommandEvent event,
                                          @CommandParameter(name = "shouldping", description = "true if yes, or false if no") boolean shouldPing) {
        if (shouldPing && !PermissionUtil.checkPermission(event.getGuild().getSelfMember(), Permission.MESSAGE_MENTION_EVERYONE)) {
            event.reply("I do not have permission to ping everyone!").queue();
            return;
        }

        if (ObjectManager.guildConfigs.containsKey(event.getGuild().getIdLong())) {
            GuildConfig config = ObjectManager.guildConfigs.get(event.getGuild().getIdLong());
            config.pingEveryoneOnBirthday = shouldPing;
            ObjectManager.guildConfigs.replace(event.getGuild().getIdLong(), config);
        } else {
            GuildConfig config = new GuildConfig();
            config.pingEveryoneOnBirthday = shouldPing;
            ObjectManager.guildConfigs.put(event.getGuild().getIdLong(), config);
        }

        ObjectManager.save(ObjectManager.guildConfigs, "guildConfigs");
        event.reply("Should ping successfully set!").queue();
    }
}
