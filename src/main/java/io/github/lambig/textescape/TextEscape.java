package io.github.lambig.textescape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * テンプレート文字列やヒアドキュメントに近い利用感で文字列を出力するためのクラスです。
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class TextEscape {
    /**
     * エスケープされた置換対象であることを示すパターン 置換されない
     */
    private static final Pattern escapePattern = Pattern.compile("\\$\\{([^}]*)\\}");

    /**
     * 置換対象であることを示すパターン
     */
    private static final Pattern unescapedPattern = Pattern.compile("(?<!\\\\)\\$\\{([^}]*)\\}");

    /**
     * 元の文字列(複数可)
     */
    private final List<String> texts;

    /**
     * 宣言された変数Map
     */
    private final Map<String, String> variables = new HashMap<>();

    private String defaultValue;

    @Setter
    private String delimiter = System.lineSeparator();

    /**
     * エスケープしたいテキストを与えます。
     *
     * @param texts テキスト(複数可)
     * @return 設定済みインスタンス
     */
    public static TextEscape escape(String... texts) {
        return new TextEscape(List.of(texts));
    }

    /**
     * 変数のマッピングを1つ追加します。
     * 値がStringでない場合、String#valueOfにより変換します。
     *
     * @param key   変換キー
     * @param value 返還後の値
     * @return 設定済みインスタンス
     */
    public TextEscape where(@NonNull String key, @NonNull Object value) {
        var stringValue = String.valueOf(value);
        if (unescapedPattern.matcher(stringValue).find()) {
            throw new IllegalArgumentException("エスケープされていない置換対象文字列が置換後の値として渡されました: " + stringValue);
        }
        this.variables.put(key, String.valueOf(value));
        return this;
    }

    /**
     * Mapに定義されているマッピングをすべて追加します。
     * MapのvalueがStringでない場合、String#valueOfにより変換します。
     * Mapのvalueは非null、かつ置換対象文字列(${abc}など)でない文字列に限ります。
     * 文字列としての ${abc} を指定したい場合は \${abc} とエスケープしてください。
     *
     * @param values 変数のマッピングを設定したMap
     * @return 設定済みインスタンス
     * @throws NullPointerException Mapがvalueとしてnullを持つ場合
     */
    public TextEscape where(@NonNull Map<String, Object> values) {
        values.forEach(this::where);
        return this;
    }

    /**
     * 文字列に登場した置換対象文字列に対応する変数がwhereで宣言されていなかった場合に
     * デフォルト値として利用する文字列を設定します。
     * デフォルト値は非null、かつ置換対象文字列(${abc}など)でない文字列に限ります。
     * 文字列としての ${abc} を指定したい場合は \${abc} とエスケープしてください。
     *
     * @param defaultValue デフォルト文字列
     * @return 設定済みインスタンス
     */
    public TextEscape setDefaultValue(String defaultValue) {
        if (unescapedPattern.matcher(defaultValue).find()) {
            throw new IllegalArgumentException("エスケープされていない置換対象文字列がデフォルト値として渡されました: " + defaultValue);
        }
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * escapeで与えていた文字列をdelimiterで連接し、
     * ${variableName}をwhereでvariableNameに対して宣言した値で置換します。
     * \${variableName}とバックスラッシュでエスケープすると置換せずに${variableName}として出力します。
     *
     * @return 返還後文字列
     * @throws NoSuchElementException variableNameに対応する値がwhereで宣言されていない場合
     */
    public String compile() {
        String text = this.texts.stream().collect(joining(this.delimiter));
        Set<String> keysInText = extractKeysFromText(text);
        Set<String> missingKeys = keysInText.stream()
                .filter(not(this.variables::containsKey))
                .collect(toSet());

        Optional.ofNullable(defaultValue)
                .ifPresentOrElse(
                        replacement -> missingKeys.forEach(key -> variables.put(key, replacement)),
                        () -> {
                            if (!missingKeys.isEmpty()) {
                                throw new NoSuchElementException("次のキーが変数マップに見つかりませんでした: " + String.join(", ", missingKeys));
                            }
                        }
                );
        return keysInText.stream()
                .reduce(text, (soFar, current) -> soFar.replaceAll("(?<!\\\\)\\$\\{" + current + "\\}", this.variables.get(current)))
                .replaceAll("\\\\\\$\\{", "\\${");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.compile();
    }

    private Set<String> extractKeysFromText(String text) {
        return escapePattern.matcher(text)
                .results()
                .map(MatchResult::group)
                .map(group -> group.substring(2, group.length() - 1))
                .collect(toSet());
    }
}