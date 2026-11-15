package example;

import io.github.lambig.textescape.TextEscape;
import java.util.Map;

/** 0.1.0でコンパイルし、新旧JARで実行する既存APIの利用例。 */
public final class CompatibilityExample {
    public static void main(String[] args) {
        TextEscape template = TextEscape.escape("${name}", "${count}", "${missing}")
                .where("name", "tea").where(Map.of("count", 2))
                .setDefaultValue("unknown").setDelimiter("/");
        if (!"tea/2/unknown".equals(template.compile())) throw new AssertionError();
        if (!"tea/2/unknown".equals(template.toString())) throw new AssertionError();
        System.out.println("existing API passed");
    }
}
