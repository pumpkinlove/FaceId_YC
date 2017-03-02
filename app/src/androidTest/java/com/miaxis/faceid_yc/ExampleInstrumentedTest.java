package com.miaxis.faceid_yc;

import android.content.Context;
import android.icu.util.TimeUnit;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private static boolean STOP;

    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.miaxis.faceid_yc", appContext.getPackageName());
    }

    @Test
    public void test() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (!STOP) {
                    i++;
                }
               System.out.println("over");
            }
        });

        t.start();
        Thread.sleep(3000);
        STOP = true;
    }
}
