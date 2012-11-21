package pl.itcrowd.revu.youtrack.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReVuStringUtilsTest {
// -------------------------- OTHER METHODS --------------------------

    @Test
    public void extractLines()
    {
//        Given
        final String textA = "0\n1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12";
        final String textB = "the only line";
        final String textC = "0\r\n1\r\n";
        final String textD = "\n\n\r3\r\n4\r\n";
        final String textE = "0\n\n\n3\n4\n";

//        When
        final String resultA0 = ReVuStringUtils.extractLines(textA, 0, 0);
        final String resultA1 = ReVuStringUtils.extractLines(textA, 0, 1);
        final String resultA2 = ReVuStringUtils.extractLines(textA, 5, 10);
        final String resultA3 = ReVuStringUtils.extractLines(textA, 5, 12);
        final String resultA4 = ReVuStringUtils.extractLines(textA, 12, 12);
        final String resultA5 = ReVuStringUtils.extractLines(textA, 13, 14);
        final String resultB0 = ReVuStringUtils.extractLines(textB, 0, 0);
        final String resultB1 = ReVuStringUtils.extractLines(textB, 1, 1);
        final String resultC0 = ReVuStringUtils.extractLines(textC, 1, 1);
        final String resultD0 = ReVuStringUtils.extractLines(textD, 1, 4);
        final String resultE0 = ReVuStringUtils.extractLines(textE, 0, 4);

//        Then
        assertEquals("0\n", resultA0);
        assertEquals("0\n1\n", resultA1);
        assertEquals("5\n" + "6\n" + "7\n" + "8\n" + "9\n" + "10\n", resultA2);
        assertEquals("5\n" + "6\n" + "7\n" + "8\n" + "9\n" + "10\n" + "11\n" + "12", resultA3);
        assertEquals("12", resultA4);
        assertEquals("", resultA5);
        assertEquals(textB, resultB0);
        assertEquals("", resultB1);
        assertEquals("1\r\n", resultC0);
        assertEquals("\n\r3\r\n4\r\n", resultD0);
        assertEquals("0\n\n\n3\n4\n", resultE0);
    }
}
