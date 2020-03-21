package main;


import org.testng.annotations.BeforeClass;

import java.io.IOException;

public class Test {

    @org.junit.BeforeClass
    @BeforeClass
    public static void before() {
        new Thread(() -> {
            try {
                Main.main(new String[]{});
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }).start();
    }
}
