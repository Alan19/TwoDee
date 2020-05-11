import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import statistics.StatisticsContext;

public class StatisticsTest {

    private StatisticsContext generator;

    @BeforeEach
    public void setUp(){
    }

    @Test
    public void correctLists(){
        EmbedBuilder embed = generator.execute();
        System.out.println(embed);
    }
}
