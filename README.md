# PexelsSearch

Pexels API で写真を検索し、選んだ1枚を全画面で表示する Android アプリです。

## セットアップ

`local.properties` に API キーを追加してください。

```properties
PEXELS_API_KEY=ここにキーを書く
```

- キーは https://www.pexels.com/api/ で取得できます。
- 未設定でもビルドは通りますが（空文字にフォールバック）、API がキーを必須とするため動かすにはキーが必要です。

## 技術スタック

Kotlin / Coroutines・Flow / Jetpack Compose / MVI / Hilt / Retrofit + OkHttp / kotlinx.serialization / Coil / Navigation Compose（type-safe route）/ JUnit4 + MockK

---

## 1. 採用したアーキテクチャとその理由

クリーンアーキテクチャに、いちばん外側のリングを MVI で載せた構成です。単一モジュール `:app` のパッケージレイヤリングにしています。

- 依存の向きは `ui → domain ← data`。`domain` は Android にも Retrofit にも依存しない純粋な Kotlin。`PhotoRepository` はインターフェースを domain、実装を data に置いて逆転させる。
- 単一モジュールは2画面という規模の判断。依存規則はコンパイラではなくパッケージ規則と手動チェック（`ui` から `data` への import が0件、など）で守る。
- MVI はクリーンアーキテクチャと並ぶものではなく、プレゼンテーションのリングの実装方式。State / Intent / Effect は画面ごとの `XxxContract.kt`、その3点セットと単一の入口は `BaseViewModel` が強制。

### ユースケース層は置いていない

- オーケストレーターに当たるケースが0件（2画面・リポジトリのメソッド2つ）。判断はサイズではなく種類で、「1操作が協力者を2つ以上」「同じドメイン規則の呼び出し元が2箇所以上」のどちらも未発生。
- 宙に浮いたビジネスロジックも無し。終端判定は `next_page` の意味に紐づくのでリポジトリ / 画像サイズと `avg_color` はマッパー / HTTP からドメインエラーはエラーマッパー / 蓄積と重複除去は蓄積が起きる ViewModel。

### MVI にした結果

- 差が出るのは入力と状態更新の経路を1本に強制するか。入口は `onIntent` だけ（公開メンバーは各 ViewModel これ1つ）、`_uiState` は親の `private` で書けるのは `updateState(reduce)` 経由のみ。MVVM でも同じ形は書けるが、そちらは規約止まり。
- 効いたのは3箇所 — `SearchScaffold(state, onIntent, modifier)` が3引数で収まり切り出しが楽 / UI テスト3件が State を値として渡すだけで成立 / State の契約（成功なら `error` を消す、失敗なら結果を空にする）の見直しが `updateState` 一箇所で済む。

---

## 2. ユニットテストを書いた場所とその理由

ユニットテスト43件（6ファイル）、UI テスト3件です。

### どこに書いたか

「サードパーティだけで作らない部分」＝ページング・エラーマッピング・状態管理が3層に分かれていたので、そこに合わせました。

| ファイル | 層 | 件数 | 何を守っているか |
|---|---|---|---|
| `PhotoErrorMapperTest` | data | 5 | `Throwable` → `PhotoError`。外部の失敗がドメインに入る境界 |
| `PhotoMapperTest` | data | 5 | 画像サイズの選択と `avg_color` のパース（欠落・書式不正・桁数） |
| `PhotoRepositoryImplTest` | data | 5 | ページ終端の判定と `Outcome` への変換 |
| `OutcomeTest` | domain | 3 | `safeCall`。特に `CancellationException` を握り潰さないこと |
| `SearchViewModelTest` | ui | 17 | 検索の発火条件、ページングの蓄積と重複除去、エラー表示の畳み方 |
| `PhotoDetailViewModelTest` | ui | 8 | 初回取得・再試行・クレジット2リンクの Effect |

- 前半3つは外部 API の応答がドメインモデルに変わる境界、後半3つは自分で書いた Flow と状態管理が集まる場所。壊れると UI 側で手当てできないため。`PhotoRepositoryImplTest` の `空ページでも page は要求したページ番号になる` は、下の「実測」をテストで固定したもの。

### どこに書かなかったか

