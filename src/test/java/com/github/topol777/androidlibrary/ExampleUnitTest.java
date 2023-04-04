package com.github.topol777.androidlibrary;

import org.junit.Test;

import com.github.topol777.androidlibrary.sound.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void storageName() throws Exception {
        File f = new File("/tmp/abc (888).txt");
    }

    @Test
    public void localeSortTest() {
        TTS.PreferedLocales d = new TTS.PreferedLocales(new Locale("ru", "RU"), Locale.CANADA_FRENCH);
        ArrayList<Locale> ll = new ArrayList<>(Arrays.asList(new Locale("en", "US"), new Locale("ru"), Locale.CANADA_FRENCH));
        Collections.sort(ll, d);
        System.out.println(ll);
    }
}