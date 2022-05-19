package logic;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.objects.interfaces.VelenCommonsArguments;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;
import sheets.SheetsHandler;
import util.RandomColor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RollPoolLogic implements VelenHybridHandler {

    public static final String POOL_NAME = "pool-name";

    public static void setupPoolCommand(Velen velen) {
        RollPoolLogic rollPoolLogic = new RollPoolLogic();
        List<SlashCommandOption> commandOptions = new ArrayList<>();

        List<SlashCommandOption> rollOptions = new ArrayList<>();
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "bonuses", "Bonus dice to add to the pool", false));
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "discount", "The number of plot points to discount (negative results in a plot point cost increase).", false));
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "dice-kept", "The number of dice kept. Keeps two dice by default.", false));
        rollOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "enhanceable", "Allows the roll to be enhanced after the roll.", false));

        List<SlashCommandOption> saveOptions = new ArrayList<>();
        saveOptions.add(new SlashCommandOptionBuilder().setName("save-type").setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("The save pool to roll, with a choice of initiative, vitality, and willpower").addChoice("initiative", "Initiative").addChoice("vitality", "Vitality").addChoice("willpower", "Willpower").build());
        saveOptions.addAll(rollOptions);
        commandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "save", "Rolls a derived attribute roll with opportunities disabled", saveOptions));

        List<SlashCommandOption> poolOptions = new ArrayList<>();
        poolOptions.add(new SlashCommandOptionBuilder().setName(POOL_NAME).setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("The named pool to roll from the character sheet").setAutocompletable(true).build());
        poolOptions.addAll(rollOptions);
        poolOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "opportunity", "Allows for opportunities on the roll. Defaults to true.", false));
        commandOptions.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "saved-pool", "Rolls a saved dice pool in your saved dice pools sheet or combat tracker", poolOptions));

        commandOptions.add(SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "query", "Query your saved pools"));

        VelenCommand.ofHybrid("roll-pool", "Rolls a predefined dice pool from your character sheet", velen, rollPoolLogic)
                .addShortcuts("rollp")
                .addFormats("roll-pool :[pool:of(string)] :[bonuses:of(string):hasMany()]",
                        "roll-pool :[pool:of(string)]",
                        "roll-pool query")
                .addOptions(commandOptions.toArray(new SlashCommandOption[0]))
                .attach();
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        VelenCommonsArguments arguments = event.isMessageEvent() ? args : args.get(0).asSubcommand();
        boolean isQuery = event.isMessageEvent() ? args.get(1).asString().orElseThrow(IllegalStateException::new).equals("query") : args.get(0).asSubcommand().getName().equals("query");
        String pool = event.isMessageEvent() ? arguments.getManyWithName("pool").orElseThrow(IllegalStateException::new) : arguments.get(0).asString().orElseThrow(IllegalStateException::new);
        String bonuses = arguments.getManyWithName("bonuses").orElse("");
        final Integer discount = arguments.withName("discount").flatMap(VelenOption::asInteger).orElse(0);
        final Integer diceKept = arguments.withName("dice-kept").flatMap(VelenOption::asInteger).orElse(2);
        final Boolean enhanceable = arguments.withName("enhanceable").flatMap(VelenOption::asBoolean).orElse(null);
        if (isQuery) {
            SheetsHandler.getSavedPools(user).onSuccess(strings -> {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("Your list of saved pools")
                        .setColor(RandomColor.getRandomColor())
                        .setAuthor(user);
                strings.forEach(pair -> builder.addField(pair.getLeft(), pair.getRight()));
                responder.addEmbed(builder).respond();
            }).onFailure(throwable -> responder.setContent(throwable.getMessage()).respond());
        }
        else if (Stream.of("vitality", "initiative", "willpower").anyMatch(pool::equalsIgnoreCase)) {
            SheetsHandler.getSavePool(StringUtils.capitalize(pool.toLowerCase()), user)
                    .map(defaultPool -> MessageFormat.format("{0} {1}", defaultPool, bonuses))
                    .onFailure(throwable -> event.getChannel().sendMessage(throwable.getMessage()))
                    .onSuccess(dicePool -> RollLogic.handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, false));
        }
        else {
            Boolean opportunity = arguments.withName("opportunity").flatMap(VelenOption::asBoolean).orElse(true);
            SheetsHandler.getSavedPool(pool.toLowerCase(), user)
                    .map(defaultPool -> MessageFormat.format("{0} {1}", defaultPool, bonuses))
                    .onFailure(throwable -> event.getChannel().sendMessage(throwable.getMessage()))
                    .onSuccess(dicePool -> RollLogic.handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, opportunity));
        }
    }
}
