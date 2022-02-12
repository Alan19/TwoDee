package logic;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import sheets.SheetsHandler;
import util.RandomColor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RollPoolLogic implements VelenEvent, VelenSlashEvent {
    public static void setupPoolCommand(Velen velen) {
        RollPoolLogic rollPoolLogic = new RollPoolLogic();
        List<SlashCommandOption> commandOptions = new ArrayList<>();

        List<SlashCommandOption> rollOptions = new ArrayList<>();
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "bonuses", "Bonus dice to add to the pool", false));
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.NUMBER, "discount", "The number of plot points to discount (negative results in a plot point cost increase).", false));
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.NUMBER, "dice-kept", "The number of dice kept. Keeps two dice by default.", false));
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "enhanceable", "Allows the roll to be enhanced after the roll.", false));

        List<SlashCommandOption> saveOptions = new ArrayList<>();
        saveOptions.add(new SlashCommandOptionBuilder().setName("save-type").setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("The save pool to roll, with a choice of initiative, vitality, and willpower").addChoice("initiative", "Initiative").addChoice("vitality", "Vitality").addChoice("willpower", "Willpower").build());
        saveOptions.addAll(rollOptions);
        commandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "save", "Rolls a derived attribute roll with opportunities disabled", saveOptions));

        List<SlashCommandOption> poolOptions = new ArrayList<>();
        poolOptions.add(new SlashCommandOptionBuilder().setName("pool-name").setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("The named pool to roll from the character sheet").build());
        poolOptions.addAll(rollOptions);
        poolOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "opportunity", "Allows for opportunities on the roll. Defaults to true.", false));
        commandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "saved-pool", "Rolls a saved dice pool in your saved dice pools sheet or combat tracker", poolOptions));

        commandOptions.add(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "query", "Query your saved pools"));

        VelenCommand.ofHybrid("roll-pool", "Rolls a predefined dice pool from your character sheet", velen, rollPoolLogic, rollPoolLogic)
                .addShortcuts("rollp", "testp")
                .addOptions(commandOptions.toArray(new SlashCommandOption[0]))
                .setServerOnly(true, 817619574450028554L)
                .attach();
    }


    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        if (args.length > 0) {
            // TODO Verify pool support when Andy's done
            String poolName = args[0];
            String bonus = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
            if (Stream.of("vitality", "initiative", "willpower").anyMatch(poolName::equalsIgnoreCase)) {
                SheetsHandler.getSavePool(StringUtils.capitalize(poolName.toLowerCase()), user)
                        .map(defaultPool -> MessageFormat.format("{0} {1}", defaultPool, bonus))
                        .onFailure(throwable -> event.getChannel().sendMessage(throwable.getMessage()))
                        .onSuccess(dicePool -> RollLogic.handleTextCommandRoll(user, event.getChannel(), dicePool, false));
            }
            else {
                SheetsHandler.getSavedPool(poolName.toLowerCase(), user)
                        .map(defaultPool -> MessageFormat.format("{0} {1}", defaultPool, bonus))
                        .onFailure(throwable -> event.getChannel().sendMessage(throwable.getMessage()))
                        .onSuccess(dicePool -> RollLogic.handleTextCommandRoll(user, event.getChannel(), dicePool, false));
            }
        }
        else {
            event.getChannel().sendMessage("save type not found!");
        }
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final SlashCommandInteractionOption subcommandOption = event.getOptions().get(0);
        String mode = subcommandOption.getName();
        final Optional<String> bonuses = event.getOptionStringValueByName("bonuses");
        final Integer discount = subcommandOption.getOptionLongValueByName("discount").map(Math::toIntExact).orElse(0);
        final Integer diceKept = subcommandOption.getOptionLongValueByName("dice-kept").map(Math::toIntExact).orElse(2);
        final Boolean enhanceable = subcommandOption.getOptionBooleanValueByName("enhanceable").orElse(null);
        if (mode.equals("save")) {
            //noinspection OptionalGetWithoutIsPresent
            SheetsHandler.getSavePool(subcommandOption.getOptionStringValueByName("save-type").get(), user)
                    .map(defaultPool -> MessageFormat.format("{0} {1}", defaultPool, bonuses.orElse("")))
                    .onFailure(throwable -> event.createImmediateResponder().setContent(throwable.getMessage()).setFlags(InteractionCallbackDataFlag.EPHEMERAL).respond())
                    .onSuccess(dicePool -> RollLogic.handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, false));
        }
        else if (mode.equals("query")) {
            event.respondLater(true).thenAccept(updater -> SheetsHandler.getSavedPools(user).onSuccess(strings -> {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Your list of saved pools")
                        .setColor(RandomColor.getRandomColor())
                        .setAuthor(user);
                strings.forEach(pair -> builder.addField(pair.getLeft(), pair.getRight()));
                updater.addEmbed(builder).update();
            }).onFailure(throwable -> updater.setContent(throwable.getMessage()).update()));
        }
        else {
            Boolean opportunity = subcommandOption.getOptionBooleanValueByName("opportunity").orElse(true);
            //noinspection OptionalGetWithoutIsPresent
            SheetsHandler.getSavedPool(subcommandOption.getOptionStringValueByName("pool-name").get(), user)
                    .map(defaultPool -> MessageFormat.format("{0} {1}", defaultPool, bonuses.orElse("")))
                    .onFailure(throwable -> event.createImmediateResponder().setContent(throwable.getMessage()).setFlags(InteractionCallbackDataFlag.EPHEMERAL).respond())
                    .onSuccess(dicePool -> RollLogic.handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, opportunity));
        }
    }
}
