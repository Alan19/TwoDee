package statistics2;

import java.util.Scanner;

public class FacetsStatsCLI {


    static void main(String[] args) {
        System.out.println("Enter your dice pool");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            DicePool dicePool = new DicePool(input);

            DicePoolStatistics statistics = new DicePoolStatistics(dicePool);
        }
    }

}
