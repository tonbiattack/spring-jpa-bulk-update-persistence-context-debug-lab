# 題材重複調査レポート

> 生成日時: `2026-08-15 10:50 UTC`

## 候補題材

| 項目 | 内容 |
| --- | --- |
| 言語 | Java |
| 題材名 | Spring Data JPAのバルク更新後に古いエンティティがflushされる問題をデバッグする |
| 契約 | 商品を停止してメモを更新する操作で、停止済みを期待するが、バグ状態ではJPAバルク更新後にPersistence Contextの古い状態がflushされてACTIVEへ戻る |
| 検索語 | Spring Data JPA, bulk update, @Modifying, persistence context, flush, stale entity, Hibernate |
| カタログ | `/home/ubuntu/work/repository-catalog/data/repositories.json`（448件） |

## 自動検索の結論

**高近接候補あり**。語彙的に近い既存題材があります。原因、実境界、観測契約、最小修正がすべて異なることを確認するまで作成しないでください。

この結果は語彙的な一次スクリーニングであり、重複の最終判定ではありません。候補がある場合は、該当リポジトリのREADME、失敗するテスト、原因、観測契約を比較してください。

## 近接候補

| リポジトリ | スコア | 共通語 | 言語 | 内容 |
| --- | --- | --- | --- | --- |
| [spring-jpa-optimistic-lock-debug-lab](https://github.com/tonbiattack/spring-jpa-optimistic-lock-debug-lab) | 17 | data, jpa, spring, update | Java | Spring Data JPAの楽観ロック漏れによるLost Updateを再現して修正するデバッグラボ |
| [spring-jpa-orphan-removal-debug-lab](https://github.com/tonbiattack/spring-jpa-orphan-removal-debug-lab) | 12 | data, jpa, spring | Java | Spring Data JPAのorphanRemoval未設定による子エンティティ削除漏れを再現して修正するデバッグラボ |
| [spring-batch-chank-on-tasklet](https://github.com/tonbiattack/spring-batch-chank-on-tasklet) | 9 | jpa, spring | Java | Spring Batch Tasklet の中で Chank を使用する |
| [employee-management](https://github.com/tonbiattack/employee-management) | 6 | spring | Java | Spring Boot Javaで作成されたグループ企業の社員情報を管理するためのWEB API |
| [error-driven-java-lab](https://github.com/tonbiattack/error-driven-java-lab) | 6 | spring | Java | 失敗するテストから学ぶJavaとSpring Bootのデバッグ教材 |
| [introduction-to-spring-for-professionals](https://github.com/tonbiattack/introduction-to-spring-for-professionals) | 6 | spring | Java | プロになるためのSpring入門 ――ゼロからの開発力養成講座 |
| [nuxt-bff-nest-spring](https://github.com/tonbiattack/nuxt-bff-nest-spring) | 6 | spring | Java | Nuxt をフロントエンド、NestJS を BFF、Spring Boot をバックエンド API として構成したサンプルアプリです。Nuxt は画面表示に集中し、データ取得や加工は NestJS の BFF が担当します。Spring Boot は業務ロジック、DB アクセス、Redis キャッシュを担当します。 |
| [SampleSpringBoot](https://github.com/tonbiattack/SampleSpringBoot) | 6 | spring | Java | 説明未設定（READMEで補足） |
| [spring-boot-transaction-debug-lab](https://github.com/tonbiattack/spring-boot-transaction-debug-lab) | 6 | spring | Java | Spring Bootのトランザクション境界をデバッグする再現可能なサンプル |
| [spring-cache-key-debug-lab](https://github.com/tonbiattack/spring-cache-key-debug-lab) | 6 | spring | Java | Spring Cacheのキーからテナントを漏らす問題を再現してデバッグするサンプル |
| [spring-checked-exception-rollback-lab](https://github.com/tonbiattack/spring-checked-exception-rollback-lab) | 6 | spring | Java | Spring Bootで検査例外がロールバックされない現象を再現してデバッグするサンプル |
| [Spring-Framework-Super-Introduction](https://github.com/tonbiattack/Spring-Framework-Super-Introduction) | 6 | spring | Java | Spring Framework 超入門 ～やさしくわかるWebアプリ開発～ |

## 手動比較の記録

| 比較対象 | 既存題材の原因・境界・契約 | 今回の差分 | 判定 |
| --- | --- | --- | --- |
| `spring-jpa-optimistic-lock-debug-lab` | 直接原因は `@Version` がないため、二つの古い在庫スナップショットの保存が競合として拒否されないことです。`InventoryService.save` を二回実行し、例外と最終在庫数を観測します。最小修正はエンティティへの `@Version` 追加です。 | 今回の直接原因は `@Modifying` によるJPQL一括更新が永続化コンテキストを通らず、同一トランザクションで管理中の古いエンティティが後続のdirty checkingで再度flushされることです。一つのサービス操作後にDBを読み直し、停止状態と更新メモを観測します。最小修正は単一エンティティの状態変更を管理中エンティティへ集約することです。 | 重複なし |
| `spring-jpa-orphan-removal-debug-lab` | 直接原因は親子関連に `orphanRemoval = true` がないことです。親コレクションから明細を外す境界で、子レコード数と存在有無を観測します。最小修正は関連マッピングへの `orphanRemoval` 指定です。 | 今回は関連マッピングや削除契約を扱いません。バルク更新と永続化コンテキストの二経路更新が同じ行の状態を矛盾させることを扱います。最小修正も関連マッピングではなく、更新経路の統一です。 | 重複なし |
| `spring-boot-transaction-debug-lab` / `spring-checked-exception-rollback-lab` | 直接原因はトランザクション境界または検査例外に対するロールバック規則です。例外発生後の注文・監査ログの状態を観測します。最小修正は別トランザクションへの分離または `rollbackFor` 指定です。 | 今回は例外もロールバック規則も使いません。サービスメソッドが正常終了しているにもかかわらず、先行したバルク更新が最後のflushで上書きされる問題を扱います。 | 重複なし |

## 作成可否

- [x] カタログは更新済みである。`refresh_catalog.py` と `validate_catalog.py` を2026-08-15に実行し、448件を検証した。
- [x] 近接候補のREADMEまたは主要テストを確認した。`spring-jpa-optimistic-lock-debug-lab`、`spring-jpa-orphan-removal-debug-lab`、およびトランザクション系2件のREADMEを確認し、前二者は主要テストも確認した。
- [x] 原因、実境界、観測契約、最小修正の差分を記録した。
- [x] 同じ失敗を別名で再実装していない。
- [x] Spring Boot統合テストでHTTP境界と最終DB状態を確認し、バグ・修正を別コミットにする計画がある。 
