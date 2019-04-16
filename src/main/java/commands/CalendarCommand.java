package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Command class that gets the amount of time before the start of next session, which is usually 7PM EST, 8 days after
 * the first and third Saturday of the week as well as the date of the next session
 */
public class CalendarCommand implements CommandExecutor {
    @Command(aliases = {"~c", "~cal", "~calendar"}, description = "Displays the amount of time before the next session")
    public void displayCalendar(String[] params, DiscordApi api, MessageAuthor author, Message message, TextChannel channel) {
        final ZoneId NY_TIME_ZONE = ZoneId.of("America/New_York");
        ZonedDateTime currentTime = ZonedDateTime.now(NY_TIME_ZONE);
        List<Integer> firstAndThird = getFirstAndThirdSat(currentTime.getYear(), currentTime.getMonth());

        //Check if next session is next month
        int secondSessionDate = firstAndThird.get(1);
        ZonedDateTime startTimeOfLastSession = ZonedDateTime.of(currentTime.getYear(), currentTime.getMonth().getValue(), secondSessionDate, 19, 0, 0, 0, NY_TIME_ZONE);
        //No more sessions this month
        if (currentTime.isAfter(startTimeOfLastSession)) {
            //Check for start of next month to avoid going out of bounds on number of months
            ZonedDateTime firstDayOfNextMonth = ZonedDateTime.of(LocalDateTime.now().plusMonths(1).withDayOfMonth(1), NY_TIME_ZONE);
            int dateOfNextSession = getFirstAndThirdSat(firstDayOfNextMonth.getYear(), firstDayOfNextMonth.getMonth()).get(0);
            ZonedDateTime startOfNextSession = ZonedDateTime.of(firstDayOfNextMonth.getYear(), firstDayOfNextMonth.getMonthValue(), dateOfNextSession, 19, 0, 0, 0, NY_TIME_ZONE);
            calculateTimeDifference(currentTime, startOfNextSession);
        }
        //Still more sessions this month
        else {
            //Check if first session has started already
            ZonedDateTime startTimeOfFirstSession = ZonedDateTime.of(currentTime.getYear(), currentTime.getMonthValue(), firstAndThird.get(0), 19, 0, 0, 0, NY_TIME_ZONE);
            if (currentTime.isAfter(startTimeOfFirstSession)) {
                calculateTimeDifference(currentTime, startTimeOfLastSession);
            } else {
                calculateTimeDifference(currentTime, startTimeOfFirstSession);
            }
        }
    }

    private void calculateTimeDifference(ZonedDateTime d1, ZonedDateTime d2) {
        Duration duration = Duration.between(d1, d2);
// total seconds of difference (using Math.abs to avoid negative values)
        long seconds = Math.abs(duration.getSeconds());
        long hours = seconds / 3600;
        seconds -= (hours * 3600);
        long minutes = seconds / 60;
        seconds -= (minutes * 60);
        System.out.println(hours + " hours " + minutes + " minutes " + seconds + " seconds");

    }

    /**
     * Returns an arraylist of the date of the first and third Saturdays of the month
     *
     * @param year
     * @param month
     * @return
     */
    private List<Integer> getFirstAndThirdSat(int year, Month month) {
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
