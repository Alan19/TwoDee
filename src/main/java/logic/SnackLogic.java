package logic;

import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.*;
import util.RandomColor;

import java.text.MessageFormat;
import java.util.*;

public class SnackLogic implements VelenSlashEvent, VelenEvent {

    private static Map<String, String> snacks;

    static {
        Map<String, String> snacks = new HashMap<>();
        snacks.put("cookie", "https://upload.wikimedia.org/wikipedia/commons/5/5c/Choc-Chip-Cookie.png");
        snacks.put("wasabi pea", "https://cdn.shopify.com/s/files/1/0286/6970/6288/products/ferris-wasabi-peas-01_1000x1000.png?v=1605023869");
        snacks.put("chip", "https://www.hobza.cz/assets/images/chips_decoration/intro_chips1.png");
        snacks.put("popcorn", "https://preview.redd.it/6v2n5xa3xgk11.png?auto=webp&s=38993ac24ac9e92958e09f6141ec746dc71ef8b2");
        snacks.put("pizza", "https://saucencheese.com/media/catalog/product/cache/1/image/9df78eab33525d08d6e5fb8d27136e95/s/l/slice.png");
        snacks.put("sandwich", "https://creamerynovelties.com/wp-content/uploads/2021/02/Ice-Cream-Sandwich-600x600-1.png");
        snacks.put("pretzel", "https://images.squarespace-cdn.com/content/v1/5ce2e46734cc4f000122f0fa/1560345863169-W1KZ7GPTDMI9NCI71UZN/MPretzels_Solo-XDark_crop+copy.png?format=500w");
        setSnacks(snacks);
    }

    private final Random random;

    public SnackLogic() {
        random = new Random();
    }

    public static void setupSnackCommand(Velen velen) {
        SnackLogic snackLogic = new SnackLogic();
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.USER, "recipient", "the user to give the snack to", false));
        options.add(SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "snack-type", "the snack to give, defaults to a cookie", false, snacks.keySet().stream().map(s -> new SlashCommandOptionChoiceBuilder().setName(s).setValue(s)).toArray(SlashCommandOptionChoiceBuilder[]::new)));
        VelenCommand.ofHybrid("snack", "Gives you a snack!", velen, snackLogic, snackLogic).addOptions(options.toArray(new SlashCommandOption[0])).attach();
    }

    public static void setSnacks(Map<String, String> snacks) {
        SnackLogic.snacks = snacks;
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        new MessageBuilder().addEmbed(getSnackEmbed(user, getRandomSnack(), user)).send(event.getChannel());
    }

    private Pair<String, String> getRandomSnack() {
        final int index = random.nextInt(snacks.size());
        return snacks.entrySet().stream().skip(index).findFirst().map(stringStringEntry -> Pair.of(stringStringEntry.getKey(), stringStringEntry.getValue())).orElseGet(() -> Pair.of("cookie", "https://upload.wikimedia.org/wikipedia/commons/5/5c/Choc-Chip-Cookie.png"));
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Pair<String, String> snackType = event.getOptionStringValueByName("snack-type").map(s -> Pair.of(Pair.of(s, snacks.get(s)))).orElse(getRandomSnack());
        final User recipient = event.getFirstOptionUserValue().orElse(user);
        firstResponder.addEmbed(getSnackEmbed(user, snackType, recipient)).respond();
    }

    // TODO get username from channel
    private EmbedBuilder getSnackEmbed(User user, Pair<String, String> selectedSnack, User recipient) {
        String content = MessageFormat.format("Here is a {0}!", selectedSnack.getKey());
//        if (Storytellers.isUserStoryteller(user)) {
//            content += "\nThe Storyteller's gift gave " + recipient.getName() + " a plot point!";
//            PlotPointUtils.addPlotPointsToUser(recipient, 1);
//        }
        return new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setTitle(MessageFormat.format("{0} is giving a snack to {1}!", user.getName(), recipient.getName()))
                .setDescription(content)
                .setFooter("Requested by " + user.getName(), user.getAvatar())
                .setImage(selectedSnack.getValue());
    }
}
