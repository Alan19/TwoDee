package logic;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;
import util.RandomColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DamageLogic implements VelenHybridHandler {

    public static void setupDamageCommand(Velen velen) {
        final List<SlashCommandOptionBuilder> options = new ArrayList<>();
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.STRING)
                .setName("type")
                .setDescription("the type of damage being inflicted")
                .addChoice("stun", "stun")
                .addChoice("basic", "basic")
                .addChoice("wound", "wound")
                .setRequired(true));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("count")
                .setDescription("the amount of damage that is being inflicted")
                .setRequired(true));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("stun-armor")
                .setDescription("the amount of stun armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("basic-armor")
                .setDescription("the amount of basic armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("wound-armor")
                .setDescription("the amount of wound armor you have"));
        options.add(new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("resilience")
                .setDescription("the amount of resilience you have"));

        DamageLogic logic = new DamageLogic();

        VelenCommand.ofHybrid("damage", "calculates the damage of each type received from an attack", velen, logic)
                .addOptions(options.toArray(new SlashCommandOptionBuilder[0]))
                .addFormats("~damage :[count:of(integer)] :[type:of(string)] :[stun-armor:of(integer)] :[basic-armor:of(integer)] :[wound-armor:of(integer)] :[resilience:of(integer)]")
                .attach();
    }

    /**
     * Creates an embed that specifies the amount of points to mark off
     *
     * @param resilienceSpent The amount of resilience spent to blunt the damage
     * @param stun            The amount of stun suffered from the attack
     * @param wounds          The amount of wounds suffered from the attack
     * @return An embed that specifies the amount of points for resilience, stun, and wounds to mark off
     */
    private EmbedBuilder createDamageEmbed(long resilienceSpent, long stun, long wounds) {
        // TODO Add more lines
        final EmbedBuilder embed = new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setTitle("Ouch! That hurt!")
                .setDescription("Mark off the following from your character sheet:");
        if (resilienceSpent != 0) {
            embed.addField("Resilience", String.valueOf(resilienceSpent));
        }
        if (stun != 0) {
            embed.addField("Stun", String.valueOf(stun));
        }
        if (wounds != 0) {
            embed.addField("Wounds", String.valueOf(wounds));
        }
        return embed;
    }

    /**
     * Calculates the amount of damage inflicted after factoring in resilience and armor
     *
     * @param type       The type of damage being inflicted
     * @param count      The amount of damage being inflicted
     * @param resilience The amount of resilience you have
     * @param stunArmor  Your stun armor rating
     * @param woundArmor Your wound armor rating
     * @return A triple that contains the amount of resilience used on the left, the amount of stun inflicted in the middle, and the amount of wounds inflicted on the right
     */
    public Triple<Long, Long, Long> calculateDamage(DamageType type, long count, long resilience, long stunArmor, long woundArmor) {
        // Resilience affects the damage count directly, before armor
        final Pair<Long, Long> damage = splitDamage(type, count - resilience);
        long resilienceUsed = Math.min(count, resilience);
        long stunMitigated = Math.min(damage.getLeft(), stunArmor);
        long woundMitigated = Math.min(damage.getRight(), woundArmor);
        return Triple.of(resilienceUsed, damage.getLeft() - stunMitigated, damage.getRight() - woundMitigated);
    }

    /**
     * Returns damage inflicted as a pair, in order store the amount of damage inflicted of each type when basic damage is inflicted
     *
     * @param type  The type of damage being inflicted
     * @param count The amount of damage being inflicted
     * @return A pair that contains the amount of stun on the left, and the amount of wounds on the right
     */
    public Pair<Long, Long> splitDamage(DamageType type, long count) {
        if (type == DamageType.STUN) {
            return Pair.of(count, 0L);
        }
        else if (type == DamageType.WOUND) {
            return Pair.of(0L, count);
        }
        else {
            return Pair.of((long) Math.ceil((double) count / 2), count / 2);
        }
    }

    /**
     * Sends an ephemeral embed to the user that contains the amount of points to mark off after taking damage
     */
    @Override
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final Optional<String> typeOptional = args.withName("type").flatMap(VelenOption::asString);
        final Optional<Long> countOptional = args.withName("count").flatMap(VelenOption::asLong);
        if (typeOptional.isPresent() && countOptional.isPresent()) {
            // Get params
            final DamageType type = DamageType.getType(typeOptional.get());
            long damageCount = countOptional.get();
            long stunArmor = args.withName("stun-armor").flatMap(VelenOption::asLong).orElse(0L);
            long basicArmor = args.withName("basic-armor").flatMap(VelenOption::asLong).orElse(0L);
            long woundArmor = args.withName("wound-armor").flatMap(VelenOption::asLong).orElse(0L);
            long resilience = args.withName("resilience").flatMap(VelenOption::asLong).orElse(0L);
            final long effectiveStunArmor = Math.max(stunArmor, (long) Math.ceil((double) basicArmor / 2));
            final long effectiveWoundArmor = Math.max(woundArmor, basicArmor / 2);
            final Triple<Long, Long, Long> damageTriple = calculateDamage(type, damageCount, resilience, effectiveStunArmor, effectiveWoundArmor);
            final EmbedBuilder embed = createDamageEmbed(damageTriple.getLeft(), damageTriple.getMiddle(), damageTriple.getRight());
            responder.addEmbed(embed).setFlags(MessageFlag.EPHEMERAL).respond();
        }
        else {
            responder.setContent("invalid damage type and / or count!").setFlags(MessageFlag.EPHEMERAL).respond();
        }

    }

    public enum DamageType {
        STUN, BASIC, WOUND;

        static DamageType getType(String string) {
            return switch (string) {
                case "stun" -> STUN;
                case "basic" -> BASIC;
                case "wound" -> WOUND;
                default -> throw new IllegalStateException("Unexpected value: " + string);
            };
        }
    }
}
