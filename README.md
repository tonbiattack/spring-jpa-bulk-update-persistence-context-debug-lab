# Spring Data JPAのバルク更新後に古いエンティティがflushされる問題をデバッグする

Spring Data JPAで商品停止APIがHTTP 204を返し、停止理由も保存されているのに、商品状態だけが`ACTIVE`へ戻る不具合を再現する最小プロジェクトです。失敗するHTTP統合テスト、SQLログ、デバッガーの観測点、最小修正、回帰テストを一つの履歴で確認できます。

## 扱う契約

商品を停止する操作では、HTTP応答の成功だけでなく、トランザクション終了後にDBから読み直した商品が`SUSPENDED`であり、停止理由とSKUも期待どおりでなければなりません。

| 観測項目 | 期待する状態 | バグ状態での実際 |
| --- | --- | --- |
| `POST /products/{id}/suspension` | HTTP 204 | HTTP 204 |
| 商品の`status` | `SUSPENDED` | `ACTIVE` |
| 商品の`note` | リクエストの停止理由 | リクエストの停止理由 |
| 商品の`sku` | `SKU-001` | `SKU-001` |

バグ状態では、管理中の`Product`を取得した後に`@Modifying`のJPQL一括更新でDB上の`status`だけを`SUSPENDED`へ変更します。永続化コンテキスト内の`Product`は`ACTIVE`のままです。その後で同じエンティティの`note`を変更すると、トランザクション完了時のdirty checkingが古い`ACTIVE`を含むUPDATEを発行します。ログに最初のUPDATEが出ることと、最終DB状態が正しいことは別の事実として確認します。

## 必要な環境

| 項目 | バージョン |
| --- | --- |
| JDK | 21 |
| Maven | 3.8以降 |
| Spring Boot | 3.3.12 |
| DB | H2（テスト実行時のインメモリDB） |

## 修正後のテストを実行する

デフォルトブランチは修正済みです。次のコマンドはHTTP 204、最終状態の`SUSPENDED`、停止理由、SKUを同時に検証します。

```bash
mvn --batch-mode test
```

## バグを自分で再現する

バグを含むコミット`985da6e`では、同じテストが設定やコンパイルではなく、意図した状態差分で失敗します。

```bash
git checkout 985da6e
mvn --batch-mode -Dtest=ProductSuspensionApiTest test
# expected: SUSPENDED
#  but was: ACTIVE

git switch main
mvn --batch-mode -Dtest=ProductSuspensionApiTest test
# BUILD SUCCESS
```

## 調査の順番

| 段階 | 確認するもの | このプロジェクトで得られる事実 |
| --- | --- | --- |
| 1. 契約テスト | `ProductSuspensionApiTest` | 204応答と最終DB状態を分けて観測できる |
| 2. アプリケーションログ | `ProductSuspensionService`のINFOログ | 一括更新後も管理中エンティティの`status`が`ACTIVE`である |
| 3. SQLログ | `docs/01-bug-reproduction.log` | `SUSPENDED`への一括UPDATEの後に`ACTIVE`を含むUPDATEが発行される |
| 4. デバッガー | サービスの一括更新前後とメソッド終了直前 | `product.getStatus()`がいつ更新されないかを追える |
| 5. 回帰テスト | 同じHTTP統合テスト | 修正後に最終DB状態が`SUSPENDED`となる |

## プロジェクト構成

```text
src/main/java/com/example/bulkupdate/
├── product/Product.java                    # 管理対象エンティティ
├── product/ProductRepository.java           # JPAリポジトリ
├── product/ProductSuspensionService.java    # 停止処理のトランザクション境界
└── product/ProductController.java           # HTTP境界
src/test/java/com/example/bulkupdate/
└── ProductSuspensionApiTest.java             # HTTP応答と最終状態を検証する回帰テスト
docs/
├── 01-bug-reproduction.log                  # バグ状態の実行記録
├── 02-fixed-verification.log                 # 修正後の対象テスト実行記録
├── 03-full-test-suite.log                    # 修正後の全テスト実行記録
├── debugging-record.md                       # 仮説、原因、修正、制限
└── novelty-report.md                         # 既存題材との重複調査
```

詳細な観測と仮説の比較は[デバッグ記録](docs/debugging-record.md)を参照してください。

## References

[1] [Spring Data JPA `@Modifying` API](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/Modifying.html)

[2] [Jakarta Persistence 3.1 Specification](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1)
