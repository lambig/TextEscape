package io.github.lambig.textescape;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static io.github.lambig.textescape.TextEscape.escape;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiteralReplacementTest {
    @Test
    void replacementMetacharactersAreLiteral() {
        assertThat(escape("${value}").where("value", "$1 ${broken C:\\tmp\\").compile())
                .isEqualTo("$1 ${broken C:\\tmp\\");
    }

    @Test
    void defaultMetacharactersAreLiteral() {
        assertThat(escape("${missing}").setDefaultValue("$5\\tail\\").compile())
                .isEqualTo("$5\\tail\\");
    }

    @Test
    void keysAreLiteralIncludingRegexCharactersAndEmptyKey() {
        assertThat(escape("${a.b}|${axb}|${[}|${}|${a\\b}|${a${b}")
                .where("a.b", "dot").where("axb", "plain").where("[", "bracket")
                .where("", "empty").where("a\\b", "slash").where("a${b", "nested").compile())
                .isEqualTo("dot|plain|bracket|empty|slash|nested");
    }

    @Test
    void escapedUnknownTokenNeedsNoMapping() {
        assertThat(escape("\\${unknown}|\\\\${other}").compile())
                .isEqualTo("${unknown}|\\${other}");
    }

    @Test
    void defaultsAreReevaluatedAndDoNotBecomeExplicitMappings() {
        TextEscape template = escape("${missing}").setDefaultValue("first");
        assertThat(template.compile()).isEqualTo("first");
        assertThat(template.setDefaultValue("second").compile()).isEqualTo("second");
        assertThat(template.where("missing", "explicit").compile()).isEqualTo("explicit");
        assertThat(template.setDefaultValue("third").compile()).isEqualTo("explicit");
    }

    @Test
    void eachCompileUsesCurrentMappingsAndDelimiter() {
        TextEscape template = escape("${a}", "${a}").setDelimiter("/").where("a", 1);
        assertThat(template.compile()).isEqualTo("1/1");
        assertThat(template.compile()).isEqualTo("1/1");
        assertThat(template.where("a", 2).setDelimiter("-").toString()).isEqualTo("2-2");
    }

    @Test
    void insertedFragmentsAreNotRescannedAsTokens() {
        assertThat(escape("${a}{b}|${b}").where("a", "$").where("b", "value").compile())
                .isEqualTo("${b}|value");
        assertThat(escape("${a}${b}").where("a", "\\").where("b", "value").compile())
                .isEqualTo("\\value");
    }

    @Test
    void escapedValuesAreNotExpandedAgain() {
        assertThat(escape("${a}|${b}").where("a", "\\${b}").where("b", "value").compile())
                .isEqualTo("${b}|value");
    }

    @Test
    void nullInputsAreRejected() {
        assertThatThrownBy(() -> escape((String[]) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> escape("a", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> escape().where(null, "a")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> escape().where("a", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> escape().where((Map<String, Object>) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> escape().setDefaultValue(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> escape().setDelimiter(null).compile()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void missingKeysStillFailWithoutDefault() {
        assertThatThrownBy(() -> escape("\\${literal}|${missing}").compile())
                .isInstanceOf(NoSuchElementException.class).hasMessageContaining("missing")
                .hasMessageNotContaining("literal");
    }

    @Test
    void whereConvertsObjectOnlyOnce() {
        Object value = new Object() {
            int calls;
            @Override public String toString() { return ++calls == 1 ? "safe" : "${unexpected}"; }
        };
        assertThat(escape("${a}").where("a", value).compile()).isEqualTo("safe");
    }

    @Test
    void mapFailureRetainsEarlierMappings() {
        Map<String, Object> entries = new LinkedHashMap<>();
        entries.put("first", "ok");
        entries.put("second", null);
        TextEscape template = escape("${first}");
        assertThatThrownBy(() -> template.where(entries)).isInstanceOf(NullPointerException.class);
        assertThat(template.compile()).isEqualTo("ok");
    }
}
