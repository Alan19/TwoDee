package logic;

import com.vdurmont.emoji.EmojiParser;
import configs.Settings;
import listeners.QuoteButtonListener;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.ButtonStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOption;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;
import util.DiscordHelper;
import util.RandomColor;
import util.UtilFunctions;

public class QuoteLogic implements VelenHybridHandler {
    public static void setupQuoteLogic(Velen velen) {
        VelenCommand.ofHybrid("quote", "Adds a new quote!", velen, new QuoteLogic())
                .addFormats("~quote :[quote-text:of(string):hasMany()]")
                .addOption(SlashCommandOption.createStringOption("quote-text", "the text for the quote", true))
                .attach();
    }

    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final String quoteText = args.getManyWithName("quote-text").orElseThrow(IllegalStateException::new);
        Settings.addQuote(quoteText);
        responder.addEmbed(new EmbedBuilder()
                        .setTitle("Added the following quote to the rolling quotes list:")
                        .setDescription(quoteText)
                        .setFooter("Requested by " + UtilFunctions.getUsernameInChannel(user, event.getChannel()), DiscordHelper.getLocalAvatar(event, user))
                        .setColor(RandomColor.getRandomColor()))
                .addActionRow(
                        Button.create("quote-confirm", ButtonStyle.PRIMARY, "Confirm", EmojiParser.parseToUnicode(":white_check_mark:")),
                        Button.create("quote-undo", ButtonStyle.DANGER, "Undo", EmojiParser.parseToUnicode(":leftwards_arrow_with_hook:"))
                )
                .respond();
        event.getChannel().getApi().addListener(new QuoteButtonListener(quoteText));
    }
}
