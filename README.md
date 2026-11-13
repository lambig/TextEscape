# TextEscape

Javaの文字列中にある ${key} を、名前に対応する値へ置き換えます。複数の文字列をつなぎ、未定義キーに既定値を指定できます。HTML・SQL・シェル用の無害化や、式の評価を行うライブラリではありません。出力先に応じた処理は利用側で行ってください。

実行にはJava 11以上が必要です。実行時の外部依存はありません。

## 導入

以下は **0.1.1候補版** の依存宣言です。Maven Centralへの公開はまだ行っていません。候補版を試すときは、このリポジトリの `sh ./verify-consumers.sh candidate` を実行してください。

Maven:

```xml
<dependency>
  <groupId>io.github.lambig</groupId>
  <artifactId>TextEscape</artifactId>
  <version>0.1.1</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.github.lambig:TextEscape:0.1.1'
```

## 利用例

[実行可能な例](example/src/main/java/example/TextEscapeExample.java)と同じプログラムです。Maven／Gradleによる実行方法は[開発・検証](DEVELOPING.md)を参照してください。

```java
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
```

出力:

```text
Tea: $5\cup | unknown | ${literal}
Coffee: $5\cup | pending | ${literal}
${name}
literal key
missing key rejected
```

## 置換の規則

- `escape(String...)` は入力をコピーします。入力ゼロは空文字列、nullの配列・要素は拒否します。
- 文字列を区切り文字で連結してから置換します。既定の区切りは `System.lineSeparator()` です。区切りにあるトークン、連結で生じるトークンも対象です。
- キーは `${` と次の `}` の間の文字列です。空キーやドット等も文字どおり比較し、正規表現として扱いません。
- `where(key, value)` は値を一度だけ `String.valueOf` で文字列化します。同じキーは後の設定が優先します。未使用キーは結果に影響しません。
- `where(Map<String, Object>)` はMapの反復順で設定します。途中で不正値があると例外になり、それ以前の設定は残ります。Map自体・キー・値のnullは拒否します。
- 値と既定値のドル記号・バックスラッシュは文字として保持します。未エスケープのトークンを含む値は `IllegalArgumentException` になります。トークンそのものを出す場合、Java文字列では `"\\${name}"` と指定します。
- 本文でトークンの直前にバックスラッシュがあれば置換しません。キーの登録も不要です。出力時には `\${` のバックスラッシュを一つ外します。連続するバックスラッシュも偶奇で判定せず、直前の一つだけがこの役割を持ちます。
- 置換は連結後の本文に一度だけ行います。挿入した値を再び変数展開せず、値と隣接文字から生じるトークンも再走査しません。
- 未定義キーは `NoSuchElementException` になります。非nullの `setDefaultValue` を設定すると、その時点の既定値を使います。既定値を明示的なキー登録へ書き戻しません。
- インスタンスは変更可能です。設定を変えなければ繰り返し `compile()` しても同じ結果です。変更後は新しい値・区切り・既定値を使います。並行利用には対応していません。
- `toString()` も `compile()` を呼ぶため、未定義キーがあれば例外になります。nullの区切りは `compile()` 時に拒否します。

## 0.1.0からの更新

公開メソッドと最低Java 11を維持します。0.1.0でコンパイルした利用例を新JARでも実行して確認します。Lombokはbuild時だけ使用し、利用側へ推移させません。

0.1.1では、置換値のドル記号・バックスラッシュとキーの正規表現記号を文字として処理します。エスケープされた未定義トークンを拒否する問題、compileが既定値をキー登録へ書き戻す問題、オブジェクト値の変換を二度行う問題も修正します。旧版の誤動作に依存した結果は変わります。再帰展開・式評価を追加するものではありません。

API説明は[公開ソース](src/main/java/io/github/lambig/textescape/TextEscape.java)と、候補buildで生成する `build/docs/javadoc/index.html` にあります。sources/Javadoc JAR、検証と公開用bundleは[DEVELOPING.md](DEVELOPING.md)を参照してください。不具合・質問は[Issues](https://github.com/lambig/TextEscape/issues)へお願いします。
