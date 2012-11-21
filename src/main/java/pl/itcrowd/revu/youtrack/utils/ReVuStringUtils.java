package pl.itcrowd.revu.youtrack.utils;

public class ReVuStringUtils {
// -------------------------- STATIC METHODS --------------------------

    public static String extractLines(String text, int lineStart, int lineEnd)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        int line = 0;
        for (int i = 0; i < text.length(); i++) {
            final char currentCharacter = text.charAt(i);
            if (line >= lineStart) {
                stringBuilder.append(currentCharacter);
            }
            if (currentCharacter == '\n') {
                line++;
            } else if (currentCharacter == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                if (line >= lineStart) {
                    stringBuilder.append('\n');
                }
                i++;
                line++;
            }
            if (line > lineEnd) {
                break;
            }
        }

        return stringBuilder.toString();
    }
}
