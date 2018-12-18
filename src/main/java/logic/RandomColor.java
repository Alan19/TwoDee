package logic;

import java.awt.*;
import java.util.Random;

public class RandomColor {

    private static RandomColor singleton = new RandomColor( );
    private static Random random;

    private RandomColor(){
        random = new Random();
    }

    public static Color getRandomColor(){
        return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat());
    }
}
