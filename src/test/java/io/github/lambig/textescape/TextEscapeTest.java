package io.github.lambig.textescape;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static io.github.lambig.textescape.TextEscape.escape;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NonAsciiCharacters")
class TextEscapeTest {

    @Nested
    class 置換の検証 {
        @Test
        void 空の本文は空で出力すること() {
            //SetUp
            //Exercise
            var actual = escape()
                    .setDelimiter(",")
                    .compile();

            //Verify
            assertThat(actual).isEmpty();
        }

        @Test
        void 未使用変数は未使用のまま出力すること() {
            //SetUp
            //Exercise
            var actual = escape("zzz")
                    .setDelimiter(",")
                    .where("a", "b")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("zzz");
        }

        @Test
        void 置換対象がない場合_デリミタで連接するだけで出力すること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "abc",
                    "abc",
                    "abc")
                    .setDelimiter(",")
                    .where("a", "b")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("abc,abc,abc");
        }

        @Test
        void 置換が定義されている場合_連接して置換すること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "abc",
                    "${a}bc")
                    .where("a", "b")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo(Stream.of("bbc", "abc", "bbc").collect(joining(System.lineSeparator())));
        }

        @Test
        void デリミタがトークンの場合_本文同様置換すること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "abc",
                    "${a}bc")
                    .setDelimiter("${a}")
                    .where("a", "b")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("bbcbabcbbbc");
        }

        @Test
        void デリミタによる連接の結果トークンができた場合_本文同様置換すること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "{a}bc",
                    "{a}bc")
                    .setDelimiter("$")
                    .where("a", "b")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("bbcbbcbbc");
        }


        @Test
        void エスケープがキャンセルされている場合_置換しないこと() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "abc",
                    "\\${a}bc")
                    .where("a", "b")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo(Stream.of("bbc", "abc", "${a}bc").collect(joining(System.lineSeparator())));
        }

        @Test
        void エスケープがキャンセルされている場合_キャンセル直前のバックスラッシュに影響しないこと() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "abc",
                    "\\\\${a}bc")
                    .where("a", "b")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo(Stream.of("bbc", "abc", "\\${a}bc").collect(joining(System.lineSeparator())));
        }

        @Test
        void 置換対象文字列が値に含まれる場合_エスケープしてなければIllegalArgumentExceptionを送出すること() {
            //SetUp
            //Exercise
            var actual = assertThatThrownBy(() -> escape(
                    "${a}bc",
                    "abc",
                    "${a}bc")
                    .where("a", "${a}")
                    .where("b", "c")
                    .compile());

            //Verify
            actual
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("エスケープされていない置換対象文字列が置換後の値として渡されました: ${a}");
        }

        @Test
        void 置換対象文字列が値に含まれる場合_エスケープしてあればエスケープを外して出力すること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "abc",
                    "${a}bc")
                    .where("a", "\\${a}")
                    .where("b", "c")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo(Stream.of("${a}bc", "abc", "${a}bc").collect(joining(System.lineSeparator())));
        }


        @Test
        void 定義が重複する場合_後勝ちになること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "abc",
                    "${a}bc")
                    .setDelimiter(",")
                    .where("a", "b")
                    .where("a", "c")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("cbc,abc,cbc");
        }
    }

    @Nested
    class Mapによる設定 {
        @Test
        void Mapで設定_空Mapの場合も置換対象なしとして実行できること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "abc",
                    "abc",
                    "abc")
                    .setDelimiter(",")
                    .where(Map.of())
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("abc,abc,abc");
        }

        @Test
        void Mapで設定_反映されていること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "a${b}c",
                    "${a}bc",
                    "abc")
                    .setDelimiter(",")
                    .where("a", "b")
                    .where(Map.of("b", "c"))
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("acc,bbc,abc");
        }

        @Test
        void Mapとwhereで直接設定した場合_重複設定は後勝ちになること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "abc",
                    "${a}bc",
                    "abc")
                    .setDelimiter(",")
                    .where("a", "b")
                    .where(Map.of("a", "c"))
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("abc,cbc,abc");
        }
    }

    @Nested
    class デフォルト値 {
        @Test
        void デフォルト値がエスケープされていないトークンの場合_IllegalArgumentExceptionを送出すること() {
            //SetUp
            //Exercise
            var actual = assertThatThrownBy(() -> escape(
                    "${a}bc",
                    "ab${c}",
                    "${a}bc")
                    .setDelimiter(",")
                    .setDefaultValue("${a}")
                    .compile());

            //Verify
            actual
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("エスケープされていない置換対象文字列がデフォルト値として渡されました: ${a}");
        }

        @Test
        void デフォルト値がエスケープされているトークンの場合_デフォルト値として利用できること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "ab${c}",
                    "${a}bc")
                    .setDelimiter(",")
                    .setDefaultValue("\\${a}")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("${a}bc,ab${a},${a}bc");
        }

        @Test
        void キー未設定の場合_デフォルトあり_デフォルトで置換すること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "ab${c}",
                    "${a}bc")
                    .setDelimiter(",")
                    .setDefaultValue("x")
                    .compile();

            //Verify
            assertThat(actual).isEqualTo("xbc,abx,xbc");
        }

        @Test
        void キー未設定の場合_デフォルトなし_NoSuchElementExceptionを送出すること() {
            //SetUp
            //Exercise
            var actual = assertThatThrownBy(() -> escape(
                    "${a}bc",
                    "ab${c}",
                    "${a}bc")
                    .setDelimiter(",")
                    .compile());

            //Verify
            actual
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("次のキーが変数マップに見つかりませんでした: a, c");
        }
    }


    @Nested
    class toString {
        @Test
        void toStringでcompileできること() {
            //SetUp
            //Exercise
            var actual = escape(
                    "${a}bc",
                    "abc",
                    "${a}bc")
                    .where("a", "b")
                    .toString();

            //Verify
            assertThat(actual).isEqualTo(Stream.of("bbc", "abc", "bbc").collect(joining(System.lineSeparator())));
        }
    }

}