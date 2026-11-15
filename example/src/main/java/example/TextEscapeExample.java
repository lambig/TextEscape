package example;

import io.github.lambig.textescape.TextEscape;
import java.util.Map;
import java.util.NoSuchElementException;

public final class TextEscapeExample {
    public static void main(String[] args) {
        TextEscape template = TextEscape.escape("${name}: ${price}", "${missing}", "\\${literal}")
                .setDelimiter(" | ")
                .where(Map.of("name", "Tea", "price", "$5\\cup"))
                .setDefaultValue("unknown");
        expect("Tea: $5\\cup | unknown | ${literal}", template.compile());
        template.setDefaultValue("pending").where("name", "Coffee");
        expect("Coffee: $5\\cup | pending | ${literal}", template.toString());
        expect("${name}", TextEscape.escape("${value}").where("value", "\\${name}").compile());
        expect("literal key", TextEscape.escape("${a.b}").where("a.b", "literal key").compile());
        try {
            TextEscape.escape("${missing}").compile();
            throw new AssertionError("Missing key must fail");
        } catch (NoSuchElementException expected) {
            System.out.println("missing key rejected");
        }
    }

    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(actual);
        System.out.println(actual);
    }
}
