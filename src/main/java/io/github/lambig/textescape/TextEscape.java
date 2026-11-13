package io.github.lambig.textescape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * テンプレート内の {@code ${key}} を、登録した文字列へ一度だけ置換します。
 * 複数の入力を区切り文字で連結してから置換し、値の再帰展開や式評価は行いません。
 * HTML・SQL等の出力先に応じた無害化は利用側で行ってください。
 * インスタンスの設定は変更可能で、並行利用には対応していません。
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class TextEscape {
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

    private String delimiter = System.lineSeparator();

    /**
     * 入力同士をつなぐ区切り文字を設定します。既定は {@link System#lineSeparator()} です。
     * 連結後に置換するため、区切り自体のトークンも置換対象になります。
     *
     * @param delimiter 区切り文字。nullを設定した場合はcompile時に拒否します
     * @return このインスタンス
     */
    public TextEscape setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    /**
     * テンプレート文字列をコピーして保持します。入力ゼロなら空文字列を出力します。
     *
     * @param texts テキスト(複数可)
     * @return 設定済みインスタンス
     * @throws NullPointerException 配列または要素がnullの場合
     */
    public static TextEscape escape(String... texts) {
        return new TextEscape(List.of(texts));
    }

    /**
     * 変数のマッピングを1つ追加します。
     * 値はString#valueOfにより一度だけ変換し、同じキーは後の設定で上書きします。
     * キー・ドル記号・バックスラッシュは正規表現として解釈しません。
     * 値にトークンを含める場合は、その直前にバックスラッシュを付けてください。
     *
     * @param key   変換キー
     * @param value 置換後の値
     * @return 設定済みインスタンス
     * @throws NullPointerException keyまたはvalueがnullの場合
     * @throws IllegalArgumentException 値が未エスケープのトークンを含む場合
     */
    public TextEscape where(@NonNull String key, @NonNull Object value) {
        var stringValue = String.valueOf(value);
        if (unescapedPattern.matcher(stringValue).find()) {
            throw new IllegalArgumentException("エスケープされていない置換対象文字列が置換後の値として渡されました: " + stringValue);
        }
        this.variables.put(key, stringValue);
        return this;
    }

    /**
     * Mapに定義されているマッピングをすべて追加します。
     * MapのvalueがStringでない場合、String#valueOfにより変換します。
     * Mapのvalueは非null、かつ置換対象文字列(${abc}など)でない文字列に限ります。
     * 文字列としての ${abc} を指定したい場合は \${abc} とエスケープしてください。
     * Mapの反復順でwhereを呼びます。途中で例外になると、それ以前の設定は残ります。
     *
     * @param values 変数のマッピングを設定したMap
     * @return 設定済みインスタンス
     * @throws NullPointerException Map、key、valueのいずれかがnullの場合
     * @throws IllegalArgumentException 値が未エスケープのトークンを含む場合
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
     * compileは既定値をキー登録へ書き戻しません。設定を変えると次のcompileから反映します。
     *
     * @param defaultValue デフォルト文字列
     * @return 設定済みインスタンス
     * @throws NullPointerException defaultValueがnullの場合
     * @throws IllegalArgumentException 未エスケープのトークンを含む場合
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
     * エスケープしたトークンには変数登録が不要です。値に含まれるエスケープも出力時に外します。
     * キーは次の閉じ波括弧までの文字列で、空キーも扱えます。
     * 挿入した値や、値と隣接文字から生じたトークンは再走査しません。
     * この操作は設定を変更せず、同じ設定では繰り返しても同じ結果になります。
     *
     * @return 置換後文字列
     * @throws NoSuchElementException 未定義キーがあり既定値も未設定の場合
     * @throws NullPointerException 区切り文字がnullの場合
     */
    public String compile() {
        String text = this.texts.stream().collect(joining(this.delimiter));
        Set<String> keysInText = extractKeysFromText(text);
        Set<String> missingKeys = keysInText.stream()
                .filter(not(this.variables::containsKey))
                .collect(toSet());

        if (defaultValue == null && !missingKeys.isEmpty()) {
            throw new NoSuchElementException("次のキーが変数マップに見つかりませんでした: " + String.join(", ", missingKeys));
        }
        return unescapedPattern.matcher(text)
                .replaceAll(match -> Matcher.quoteReplacement(variables.getOrDefault(match.group(1), defaultValue)))
                .replace("\\${", "${");
    }

    /**
     * {@link #compile()} と同じ結果を返します。未定義キーの例外も同様に送出します。
     *
     * @return 置換後文字列
     */
    @Override
    public String toString() {
        return this.compile();
    }

    private Set<String> extractKeysFromText(String text) {
        return unescapedPattern.matcher(text)
                .results()
                .map(match -> match.group(1))
                .collect(toSet());
    }
}
