package dev.loukylor.aiko.functions.birthdays;

import dev.loukylor.aiko.Main;
import dev.loukylor.aiko.ObjectManager;
import dev.loukylor.aiko.entities.Birthday;
import dev.loukylor.aiko.entities.GuildConfig;
import dev.loukylor.aiko.hooks.MainLoopListener;
import dev.loukylor.aiko.utils.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MainLoop implements MainLoopListener {
    private static int timeSlept = 0;
    @Override
    public void onMainLoop() {
        timeSlept += 250;

        LocalDate now = LocalDate.now();
        for (Birthday birthday : ObjectManager.birthdays.values())
            // Should cover things that may have happened in downtime
            if (birthday.whenCelebrate.isBefore(now))
                processUser(Objects.requireNonNull(Main.getApi().getUserById(birthday.user)), birthday, now);

        // Save after ~10 seconds
        if (timeSlept % 10000 == 0) {
            ObjectManager.save(ObjectManager.birthdays, "birthdays");
            timeSlept = 0;
        }
    }

    private static void processUser(User user, Birthday birthday, LocalDate now) {
        if (user.getMutualGuilds().isEmpty())
            return;

        // create initial embed
        EmbedBuilder builder = new EmbedBuilder()
                .setThumbnail(user.getAvatarUrl())
                .setAuthor(user.getAsTag(), user.getAvatarUrl())
                .setColor(Color.white);

        // Send message into all mutual guilds
        for (Guild mutual : user.getMutualGuilds()) {
            // customize for nicknames and things
            Member userAsMember = mutual.getMember(user);
            assert userAsMember != null;

            builder.clearFields();
            builder.setTitle(String.format("Happy %s birthday %s!",
                             StringUtils.addSuffixToNumber(ChronoUnit.YEARS.between(birthday.date, now)),
                             userAsMember.getEffectiveName()));

            // If they have any messages, add em as fields
            if (!birthday.customMessages.isEmpty()) {
                for (Map.Entry<Long, String> message : birthday.customMessages.entrySet()) {
                    Member messageUser = mutual.getMemberById(message.getKey());
                    if (messageUser == null)
                        continue;

                    // Break if the field count is at max
                    if (builder.getFields().size() >= 25) {
                        builder.setDescription("(Some messages had to be excluded due to limitations)");
                        break;
                    }

                    builder.addField(
                            String.format("%s says:", messageUser.getEffectiveName()),
                            message.getValue(),
                            false);
                }
            }

            GuildConfig guildConfig = ObjectManager.guildConfigs.get(mutual.getIdLong());
            Message customizedMessage = determineIfShouldPing(guildConfig, user, builder);

            if (guildConfig == null) {
                // Send it to the default channel if there is no guild config
                mutual.getDefaultChannel().sendMessage(customizedMessage).queue();
            } else {
                TextChannel channel = mutual.getTextChannelById(guildConfig.birthdayChannel);
                // or it cant find the channel
                if (channel == null)
                    mutual.getDefaultChannel().sendMessage(customizedMessage).queue();
                else
                    channel.sendMessage(customizedMessage).queue();
            }
        }

        birthday.whenCelebrate = birthday.whenCelebrate.plusYears(1);
        ObjectManager.birthdays.replace(birthday.user, birthday);
        ObjectManager.save(ObjectManager.birthdays, "birthdays");
    }

    private static Message determineIfShouldPing(GuildConfig config, User user, EmbedBuilder builder) {
        if (config == null || !config.pingEveryoneOnBirthday) {
            return new MessageBuilder()
                    .setContent(user.getAsMention())
                    .setEmbeds(builder.build())
                    .build();
        } else {
            return new MessageBuilder()
                    .setContent("@everyone")
                    .setEmbeds(builder.build())
                    .build();
        }
    }
}
