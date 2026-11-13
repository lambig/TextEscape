# 開発・検証

ビルドはJDK 17以上、実行対象はJava 11以上です。Gradleは同梱Wrapperを使います。
独立例の検証にはMaven 3.8以上、Python 3、POSIXシェルも必要です。
`TEST_JAVA_HOME`を指定すると、そのJavaで試験と利用例を実行します。
CIではbuild JDK 17と実行環境Java 11／25を組み合わせます。

```sh
sh ./verify-consumers.sh candidate
```

単体試験、sources/Javadoc、公開版0.1.0とのAPI比較、旧版でコンパイルした利用コード、
空キャッシュからのMaven／Gradle独立導入を確認します。READMEは同じ実行例を掲載し、
ソースと期待出力が両方のconsumerと一致することを照合します。
runtimeがTextEscapeのJARだけであること、Java 11 bytecodeとPOM／Module Metadataも確認します。
APIシグネチャと代表的な旧利用コードの互換性検査であり、旧版の全入力の動作一致ではありません。
修正した境界動作はREADMEの更新説明と契約試験を参照してください。

候補版は`build/repository`、結果は`build/consumer-results/candidate`、
試験・カバレッジは`build/reports`、日本語API文書は`build/docs/javadoc/index.html`に生成します。
JavadocはLombokを展開したソースから作成し、sources JARには元のJavaソースを含めます。

## 導入例を個別に動かす

```sh
sh ./gradlew publishMavenJavaPublicationToCandidateRepository
sh ./gradlew -p example -PcandidateRepository="$PWD/build/repository" runExample
mvn -f example/pom.xml -DcandidateRepository="file://$PWD/build/repository" \
  compile dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "example/target/classes:$(cat example/target/runtime-classpath.txt)" example.TextEscapeExample
```

期待出力はREADMEの利用例に記載しています。
0.1.1公開後は次のコマンドで、候補リポジトリを使わず同じ例を実行できます。
現在の0.1.1は未公開の候補です。

```sh
sh ./verify-consumers.sh public 0.1.1
```

## 公開用bundle

公開用のASCII形式秘密鍵を`SIGNING_KEY`、パスフレーズを`SIGNING_PASSWORD`に設定します。

```sh
sh ./gradlew clean check -PreleaseBundle centralBundle
```

`build/central/TextEscape-0.1.1-central.zip`には本体・sources・Javadoc・POM・Module Metadataと
署名・チェックサムを格納します。公開担当者は対象コミット・試験結果・bundle内容を確認して
Central Portalへ登録し、公開後に上記`public`検証を行います。署名鍵をリポジトリへ保存しないでください。
