package logic;

import advancement.GeneralSkillCalculator;
import advancement.SpecialtySkill;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenArguments;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenSlashEvent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdvancementLogic implements VelenSlashEvent {
    public static void setupAdvancementCommand(Velen velen) {
        final List<SlashCommandOption> options = new ArrayList<>();
        final SlashCommandOptionBuilder startingArray = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.STRING)
                .setName("starting-array")
                .setDescription("The current array of specialty skills, e.g. use 4 6 8 if you have a d4 d6 d8")
                .setRequired(false);
        final SlashCommandOptionBuilder targetFacets = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("target-facets")
                .setDescription("The amount of facets to target for the general skill")
                .setRequired(true);
        final SlashCommandOptionBuilder targetExpert = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.BOOLEAN)
                .setName("target-expert")
                .setDescription("Attempts to ensure at least one specialty skill reaches d14, defaults to false")
                .setRequired(false);
        final SlashCommandOptionBuilder minimumFacets = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("minimum-facets")
                .setDescription("Ensures as many specialty skills as possible are above the specified facet count, defaults to 12")
                .setRequired(false);
        final SlashCommandOptionBuilder nervewrightMana = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.LONG)
                .setName("nervewright-mana")
                .setDescription("Specifies the amount of mana that can be invested for Nervewright, defaults to 0")
                .setRequired(false);
        final SlashCommandOptionBuilder adolescentInterests = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.BOOLEAN)
                .setName("adolescent-interests")
                .setDescription("Set to true to factor in the effects of adolescent interests, defaults to false")
                .setRequired(false);
        final SlashCommandOptionBuilder nonEphemeral = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.BOOLEAN)
                .setName("non-ephemeral")
                .setDescription("Set to true to send the message to the channel, defaults to false")
                .setRequired(false);
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "tall", "Calculates the skill array to reach a given general skill by going tall.", targetFacets, startingArray, targetExpert, minimumFacets, nervewrightMana, adolescentInterests, nonEphemeral));
        options.add(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "wide", "Calculates the skill array to reach a given general skill facet as cheaply as possible.", targetFacets, startingArray, targetExpert, nervewrightMana, adolescentInterests, nonEphemeral));
        VelenCommand.ofSlash("advancement", "Calculates the AP cost to get a general skill", velen, new AdvancementLogic())
                .addOptions(options.toArray(new SlashCommandOption[]{}))
                .setServerOnly(true, 468046159781429250L)
                .attach();
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final SlashCommandInteractionOption subcommandOption = event.getOptions().get(0);
        final Optional<Long> targetFacets = subcommandOption.getOptionLongValueByName("target-facets");
        if (targetFacets.isPresent()) {
            final Boolean expert = subcommandOption.getOptionBooleanValueByName("target-expert").orElse(false);
            final List<Integer> startingArray = subcommandOption.getOptionStringValueByName("starting-array")
                    .map(s -> Arrays.stream(s.split(" ")).map(Integer::parseInt).collect(Collectors.toList()))
                    .orElse(new ArrayList<>());
            final long minimumFacets = subcommandOption.getOptionLongValueByName("minimum-facets").orElse(12L);
            final boolean tall = subcommandOption.getName().equals("tall");
            final Boolean adolescentInterests = subcommandOption.getOptionBooleanValueByName("adolescent-interests").orElse(false);
            final long nervewrightMana = subcommandOption.getOptionLongValueByName("nervewright-mana").orElse(0L);
            final boolean nonEphemeral = subcommandOption.getOptionBooleanValueByName("non-ephemeral").orElse(false);

            final Long targetFacetsActual = targetFacets.get();
            List<SpecialtySkill> skills = new GeneralSkillCalculator(startingArray, expert, tall, minimumFacets, adolescentInterests, nervewrightMana).generate(targetFacetsActual);
            EmbedBuilder resultsEmbed = new EmbedBuilder();
            resultsEmbed.setTitle("How to get this general skill to f" + targetFacetsActual);
            resultsEmbed.setDescription("In order to advance this general skill and go " + subcommandOption.getName() + ", you can raise the specialty skills under the general skill like this (assuming you have a high enough attribute or mentors):");
            final Optional<String> existingSpecialtyAdvancement = skills.stream()
                    .filter(specialtySkill -> specialtySkill.getStartingFacets() != 0)
                    .map(specialtySkill -> MessageFormat.format("f{0} → f{1} ({2} AP)", specialtySkill.getStartingFacets(), specialtySkill.getCurrentFacets(), specialtySkill.getAPSpent()))
                    .reduce((s, s2) -> MessageFormat.format("{0}\n{1}", s, s2));
            existingSpecialtyAdvancement.ifPresent(s -> resultsEmbed.addInlineField("Existing Specialties", s));
            final Optional<String> newSpecialtySkillAdvancement = skills.stream()
                    .filter(specialtySkill -> specialtySkill.getStartingFacets() == 0)
                    .map(specialtySkill -> MessageFormat.format("f{0} ({1} AP)", specialtySkill.getCurrentFacets(), specialtySkill.getAPSpent()))
                    .reduce((s, s2) -> MessageFormat.format("{0}\n{1}", s, s2));
            newSpecialtySkillAdvancement.ifPresent(s -> resultsEmbed.addInlineField("New Specialties", s));
            resultsEmbed.addField("Total AP Cost", String.valueOf(skills.stream().mapToInt(SpecialtySkill::getAPSpent).sum()));


            firstResponder.addEmbed(resultsEmbed);
            if (!nonEphemeral) {
                firstResponder.setFlags(InteractionCallbackDataFlag.EPHEMERAL);
            }
            firstResponder.respond();
        }
        else {
            firstResponder.setContent("Desired general skill facet not set!").setFlags(InteractionCallbackDataFlag.EPHEMERAL).respond();
        }
    }
}