- `PexelsApiKeyInterceptor` — ヘッダーを1行足すだけで、検証する分岐が無いため。
- State の契約を守る4箇所（進行中ジョブのキャンセル、成功時に `error` を消す、など）— UI から到達できず、`coAnswers { delay() }` で人工的なインターリーブを作るしかないため。消しても43件が全部通ることは確認済み。

### テストが回帰を捕まえるか確認した

- 主要な分岐は実装をわざと壊して確認し、ほとんどは狙ったテストだけが落ちた。
- クエリの trim だけは壊しても通過。`coVerify(exactly = 1)` で正しい呼び出しだけを見て、余計な呼び出しが増えるのを見ていなかったため。`exactly = 0` を追加。

### UI テストを3件で切った理由

- 対象は `SearchScaffold`（状態を持たない Composable）のローディング / 空結果 / エラーの3分岐だけ。`SearchScreen` は ViewModel と Hilt が絡む割に、確認できるのが配線だけのため。
- 詳細画面は空結果が無く、ローディングとエラーも同じ `ui/common/` を使い回すため、足しても増える検証は `when` の分岐順だけ。画面ごとに増やすより分岐が実際にある場所を取った。

---

## 3. 時間の制約で意図的に実装しなかったもの

- Room / オフラインキャッシュ
- マルチモジュール分割
- Paging 3（ページングは手書き。理由は下）
- カスタムデザインシステム、凝ったアニメーション、共有要素トランジション
- 多言語対応（`strings.xml` に配置。既定ロケールのみ）
- CI/CD、カバレッジ計測、Detekt / ktlint
- ダークテーマの細かい調整（Material3 の既定のまま）
- 詳細画面のズーム・パン
- 検索結果とスクロール位置のプロセス再生成復元（検索語だけ復元）

### 無限スクロールだけは入れた

- ページングが増やすのは Flow そのものではなく状態管理。ページの蓄積と `distinctBy(Photo::id)` による重複除去、`isLoading` と `isLoadingMore` の切り分け、重複リクエストの遮断（ガード return・`loadMoreJob?.cancel()`・`searchedQuery` の分離）が必要になる。その代わり他の追加機能は全部見送り、ここ一箇所に寄せた。

### Pexels API の実測

手書きにしたぶん応答を直接見ることになり、ドキュメントとの食い違いに気づきました。

| | ドキュメントの記述 | 手元で観測した挙動 |
|---|---|---|
| `next_page` | 対応するページがある場合のみ返る | データがある最後のページにも付き、0件ページで初めて消える |
| `total_results` | 検索結果の総件数 | 実データよりはるかに多い。`xylophone` は473件（6ページ）で尽きるのに 3932 |
| 空ページの `page` / `per_page` | 現在のページ番号 / 各ページの件数 | 要求と無関係な固定値。page=7 でも page=999 でも `{"page":1,"per_page":1,"photos":[],"total_results":0}` |

- 終端は `photos.isEmpty() || nextPage == null` の2条件で判定。`next_page` は膨らんだ `total_results` から計算されるらしく実データが尽きても付き続けるため、空ページを1回余分に受け取るコストは受け入れた。
- 応答ボディのページ情報は信用せず `PhotoPage.page` には要求した値を入れ、DTO からも `page` を外して「使う値」と誤読されないようにした。

---

## 4. 実務プロジェクトなら追加で対応すること

- **リポジトリにキャッシュを載せる。** 詳細画面は一覧が取得済みのデータを捨てて `GET /v1/photos/{id}` を引き直すため、タップのたびにローディングが見える。id で先にキャッシュを引ければ、詳細は「自分で取得する」ままで即座に返り、State の契約も NavHost も変わらない。
- マルチモジュール分割。上の依存規則を、手動チェックではなくコンパイラに強制させる。
- Timber / Crashlytics と CI。ビルド・テスト・lint は今のところ手元で回しているだけ。
- 無意味なクエリは弾いていない。`zzqqxxjjvvww9911` でも0件ではなく無関係な写真が20件返り、`total_results` も3000台で実在キーワード（`cat` で8000）と桁で区別できず、関連度スコアも一致フラグも応答に無いため。まず検索品質をログに取り、弾く根拠を作るところから。
