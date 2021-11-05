package dev.loukylor.aiko.utils;

public class StringUtils {
    public static boolean isEmptyOrNull(String str) { return str == null || str.isEmpty(); }

    public static String addSuffixToNumber(Number num) {
        // To bypass the char check that addSuffixToNumber has
        return addSuffixToNumberInternal(num.toString());
    }

    public static String addSuffixToNumber (String num) {
        for (int i = 0; i < num.length(); i++)
            if (!Character.isDigit(num.charAt(i)))
                throw new IllegalArgumentException("The given string is not a number");

        return addSuffixToNumberInternal(num);
    }

    private static String addSuffixToNumberInternal (String num) {
        // According to this grammarly page https://www.grammarly.com/blog/how-to-write-ordinal-numbers-correctly/
        if (num.isEmpty())
            throw new IllegalArgumentException("The given string cannot be empty");

        if (num.length() > 1) {
            String last2Chars = num.substring(num.length() - 2, num.length());
            switch (last2Chars) {
                case "11":
                case "12":
                case "13":
                    return num + "th";
            }
        }

        char lastChar = num.charAt(num.length() - 1);
        switch (lastChar) {
            case '1':
                return num + "st";
            case '2':
                return num + "nd";
            case '3':
                return num + "rd";
            default:
                return num + "th";
        }
    }
}
