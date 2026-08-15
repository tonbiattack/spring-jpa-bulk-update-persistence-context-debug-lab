# J001: バルク更新後に古い商品状態がflushされる

## 目的

商品停止APIは、HTTP 204を返すだけでなく、処理完了後にDBから読み直した`Product`の`status`を`SUSPENDED`、`note`を指定した停止理由にしなければなりません。SKUは更新対象ではないため、`SKU-001`のまま維持します。

## 最初に観測した事実

バグ状態のコミットは`985da6e`、最小修正のコミットは`62460dc`です。`mvn --batch-mode -Dtest=ProductSuspensionApiTest test`を実行すると、バグ状態のテストは設定不備ではなく、最終DB状態のアサーションで失敗しました。

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| HTTP応答 | 204 | 204 | `docs/01-bug-reproduction.log` 338〜343行 |
| 一括更新の直後の管理中エンティティ | `SUSPENDED`として扱える状態 | `status=ACTIVE` | 同ログ 273〜284行 |
| SQLの途中経過 | `status=SUSPENDED`へ更新 | `SUSPENDED`へのUPDATEを発行 | 同ログ 274〜282行 |
| SQLの最終flush | `SUSPENDED`を保持 | `status=ACTIVE`を含むUPDATEを発行 | 同ログ 285〜297行 |
| 最終DB状態 | `SUSPENDED` | `ACTIVE` | 同ログ 347〜388行の統合テスト |
| 停止理由 | `品質確認のため停止` | `品質確認のため停止` | 同じ統合テストの独立アサーション |

```text
expected: SUSPENDED
 but was: ACTIVE
```

HTTP 204と`SUSPENDED`への一度目のSQLは途中経過であり、商品が停止済みである証拠にはなりません。テストはサービスメソッドの終了後に`ProductRepository`で読み直し、永続化後の状態を確認しています。

## テストの境界

`ProductSuspensionApiTest`は`@SpringBootTest`と`MockMvc`を使い、停止APIをHTTP境界から実行します。テストはHTTP 204を確認した後、別のリポジトリ読み出しでDB状態を検証します。この境界を選んだのは、コントローラー、トランザクション、JPAのdirty checking、commit時のflushを通過してから初めて不具合が観測できるためです。サービスのメソッド呼び出しやモックの検証だけでは、最後のUPDATEによる上書きを検出できません。

## コードリーディングとデバッガーの観測点

バグ状態では、`ProductSuspensionService#suspendAndRecordReason`が管理中の`Product`を取得し、`ProductRepository#updateStatus`の`@Modifying`クエリを呼びます。続けて同じ`Product`の`note`を変更するため、メソッド終了時にはエンティティの状態がdirty checkingの対象になります。

デバッガーでは、次の3か所にブレークポイントを置くと、原因を短い手順で観測できます。

| 停止位置 | 評価する式 | 観測できる事実 |
| --- | --- | --- |
| `updateStatus`呼び出し直前 | `product.getStatus()` | 取得直後の管理中エンティティは`ACTIVE` |
| `updateStatus`呼び出し直後 | `product.getStatus()`と`updatedRows` | DB更新は1行でも、管理中エンティティは`ACTIVE`のまま |
| `changeNote`の直後 | `product.getStatus()`と`product.getNote()` | dirty checkingに渡るオブジェクトは`ACTIVE`と新しい`note`を同時に持つ |

SQLログは、`SUSPENDED`へのバルクUPDATEの直後に、`ACTIVE`を束縛したUPDATEが発行される順序を示します。これはデバッガーのオブジェクト状態と一致します。

## 仮説と切り分け

| 仮説 | 確認方法 | 結果 |
| --- | --- | --- |
| コントローラーが成功を誤って返している | `MockMvc`のHTTP応答を確認する | 204は実際に返る。原因ではないが、204だけでは業務状態を保証しない。 |
| 例外やロールバックで停止更新が取り消された | 例外の有無、`note`の永続化、SQLログを確認する | 例外はなく、`note`は永続化される。ロールバック仮説は否定した。 |
| バルク更新の実行自体が失敗した | 更新件数と最初のSQLパラメータを確認する | 更新件数は1で、最初のSQLは`SUSPENDED`を束縛する。否定した。 |
| 管理中エンティティが古い状態のままflushされる | デバッガー、サービスログ、二つ目のSQLパラメータを確認する | 一括更新後も`product.getStatus()`は`ACTIVE`で、最後のUPDATEも`ACTIVE`を束縛する。採用した。 |

## 原因

JPQLの一括更新はDB行を直接変更する一方、同一トランザクションですでに管理されている`Product`の状態を更新しません。Spring Data JPAの`@Modifying`には、修飾クエリの後に永続化コンテキストをclearするかを指定する`clearAutomatically`と、前にflushするかを指定する`flushAutomatically`があり、どちらの既定値も`false`です。[1]

このバグでは、一括更新の後に管理中の`Product`の`note`だけを変更しました。しかし、Hibernateはdirty checking時にエンティティが保持する状態をUPDATEへ束縛するため、管理中の古い`ACTIVE`が新しい`note`と一緒に書き戻されます。SQLログとデバッガーの両方で、最初の`SUSPENDED`更新と最後の`ACTIVE`更新の順序を確認できました。

## 修正

単一の商品を停止する業務操作では、バルク更新を使わず、すでに管理中のエンティティへ状態変更を集約しました。

```java
product.suspend();
product.changeNote(reason);
```

この修正では、commit時のflushが参照する`Product`自身が`SUSPENDED`と新しい`note`を持つため、最後のUPDATEは両方の意図した値を書き込みます。修正後の実行記録では、状態を`SUSPENDED`に変更した後に一つのUPDATEが発行され、同じHTTP統合テストが成功しました。

多数行を対象に一括更新を使う要件では、管理済みエンティティとの共存を避ける設計が必要です。`clearAutomatically = true`は修飾クエリ後に永続化コンテキストをclearする選択肢ですが、以後に対象エンティティを使うなら再取得が必要です。[1] この教材は単一商品の業務更新であるため、管理中エンティティの状態変更を選びました。

## 再発防止テスト

`ProductSuspensionApiTest#停止APIが204を返しても商品は停止済みかつ理由が保存される`は、修正前に`SUSPENDED`と`ACTIVE`の差分で失敗し、修正後に成功する同一のテストです。テストは次の3点を独立に確認します。

| 検証対象 | 目的 |
| --- | --- |
| HTTP 204 | API境界が要求を受け付けたことを確認する |
| `status=SUSPENDED` | 本来変更すべき最終状態を確認する |
| `note`と`sku` | 停止理由の保存と、変更してはならないSKUの維持を確認する |

## 再現手順

```bash
git checkout 985da6e
mvn --batch-mode -Dtest=ProductSuspensionApiTest test
# expected: SUSPENDED
#  but was: ACTIVE

git switch main
mvn --batch-mode -Dtest=ProductSuspensionApiTest test
# BUILD SUCCESS
```

## 適用範囲と注意点

この修正は、一つのトランザクションで管理中の単一エンティティを更新する業務操作に適用します。大量データを一括更新する場合まで、常にエンティティを1件ずつ更新することを推奨するものではありません。一括更新を必要とする処理では、対象エンティティを同じ永続化コンテキストで継続利用しないこと、clear後に必要な状態を再取得すること、並行更新に対しては別途バージョン管理や更新条件を設計することを検討してください。

## References

[1] [Spring Data JPA `@Modifying` API](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/Modifying.html)

[2] [Jakarta Persistence 3.1 Specification](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1)
