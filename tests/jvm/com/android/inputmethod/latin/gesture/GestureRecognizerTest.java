package com.android.inputmethod.latin.gesture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GestureRecognizerTest {
    private static final int KEY_WIDTH = 100;
    private static final int KEY_HEIGHT = 140;
    private static final String[] ROWS = { "qwertyuiop", "asdfghjkl", "zxcvbnm" };
    private static final float[] ROW_OFFSETS = { 0.0f, 0.5f, 1.5f };

    private static GestureKeyLayout createQwerty() {
        final int[] codes = new int[26];
        final int[] x = new int[26];
        final int[] y = new int[26];
        final int[] width = new int[26];
        final int[] height = new int[26];
        int n = 0;
        for (int row = 0; row < ROWS.length; ++row) {
            for (int column = 0; column < ROWS[row].length(); ++column) {
                codes[n] = Character.toUpperCase(ROWS[row].charAt(column));
                x[n] = (int) ((ROW_OFFSETS[row] + column) * KEY_WIDTH);
                y[n] = row * KEY_HEIGHT;
                width[n] = KEY_WIDTH;
                height[n] = KEY_HEIGHT;
                ++n;
            }
        }
        return new GestureKeyLayout(codes, x, y, width, height, n, KEY_WIDTH);
    }

    private static float[] keyCenter(final char letter) {
        for (int row = 0; row < ROWS.length; ++row) {
            final int column = ROWS[row].indexOf(letter);
            if (column >= 0) {
                return new float[] { (ROW_OFFSETS[row] + column + 0.5f) * KEY_WIDTH,
                        (row + 0.5f) * KEY_HEIGHT };
            }
        }
        throw new IllegalArgumentException("No key for " + letter);
    }

    private static int[][] createStroke(final String word, final Random random,
            final float noise) {
        final List<float[]> targets = new ArrayList<>();
        char previous = 0;
        for (final char letter : word.toCharArray()) {
            if (letter == previous) continue;
            previous = letter;
            final float[] center = keyCenter(letter);
            targets.add(new float[] {
                    center[0] + (float) random.nextGaussian() * noise * KEY_WIDTH,
                    center[1] + (float) random.nextGaussian() * noise * KEY_WIDTH });
        }
        final List<int[]> points = new ArrayList<>();
        for (int i = 0; i + 1 < targets.size(); ++i) {
            final float[] a = targets.get(i);
            final float[] b = targets.get(i + 1);
            for (int step = 0; step < 10; ++step) {
                final float t = step / 10.0f;
                points.add(new int[] { (int) (a[0] + t * (b[0] - a[0])),
                        (int) (a[1] + t * (b[1] - a[1])) });
            }
        }
        final float[] last = targets.get(targets.size() - 1);
        points.add(new int[] { (int) last[0], (int) last[1] });
        final int[][] stroke = new int[2][points.size()];
        for (int i = 0; i < points.size(); ++i) {
            stroke[0][i] = points.get(i)[0];
            stroke[1][i] = points.get(i)[1];
        }
        return stroke;
    }

    private static CompiledLexicon createLexicon() {
        final GestureLexicon.Builder builder = new GestureLexicon.Builder();
        final String[] words = { "the", "this", "that", "hello", "help", "world", "word",
                "would", "gesture", "typing", "keyboard", "quick", "brown", "fox", "jumps",
                "over", "lazy", "dog", "penguin", "input", "great", "treat", "it's", "Paris" };
        for (final String word : words) {
            assertTrue(word, builder.add(word, 150));
        }
        assertFalse(builder.add("a", 200));
        assertFalse(builder.add("r2d2", 200));
        return new CompiledLexicon(builder.build(1000), createQwerty());
    }

    @Test
    public void testResampleStraightLine() {
        final float[] xs = { 0.0f, 10.0f };
        final float[] ys = { 0.0f, 0.0f };
        final float[] outX = new float[5];
        final float[] outY = new float[5];
        final float length = GestureMath.resample(xs, ys, 2, 5, outX, outY);
        assertEquals(10.0f, length, 1e-4f);
        for (int i = 0; i < 5; ++i) {
            assertEquals(2.5f * i, outX[i], 1e-4f);
            assertEquals(0.0f, outY[i], 1e-4f);
        }
    }

    @Test
    public void testResampleKeepsEndpointsAndSpacing() {
        final float[] xs = { 0.0f, 3.0f, 3.0f, 3.0f };
        final float[] ys = { 0.0f, 0.0f, 0.0f, 4.0f };
        final int n = 8;
        final float[] outX = new float[n];
        final float[] outY = new float[n];
        assertEquals(7.0f, GestureMath.resample(xs, ys, xs.length, n, outX, outY), 1e-4f);
        assertEquals(0.0f, outX[0], 1e-4f);
        assertEquals(3.0f, outX[n - 1], 1e-4f);
        assertEquals(4.0f, outY[n - 1], 1e-4f);
        for (int i = 1; i < n; ++i) {
            final float d = GestureMath.distance(outX[i - 1], outY[i - 1], outX[i], outY[i]);
            assertTrue(d <= 1.0f + 1e-4f);
        }
    }

    @Test
    public void testResampleSinglePoint() {
        final float[] outX = new float[4];
        final float[] outY = new float[4];
        assertEquals(0.0f, GestureMath.resample(new float[] { 2.0f }, new float[] { 3.0f }, 1,
                4, outX, outY), 0.0f);
        for (int i = 0; i < 4; ++i) {
            assertEquals(2.0f, outX[i], 0.0f);
            assertEquals(3.0f, outY[i], 0.0f);
        }
    }

    @Test
    public void testNormalizeShape() {
        final float[] xs = { 10.0f, 14.0f, 14.0f, 10.0f };
        final float[] ys = { 5.0f, 5.0f, 7.0f, 7.0f };
        final float[] outX = new float[4];
        final float[] outY = new float[4];
        GestureMath.normalizeShape(xs, ys, 4, 0.1f, outX, outY);
        assertEquals(-0.5f, outX[0], 1e-4f);
        assertEquals(0.5f, outX[1], 1e-4f);
        assertEquals(-0.25f, outY[0], 1e-4f);
        assertEquals(0.25f, outY[2], 1e-4f);
    }

    @Test
    public void testLexiconKeepsMostProbableWords() {
        final GestureLexicon.Builder builder = new GestureLexicon.Builder();
        builder.add("low", 10);
        builder.add("high", 200);
        builder.add("mid", 100);
        builder.add("top", 255);
        final GestureLexicon lexicon = builder.build(2);
        assertEquals(2, lexicon.size());
        assertEquals("high", lexicon.getWord(0));
        assertEquals("top", lexicon.getWord(1));
        assertEquals(255, lexicon.getProbability(1));
    }

    @Test
    public void testKeyLookupIgnoresCaseAndDiacritics() {
        final GestureKeyLayout layout = createQwerty();
        final int e = layout.getKeyIndexForCodePoint('e');
        assertTrue(e >= 0);
        assertEquals(e, layout.getKeyIndexForCodePoint('E'));
        assertEquals(e, layout.getKeyIndexForCodePoint('é'));
        assertEquals(-1, layout.getKeyIndexForCodePoint('1'));
    }

    @Test
    public void testRecognizesExactStrokes() {
        final CompiledLexicon lexicon = createLexicon();
        final GestureRecognizer recognizer = new GestureRecognizer();
        final Random random = new Random(1);
        for (final String word : new String[] { "hello", "world", "gesture", "keyboard",
                "penguin", "quick", "lazy" }) {
            final int[][] stroke = createStroke(word, random, 0.0f);
            final GestureRecognizer.Candidate[] candidates = recognizer.recognize(stroke[0],
                    stroke[1], stroke[0].length, new CompiledLexicon[] { lexicon }, 5);
            assertTrue(word, candidates.length > 0);
            assertEquals(word, candidates[0].getWord());
        }
    }

    @Test
    public void testRecognizesNoisyStrokes() {
        final CompiledLexicon lexicon = createLexicon();
        final GestureRecognizer recognizer = new GestureRecognizer();
        final Random random = new Random(7);
        final String[] words = { "hello", "world", "typing", "keyboard", "brown", "jumps",
                "input", "great" };
        int correct = 0;
        int total = 0;
        for (int round = 0; round < 5; ++round) {
            for (final String word : words) {
                final int[][] stroke = createStroke(word, random, 0.15f);
                final GestureRecognizer.Candidate[] candidates = recognizer.recognize(
                        stroke[0], stroke[1], stroke[0].length,
                        new CompiledLexicon[] { lexicon }, 5);
                ++total;
                if (candidates.length > 0 && word.equals(candidates[0].getWord())) ++correct;
            }
        }
        assertTrue("accuracy " + correct + "/" + total, correct >= total * 0.9);
    }

    @Test
    public void testSkippedCharactersAndCapitals() {
        final CompiledLexicon lexicon = createLexicon();
        final GestureRecognizer recognizer = new GestureRecognizer();
        final Random random = new Random(3);
        final int[][] its = createStroke("its", random, 0.0f);
        GestureRecognizer.Candidate[] candidates = recognizer.recognize(its[0], its[1],
                its[0].length, new CompiledLexicon[] { lexicon }, 3);
        assertEquals("it's", candidates[0].getWord());
        final int[][] paris = createStroke("paris", random, 0.0f);
        candidates = recognizer.recognize(paris[0], paris[1], paris[0].length,
                new CompiledLexicon[] { lexicon }, 3);
        assertEquals("Paris", candidates[0].getWord());
    }

    @Test
    public void testTinyStrokeIsIgnored() {
        final CompiledLexicon lexicon = createLexicon();
        final GestureRecognizer recognizer = new GestureRecognizer();
        final int[] xs = { 50, 52 };
        final int[] ys = { 70, 70 };
        assertEquals(0, recognizer.recognize(xs, ys, 2,
                new CompiledLexicon[] { lexicon }, 3).length);
    }
}
