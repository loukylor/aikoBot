package dev.loukylor.aiko;

import dev.loukylor.aiko.hooks.*;
import dev.loukylor.aiko.utils.Tuple;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.internal.entities.RoleImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class EventHandler extends ListenerAdapter {
    private static void createCommands(Reflections reflections) {
        // command name: commandData
        HashMap<String, CommandData> finishedCommands = new HashMap<>();

        // Get all methods with the command annotation in the functions package
        Set<Method> commands = reflections.getMethodsAnnotatedWith(Command.class);

        for (Method command : commands) {
            Command commandAnnotation = command.getAnnotation(Command.class);

            // Create the initial command
            CommandData currentData = new CommandData(commandAnnotation.name(), commandAnnotation.description());
            // Create command parameters based off params of method and skip the first param as it's the event
            for (Parameter param : Arrays.copyOfRange(command.getParameters(), 1, command.getParameterCount())) {
                CommandParameter commandParam = param.getAnnotation(CommandParameter.class);
                currentData.addOption(
                        convertParamTypeToOptionType(command, param),
                        commandParam.name(),
                        commandParam.description(),
                        commandParam.isRequired()
                );
            }

            cachedCommands.put(commandAnnotation.name(), new CachedCommand(command));
            finishedCommands.put(commandAnnotation.name(), currentData);
        }

        // Subcommand group name: SubcommandGroupData
        HashMap<String, SubcommandGroupData> finishedSubcommandGroups = new HashMap<>();

        // Get all methods with the subcommand annotation in the functions package
        Set<Method> subcommands = reflections.getMethodsAnnotatedWith(Subcommand.class);

        for (Method subcommand : subcommands) {
            Subcommand subcommandAnnotation = subcommand.getAnnotation(Subcommand.class);

            // Get the subcommand group of the subcommand as well as the containing command
            SubcommandGroup subcommandGroupAnnotation = subcommand.getAnnotation(SubcommandGroup.class);
            if (subcommandGroupAnnotation == null)
                subcommandGroupAnnotation = subcommand.getDeclaringClass().getAnnotation(SubcommandGroup.class);

            Command parentCommandAnnotation = subcommand.getAnnotation(Command.class);
            if (parentCommandAnnotation == null)
                parentCommandAnnotation = subcommand.getDeclaringClass().getAnnotation(Command.class);

            CommandData parentCommand;
            HashMap<String, Object> cachedCommand;
            if (finishedCommands.containsKey(parentCommandAnnotation.name())) {
                parentCommand = finishedCommands.get(parentCommandAnnotation.name());
                cachedCommand = (HashMap<String, Object>) cachedCommands.get(parentCommand.getName());
            } else {
                parentCommand = new CommandData(parentCommandAnnotation.name(), parentCommandAnnotation.description());
                finishedCommands.put(parentCommand.getName(), parentCommand);

                cachedCommand = new HashMap<>();
                cachedCommands.put(parentCommand.getName(), cachedCommand);
            }

            SubcommandData subcommandData = new SubcommandData(subcommandAnnotation.name(), subcommandAnnotation.description());

            // Create command parameters based off params of method // Skip first arg as that's the event arg
            for (Parameter param : Arrays.copyOfRange(subcommand.getParameters(), 1, subcommand.getParameterCount())) {
                CommandParameter commandParam = param.getAnnotation(CommandParameter.class);
                subcommandData.addOption(
                        convertParamTypeToOptionType(subcommand, param),
                        commandParam.name(),
                        commandParam.description(),
                        commandParam.isRequired()
                );
            }

            // if the group is null then just make it a subcommand
            // else assign a group
            if (subcommandGroupAnnotation == null) {
                parentCommand.addSubcommands(subcommandData);
                cachedCommand.put(subcommandAnnotation.name(), new CachedCommand(subcommand));
            } else {
                // if it's not in the finished subcommand then add a new one
                if (!finishedSubcommandGroups.containsKey(subcommandGroupAnnotation.name())) {
                    SubcommandGroupData currentSubcommandGroup = new SubcommandGroupData(subcommandGroupAnnotation.name(), subcommandGroupAnnotation.description());

                    finishedSubcommandGroups.put(subcommandGroupAnnotation.name(), currentSubcommandGroup);
                    parentCommand.addSubcommandGroups(currentSubcommandGroup);

                    cachedCommand.put(currentSubcommandGroup.getName(), new HashMap<String, Object>());
                }

                finishedSubcommandGroups.get(subcommandGroupAnnotation.name()).addSubcommands(subcommandData);
                ((HashMap<String, Object>) cachedCommand.get(subcommandGroupAnnotation.name())).put(subcommandData.getName(), new CachedCommand(subcommand));
            }
        }

        Main.getApi().updateCommands().addCommands(finishedCommands.values()).queue();
    }

    // Converts a parameter's type to the OptionType enum
    private static OptionType convertParamTypeToOptionType(Method method, Parameter parameter)
            throws IllegalArgumentException
    {
        Class<?> type = (Class<?>) parameter.getParameterizedType();

        if (type.equals(String.class))
            return OptionType.STRING;
        else if (type.equals(long.class) || type.equals(int.class))
            return OptionType.INTEGER;
        else if (type.equals(boolean.class))
            return OptionType.BOOLEAN;
        else if (type.equals(User.class))
            return OptionType.USER;
        else if (type.equals(GuildChannel.class))
            return OptionType.CHANNEL;
        else if (RoleImpl.class.isAssignableFrom(type)) // Check if type is the role interface by checking if roleimpl supers it
            return OptionType.ROLE;
        else if (type.isAssignableFrom(IMentionable.class)) // check if type is an imentionable if it supers it
            return OptionType.MENTIONABLE;
        else if (type.equals(double.class) || type.equals(float.class))
            return OptionType.NUMBER;
        else
            throw new IllegalArgumentException(String.format("The parameter %s on method %s has an incompatible type of %s.", parameter.getName(), method.getName(), ((Class<?>) parameter.getParameterizedType()).getName()));
    }

    private static final HashMap<String, Object> cachedCommands = new HashMap<>();
    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        try {
            if (cachedCommands.containsKey(event.getName())) {
                Object value = cachedCommands.get(event.getName());
                // If it's just a non nested command
                if (value instanceof CachedCommand) {
                    runCommand(event, value);
                } else {
                    HashMap<String, ?> subCommand = (HashMap<String, ?>)value;
                    // If it's not in a group
                    if (event.getSubcommandGroup() != null)
                        runCommand(event, ((HashMap<String, ?>) subCommand.get(event.getSubcommandGroup())).get(event.getSubcommandName()));
                    // It has to just be a subcommand
                    else
                        runCommand(event, subCommand.get(event.getSubcommandName()));
                }
            } else {
                throw new IllegalArgumentException("Could not find command with name: " + event.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runCommand(SlashCommandEvent event, Object command)
    {
        // check if you have all the required permissions
        CachedCommand currentCommand = ((CachedCommand) command);
        if (event.isFromGuild()) {
            for (Permission permission : currentCommand.requiredPermissions) {
                if (!PermissionUtil.checkPermission(event.getGuildChannel(), event.getMember(), permission)) {
                    event.reply(String.format("You lack the permission `%s` to run that command!", permission.getName())).queue();
                    return;
                }
            }
        }

        // Create params array with the event as the first arg
        ArrayList<Object> params = new ArrayList<>();
        params.add(event);

        for (OptionMapping option : event.getOptions())
            params.add(processOption(option));

        try {
            currentCommand.method.invoke(null, params.toArray());
        } catch (Exception ex) {
            event.reply("Oops! Looks like the bot errored while running your command.").queue();
            ex.printStackTrace();
        }
    }

    private static Object processOption(OptionMapping option)
        throws IllegalArgumentException
    {
        switch (option.getType()) {
            case STRING:
                return option.getAsString();
            case INTEGER:
                return option.getAsLong();
            case BOOLEAN:
                return option.getAsBoolean();
            case USER:
                // Just assume it's always user so no errors and things.
                return option.getAsUser();
            case CHANNEL:
                return option.getAsGuildChannel();
            case ROLE:
                return option.getAsRole();
            case MENTIONABLE:
                return option.getAsMentionable();
            case NUMBER:
                return option.getAsDouble();
            default:
                throw new IllegalArgumentException("I don't know how, but the option didn't match any types");
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackage("dev.loukylor.aiko.functions", ClassLoader.getSystemClassLoader())
                        .addScanners(Scanners.MethodsAnnotated, Scanners.TypesAnnotated, Scanners.SubTypes)
        );

        startMainLoop(reflections);
        createCommands(reflections);
    }

    private static final ArrayList<Tuple<Object, Method>> mainLoopListeners = new ArrayList<>();
    private static void startMainLoop(Reflections reflections) {
        Set<Class<? extends MainLoopListener>> listenerImplementations = reflections.getSubTypesOf(MainLoopListener.class);
        for (Class<? extends MainLoopListener> listeners : listenerImplementations) {
            try {
                mainLoopListeners.add(new Tuple<>(listeners.getConstructor().newInstance(), listeners.getMethod("onMainLoop")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Runnable loop = () ->
        {
            while (true) {
                try {
                    Thread.sleep(250);

                    for (Tuple<Object, Method> listener : mainLoopListeners)
                        listener.y.invoke(listener.x);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(loop).start();
    }
}

// Here just in case I need to cache more data
class CachedCommand {
    final Method method;
    final Permission[] requiredPermissions;
    final boolean requireOutOfDMs;

    public CachedCommand(Method method)
    {
        this.method = method;

        RequirePermissions requirePermissionsAnnotation = method.getAnnotation(RequirePermissions.class);
        requiredPermissions = requirePermissionsAnnotation == null ? new Permission[0] : requirePermissionsAnnotation.permissions();

        requireOutOfDMs = method.getAnnotation(RequireOutOfDMs.class) != null;
    }
}