package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import logic.RandomColor;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Command class that gets the amount of time before the start of next session, which is usually 7PM EST, 8 days after
 * the first and third Saturday of the week as well as the date of the next session
 */
public class CalendarCommand implements CommandExecutor {

    private final ZoneId nyTimeZone = ZoneId.of("America/New_York");

    @Command(aliases = {"~c", "~cal", "~calendar"}, description = "Displays the amount of time before the next session." +
            " Some examples of valid commands are ``~c GMT-5``", usage = "~c [timezone]")
    public void displayCalendar(String[] params, DiscordApi api, MessageAuthor author, Message message, TextChannel channel) {
        ZonedDateTime currentTime = ZonedDateTime.now(nyTimeZone);
        ZoneId selectedID;
        //Try to find time zone ID of input
        try {
            selectedID = ZoneId.of(params[0].toUpperCase());
        } catch (DateTimeException | ArrayIndexOutOfBoundsException d) {
            selectedID = nyTimeZone;
            if (params.length != 0) {
                channel.sendMessage("Unable to find time zone. If you are in the Americas, you probably need to use UTC with a modifier. Defaulting to EDT (UST-4).");
            }
        }
        List<Integer> firstAndThird = getSessionDates(currentTime.getYear(), currentTime.getMonth());

        //Check if next session is next month
        int firstSessionDate = firstAndThird.get(0);
        int secondSessionDate = firstAndThird.get(1);
        ZonedDateTime startTimeOfFirstSession = ZonedDateTime.of(currentTime.getYear(), currentTime.getMonthValue(), firstSessionDate, 19, 30, 0, 0, nyTimeZone);
        ZonedDateTime startTimeOfLastSession = ZonedDateTime.of(currentTime.getYear(), currentTime.getMonth().getValue(), secondSessionDate, 19, 30, 0, 0, nyTimeZone);
        ZonedDateTime startOfNextSession = calculateNextSessionTime(currentTime, startTimeOfFirstSession, startTimeOfLastSession);
        //Generate embed to next session information
        EmbedBuilder resultEmbed = new EmbedBuilder()
                .addField("Date of Next Session", startOfNextSession.withZoneSameInstant(selectedID).format(DateTimeFormatter.ofPattern("yyyy MMM dd HH:mm (z)")))
                .addField("Time until next session", calculateTimeDifference(currentTime, startOfNextSession))
                .setTitle("Next session information")
                .setColor(RandomColor.getRandomColor());
        channel.sendMessage(resultEmbed);
    }

    /**
     * Calculates the start time of next session.
     *
     * @param currentTime             The current time expressed as a ZonedDateTime
     * @param startTimeOfFirstSession The start time of the first session of the month
     * @param startTimeOfLastSession  The start time of the second (and last) session of the month
     * @return The start time of the next session as a ZonedDateTime object
     */
    private ZonedDateTime calculateNextSessionTime(ZonedDateTime currentTime, ZonedDateTime startTimeOfFirstSession, ZonedDateTime startTimeOfLastSession) {
        //No more sessions this month
        if (currentTime.isAfter(startTimeOfLastSession)) {
            //Check for start of next month to avoid going out of bounds on number of months
            ZonedDateTime firstDayOfNextMonth = ZonedDateTime.of(LocalDateTime.now().plusMonths(1).withDayOfMonth(1), nyTimeZone);
            //Get date of first session for next month
            int dateOfNextSession = getSessionDates(firstDayOfNextMonth.getYear(), firstDayOfNextMonth.getMonth()).get(0);
            return ZonedDateTime.of(firstDayOfNextMonth.getYear(), firstDayOfNextMonth.getMonthValue(), dateOfNextSession, 19, 30, 0, 0, nyTimeZone);
        }
        //Still more sessions this month
        else {
            //Check if first session has started already
            if (currentTime.isAfter(startTimeOfFirstSession)) {
                return startTimeOfLastSession;
            }
            else {
                return startTimeOfFirstSession;
            }
        }

    }

    private String calculateTimeDifference(ZonedDateTime d1, ZonedDateTime d2) {
        Duration duration = Duration.between(d1, d2);
// total seconds of difference (using Math.abs to avoid negative values)
        long seconds = Math.abs(duration.getSeconds());
        long days = seconds / 86400;
        seconds -= days * 86400;
        long hours = seconds / 3600;
        seconds -= (hours * 3600);
        long minutes = seconds / 60;
        seconds -= (minutes * 60);
        return days + " days " + hours + " hours " + minutes + " minutes " + seconds + " seconds";
    }

    /**
     * Returns an arraylist of the date of the days that are 8 days after the first and third Saturdays of the month.
     *
     * @param year  The input year
     * @param month The input month
     * @return The days of the month that have a session
     */
    private List<Integer> getSessionDates(int year, Month month) {
        ArrayList<Integer> saturdays = new ArrayList<>();
        IntStream.rangeClosed(1, YearMonth.of(year, month).lengthOfMonth())
                .mapToObj(day -> LocalDate.of(year, month, day))
                .filter(date -> date.getDayOfWeek() == DayOfWeek.SATURDAY)
                .forEach(date -> saturdays.add(date.getDayOfMonth() + 8));
        saturdays.sort(Integer::compareTo);
        ArrayList<Integer> firstAndThird = new ArrayList<>();
        firstAndThird.add(saturdays.get(0));
        firstAndThird.add(saturdays.get(2));
        return firstAndThird;
    }
}
