# 時間数入力 UI の調査

Android（Jetpack Compose）のアプリで、時間の長さ（N時間M分）を入力する UI の方式を選ぶための資料である。調査日は 2026-11-12。サンプルのアプリは、この資料をもとに別に作る。

TalkBack と UIAutomator2 での振る舞いは、AndroidX と AOSP のソースから読み取った。推奨した 4 つのパターンは、そのあとサンプルのアプリを作ってエミュレータで確かめ、結果をこの資料に反映した。詳しい結果は [docs/sample-app.md](sample-app.md) にある。実機では確かめていない。アプリの事例も、実機では確かめていない。

## 結論（推奨するパターン）

時間の長さを入力する標準の部品は、Android、Material（M2、M3）、web（HTML）、iOS のどれにもない。M2 と M3 の time picker は時刻用で、時は 0〜23 に固定されている。iOS の countdown timer も 23 時間 59 分が上限である。そのため、どの案も標準の部品の組み合わせか、自作になる。

サンプルのアプリにする価値があるのは次の 4 つと考える。どれを使うかはこの資料では決めず、推奨に留める。

1. 時と分の 2 つのドロップダウン（read-only の Exposed dropdown menu）。M2 に部品（`ExposedDropdownMenuBox`）があり、ガイドラインの支持がもっとも多い。UIAutomator2 では項目を文字で探してタップするだけで値が決まる。ステップ数（4 回のタップ）と、時の項目数（範囲が 0〜25 なら 26、0〜48 なら 49）が多すぎないかは、実際に操作して確かめる必要がある
2. 値をすべて並べた 1 つのリスト。値ごとに「翌1:00」のようなラベルを自由に付けられ、刻みと最大値はリストの中身でそのまま表せる。2 回のタップで決まるが、10 分刻みで 0:00〜25:00 なら 151 項目になり、長いリストを避けるという NN/g の指摘に当たる。1 と並べて比べると、ステップ数と項目数のどちらを取るかを判断しやすい
3. ホイール（時と分の 2 列）。View の `NumberPicker` に近い。material3 1.5.0 で汎用のホイール `ScrollField` が入り、TalkBack への公開方法（1 つの調整可能な要素として値を読ませる）の公式の手本ができた。M2 だけで作るなら、この方法をまねて自作する。View の `NumberPicker` との操作感の差を確かめるために作る価値がある
4. ステッパー（+ と − のボタン）。Android の「サイレント モード」（Do Not Disturb）の時間の設定が、決まった値の並び（15 分、30 分、45 分、1〜12 時間）を + と − でたどる作りになっている。M2 の部品だけで作れ、UIAutomator2 でも操作しやすい。ただし値が既定値から遠いとタップの回数が増えるので、よく選ばれる値に偏りがある場合に限って候補になる

スライダー、キーパッド、テキストフィールド、M3 の `TimePicker` は推奨しない。理由は「パターンごとの説明」に書いた。

## 要件

次の条件を要件として想定した。

- 24 時間を超える値を入力できる
- ラベルを変えられる（例: 0:00〜翌1:00）
- 値には刻み（0:00、0:10、0:20 のような）と最大値がある
- ユーザーにとって驚きの少ない、Android の標準に近い UX であること
- TalkBack で使える
- UIAutomator2 で容易に操作できる
- M2（Compose の `androidx.compose.material`）で作れる

まだ決まっていないことは、比較の中で条件として扱う。

- 時の範囲（0〜25 か、0〜48 か）
- 分の刻みが画面によって違うか
- よく選ばれる値に偏りがあるか
- iOS と Android で同じ UI にすることが要件か

調査の前提は次のとおり。有限の選択肢なら、自由入力より選ばせる UI が筋である。部品の組み合わせを一から考えず、ガイドラインや事例に乗る。

## パターンの比較表

比較に使ったライブラリのバージョンは、`androidx.compose.material` 1.12.1、`androidx.compose.material3` 1.4.0（1.5.0 は 2026-11-04 に rc01）、`androidx.test.uiautomator` 2.4.1 である。

ステップ数は、画面に入力欄が見えている状態から値が決まるまでのタップやドラッグの回数とした。分の刻みは 10 分、時の範囲は 0〜25 と 0〜48 の 2 とおりで数えた。

| パターン | Android の標準との近さ | TalkBack | UIAutomator2 | M2 で作れるか | 24 時間超、ラベル、刻み、最大値 | ステップ数 |
|---|---|---|---|---|---|---|
| 1. 時と分の 2 つのドロップダウン | M2、M3 の Menus に正式な用途として「Select menus」がある。AOSP のカレンダーの通知の時刻が `Spinner` | M2 のままでは TalkBack からメニューを開けない。`TextField` に `Role.DropdownList` とメニューを開く `onClick` を付けると「プルダウンリスト」と読まれ、開ける | 欄と項目を testTag で探してタップ。画面外の項目はスワイプ。`scrollUntil` は使えなかった | 可。`ExposedDropdownMenuBox` は `@ExperimentalMaterialApi` | すべて可。最大値のある時は分の選択肢を絞る処理が要る | 4 回のタップ。時の項目が画面に収まらなければスクロールも |
| 2. 値をすべて並べた 1 つのリスト | 1 と同じ部品。AOSP のカレンダーの通知、画面消灯の時間が固定のリスト | 1 と同じ | 1 と同じ。0〜25 の 25:30 は、スワイプ 19 回で約 33 秒 | 可（1 と同じ部品） | すべて可。値ごとにラベルを自由に付けられる | 2 回のタップと、リストのスクロール。項目は 0〜25 なら 151、0〜48 なら 289 |
| 3. ホイール | `NumberPicker`（View）、M3 1.5.0 の `ScrollField`。Samsung がホイールを使っているかは未確認 | 「0時間. 時. スライダー」のように 1 つの要素として読まれ、上下のスワイプで値が変わる（`ScrollField` と自作で同じ）。自作では semantics を自分で書く | `setProgress` を `UiObject2.getAccessibilityNodeInfo()`（2.4.0 以降）経由で実行して 0.1〜2.1 秒。スワイプでも入る | M2 だけなら自作。`ScrollField` は material3 が要る | すべて可。`ScrollField` は必ず循環する | 列ごとにドラッグかフリング（2 列で 2 回以上） |
| 4. ステッパー | Android の「サイレント モード」の時間、Google Health の睡眠目標 | ボタンと値の文字を読ませる。値の変化を伝える仕組みが要る | ボタンを文字や testTag で探して必要な回数だけタップ | 可（`IconButton` と `Text`） | すべて可。値の並びをたどれば刻みが不均一でもよい | 既定値から目標までの刻みの数だけタップ |
| 5. スライダー | M2、M3 の `Slider`。時間の長さに使う Android の事例は見つからなかった | 既定ではパーセントで読まれる。`stateDescription` で上書きできる | `setProgress` を `getAccessibilityNodeInfo()` 経由で実行するか、ドラッグ | 可（`Slider`） | 可。ただし刻みは均一のみ。ラベルは読み上げの文字として付ける | 1 回のドラッグ。正確に合わせるには微調整 |
| 6. キーパッド | Google の時計のタイマー（AOSP）、Samsung の時計のタイマー | 数字のボタン | 数字のボタンをタップ | 自作（`Button` の並び） | 24 時間超は可。刻みと最大値に合わない値が入力できてしまう | 桁の数だけタップ（1:30 なら 3 回）と確定 |
| 7. テキストフィールド | M2、M3 の Text fields | 標準の `TextField` として読まれる | `setText()` | 可（`TextField`） | 24 時間超は可。刻みと最大値は入力後の検証になる | 欄のタップ、文字の入力、確定 |
| 8. M3 の `TimePicker` / `TimeInput` | M3 の time pickers | 標準の部品として読まれる | 未調査 | 不可（material3 のみ） | 不可。時は 0〜23 に固定、刻みと最大値の設定なし | 対象外 |

## パターンごとの説明

### 1. 時と分の 2 つのドロップダウン

時と分を、それぞれ read-only の Exposed dropdown menu（選んでいる値を欄に見せ、タップで選択肢の一覧を開く部品）にする。

ガイドラインの支持がもっとも多い。M2 と M3 の Menus は、正式な用途に「Select menus」を挙げている。Material Design 1 には、フォームの中では「segmented dropdown buttons」を使うよう勧める記述がある。NN/g は、複数の欄が 1 つのまとまりとして読まれるときはドロップダウンでまとめる構成を支持している。CVS Health は、read-only の Exposed dropdown menu はアクセシビリティを高くできると評価している。

一方で、NN/g はドロップダウンに向くのは選択肢が少ない場合で、15 を大きく超えるなら combobox（文字を入れて候補を絞り込める欄）を検討するよう書いている。分は 10 分刻みなら 6 項目で収まるが、時は範囲が 0〜25 なら 26 項目、0〜48 なら 49 項目になる。GOV.UK Design System は、公共サービスでは select を最後の手段にとどめるよう書いている。

M2 で作れる。`ExposedDropdownMenuBox` は M2 にあり、1.1.0 で入ったが、今も `@ExperimentalMaterialApi` が付いている。M2 の `ExposedDropdownMenuBox` は、欄に「Dropdown menu」に当たる `contentDescription` と `onClick` を付けるだけで、role は付けない（`ExposedDropdownMenu.kt` の `Modifier.expandable`）。M3 は `Role.DropdownList` を付ける。CVS Health は、ドロップダウンは「Drop down list」という role を読み上げるべきだと書いている。`Role.DropdownList` は、ソースでは Android の `android.widget.Spinner` のクラス名に対応づけられている。

サンプルのアプリで確かめると、M2 の `ExposedDropdownMenuBox` は、そのままでは TalkBack からメニューを開けなかった。メニューを開く `onClick` を持つ要素は `focusable="false"` で、TalkBack はそれを飛ばして中の read-only の `TextField` にフォーカスし、「0. 時」と読む。ダブルタップしても、メニューは開かない。この要素に `Role.DropdownList` を付けても、UI の階層でのクラス名は `android.view.View` のままだった。M3 の `menuAnchor` と同じく、`TextField` に `Role.DropdownList` とメニューを開く `onClick` を付けると、「0. プルダウンリスト. 時」と読まれ、ダブルタップで開けた。開いたあとも TalkBack のフォーカスは欄に残ることが多く（5 回のうち 4 回）、メニューが開いたことは読み上げられなかった。項目は「1」「2」のように数字だけが読まれ、右へのスワイプで画面外の項目まで進めた。

UIAutomator2 では、欄を testTag で探してタップし、開いた一覧の項目を testTag で探してタップすれば値が決まる。testTag で探すには、`Modifier.semantics { testTagsAsResourceId = true }` が要る。サンプルのアプリでは、メニューは別のウィンドウ（Popup）に出るため、ルートに付けた `testTagsAsResourceId` が届かず、メニューの modifier にも付ける必要があった。画面外の項目はアクセシビリティのツリーに出ないので、スクロールして探す。`UiObject2.scrollUntil` は、メニューから `TYPE_VIEW_SCROLLED` を受け取れず、5 回スワイプしたところで止まった。そこで `UiDevice.swipe` でスワイプを繰り返した。時の 25 は 2 回のスワイプで見つかり、25:30 を入れるのに約 8.5 秒かかった。

最大値が時の区切りにない場合（例: 最大 25:30）、時が 25 のときに分の選択肢を 0〜30 に絞る処理が要る。時を変えたときに、選んでいた分が範囲を超えたらどうするかも決める必要がある。

### 2. 値をすべて並べた 1 つのリスト

0:00、0:10、…、翌1:00 のように、選べる値をすべて 1 つのリストに並べる。部品は 1 と同じ Exposed dropdown menu か、ダイアログのラジオボタンの一覧にする。

ラベルの自由度がもっとも高い。値ごとに任意の文字列を付けられ、刻みが不均一でもよく、最大値もリストの最後の項目で決まる。MDN は、spinbutton で読み上げの文字が重要なら、select と option を使うよう勧めている。USWDS の time picker は、刻みごとの候補を combobox の一覧に並べ、文字を入れて絞り込む作りである。

Android の事例では、AOSP のカレンダーが通知の時刻を `Spinner` で選ばせる。選択肢は 0 分〜1 週間の 16 項目で、刻みは不均一である。設定アプリの画面消灯の時間は、7 項目のラジオボタンの一覧である。どちらも項目は少ない。

懸念は項目数である。10 分刻みなら、0:00〜25:00 で 151 項目、0:00〜48:00 で 289 項目になる。NN/g は、スクロールしないと全体が見えない長いドロップダウンを避けるよう書いている。M3 の Menus は、長いリストには文字で絞り込む autocomplete を使えると書いている。一方、CVS Health は、編集できる autocomplete はアクセシビリティが下がると評価している。

UIAutomator2 では、項目を文字で探してタップする。画面外の項目はスクロールが要る。

### 3. ホイール

時と分をそれぞれ縦にスクロールする列にし、中央の値を選ぶ。View の `NumberPicker` と同じ形である。

Compose の公式の部品は次の 3 つで、どれも M2 からはそのまま使えない。

- `ScrollField`（material3 1.5.0、2026-11-04 に rc01）。項目数を指定する汎用のホイールで、`fieldAccessibilityDescription` で項目ごとの読み上げの文字を返せる。項目は必ず循環する（`InfinitePageCount = 100_000` のページを持つ `VerticalPager`）。同時に入った `TimeScroll` は `TimePickerState` を使うので、時は 0〜23 に固定である
- Wear の `Picker`。Wear OS 向けのアーティファクト（`androidx.wear.compose`）にあり、公式のドキュメントはスマートフォン向けの Material と混ぜないよう注意している
- `NumberPicker`（View）。`AndroidView` で包めば使える。`setDisplayedValues` で任意のラベルを付けられる

TalkBack への公開方法は、部品によって違う。

- `ScrollField` は、列全体に `clearAndSetSemantics` で `contentDescription`、`stateDescription`（今の項目のラベル）、`progressBarRangeInfo`、`setProgress` を付ける。Android では列全体が `android.widget.SeekBar` のクラス名を持つ 1 つの要素になり、今の項目のラベルが読まれ、スクロールのアクション 1 回で 1 項目動く。項目そのものはアクセシビリティのツリーに含まれない。サンプルのアプリでは、TalkBack は「0時間. 時. スライダー」と読み、上へのスワイプで「1時間」に、0時間から下へのスワイプで「25時間」に変わった
- Wear の `Picker` は `Role.ValuePicker` を付け、値は `contentDescription` で伝える。`setProgress` はない
- `NumberPicker` は、独自の `AccessibilityNodeProvider` で、前後の値のボタンと入力欄を仮想の子要素として公開する。スクロールのアクションで 1 つずつ値が変わる。`ACTION_SET_PROGRESS` はない

`Role.ValuePicker` は Compose UI 1.8 で入った。KDoc には、次と前の値を選ぶために `ACTION_SCROLL_FORWARD` と `ACTION_SCROLL_BACKWARD` に対応すべきだと書かれている。ただし、role を付けただけではこれらのアクションは付かない。M2 だけで作るなら、`ScrollField` か `NumberPicker` の方法をまねて、semantics を自分で書くことになる。

UIAutomator2 には、`setProgress` に当たるメソッドがない。`UiObject2.scroll` はスワイプのジェスチャーで、アクセシビリティのアクションは使わない。2.4.0 から `UiObject2.getAccessibilityNodeInfo()` が公開されたので、`performAction` で `ACTION_SET_PROGRESS` を実行できる。サンプルのアプリで確かめると、`ScrollField` と自作のホイールのどちらも、0.1〜2.1 秒で値が入った。スワイプで入れると、`ScrollField` は循環するので近い向きに回せて 2〜4 回で済んだ。自作のホイールは 28〜30 回のスワイプが要り、約 40 秒かかった。`ScrollField` の場合、`setProgress` に渡す値は項目の番号ではなくページの番号（0〜99,999）なので、今のページから差を計算する必要がある。自作なら、範囲を項目の番号にできる。Appium の UiAutomator2 driver は、要素が `ACTION_SET_PROGRESS` を持っていれば、`sendKeys` の文字を数値として `ACTION_SET_PROGRESS` を実行する。

M3 の Time pickers のアクセシビリティのページは、ダイヤルだけでなく、文字で入力する手段も用意するよう書いている。ホイールだけで入力させる場合は、この点が当てはまるかを確かめる必要がある。

ライブラリの候補は、どちらも M2 の要件に合わない。

- kez-lab/Compose-Pickers は、公開されている最新が 0.7.0（2026-08-09）で、0.8.0 は未公開である。0.8.0 では public のパッケージ名がすべて `com.kez.picker` から `io.github.kezlab.compose.pickers` に変わる。Compose Multiplatform 向けで、`org.jetbrains.compose.material3` に `implementation` で依存する。`DurationPicker` は時のリストを任意に渡せるので、24 時間超は扱える。semantics は、列に `collectionInfo` と前後の値に動く `customActions` を、項目に `Role.Button` を付ける。`setProgress` はない
- Seo-4d696b75/compose-m3-picker は、最新が 0.2.2（2026-08-09）で、material3 1.4.0 に `api` で依存する。semantics の記述が 1 つもなく、スクロールは独自の `pointerInput` で処理している

### 4. ステッパー

値を表示し、その両側に − と + のボタンを置く。

Android のシステムの事例がある。「サイレント モード」の時間のダイアログ（AOSP の `EnableZenModeDialog` と `ZenDurationDialog`）は、`ZenModeConfig.MINUTE_BUCKETS` の値を + と − で 1 つずつたどる。値は 15 分、30 分、45 分と、1 時間から 12 時間までの 1 時間ごとである。両端ではボタンが無効になる。一定の刻みを足すのではなく値の並びをたどる作りなので、刻みが不均一でも、ラベルが任意でも扱える。Google Health アプリの睡眠目標も、+ と − で時間を変えるとヘルプに書かれている。刻みと上限は書かれていない。

NN/g は、1 から 50 まで変えるような大きな調整にはステッパーは向かないと書いている。よく選ばれる値がはっきりある欄に使い、刻みと範囲を明示するよう勧めている。10 分刻みで 0:00 から 25:00 まで動かすと 150 回のタップになる。時と分を別のステッパーにすれば、0〜25 の範囲で最大 25 回と 5 回になる。

M2 の `IconButton` と `Text` で作れる。TalkBack では、ボタンの `contentDescription`（例: 「10 分増やす」）と値の文字を読ませる。ボタンを押したあとに新しい値を伝えるには `liveRegion` などが要る。CVS Health は、`liveRegion` に既知の問題があり、同じ semantics に `contentDescription` を付けて回避すると書いている。全体を 1 つの調整可能な要素（`progressBarRangeInfo` と `setProgress`）にする作り方もある。ボタンは 48dp 以上にする（Android の公式のドキュメント）。

UIAutomator2 では、ボタンを文字か testTag で探してタップする。値までの回数だけタップを繰り返す。

### 5. スライダー

M2 の `Slider` の `steps` で刻みを付けられる。0〜1,500 分を 10 分刻みにするなら、`valueRange = 0f..1500f`、`steps = 149` である。

NN/g は、正確な値が必要な場合にスライダーは向かないと書いており、Kayak のアプリの「Duration」のスライダーを、分単位まで合わせさせる悪い例として挙げている。M3 の Sliders は、停止点を多くしすぎないよう書いている。

TalkBack では、`Slider` は `android.widget.SeekBar` のクラス名で公開され、スクロールのアクション 1 回で 1 刻み動く。`stateDescription` がなければパーセントが読まれる。M2 の `Slider` は `stateDescription` を付けないので、`Modifier.semantics { stateDescription = "1時間30分" }` を渡せば上書きできる。CVS Health も、この上書きとキーボード操作の対応を求めている。

UIAutomator2 では、`getAccessibilityNodeInfo()` 経由で `ACTION_SET_PROGRESS` を実行するか、ドラッグする。

### 6. キーパッド

Google の時計のタイマー（AOSP の DeskClock の `TimerSetupView`）は、0〜9 の数字のボタンで、右から時、分、秒の 6 桁を埋める。それぞれの 2 桁は 00〜99 を受け付け、最大は 99 時間 99 分 99 秒である。Samsung の時計のタイマーも、ヘルプでは「Use the keypad」と書かれ、最大は 99:59:59 である。

24 時間超は扱えるが、10 分刻みと最大値には合わない。0:15 や上限を超える値が入力できてしまい、それをどう扱うかが利用者の驚きになる。自由入力の問題と同じなので、推奨しない。

### 7. テキストフィールド

M2 と M3 の Text fields は、単位を接尾辞で示す方法と、検証で出すエラーの文字を示す方法を書いている。UIAutomator2 では `setText()` で入れられる。ただし、刻みと最大値は入力後の検証になり、6 と同じ理由で推奨しない。

GOV.UK Design System と M3 の Time pickers は、ピッカーを使う場合も文字で入力する手段を残すよう書いている。主な UI にはしないが、ほかのパターンの補助として使う余地はある。

### 8. M3 の `TimePicker` / `TimeInput`

`TimePickerState` は `initialHour` を 0〜23 に制限し（`require(initialHour in 0..MaxHourValue)`、`MaxHourValue = 23`）、刻み、最小値、最大値の設定はない。24 時間超の要件を満たせず、M2 では使えない。ただし、M3 の Time pickers のガイドラインにある「Hours and minutes should have separate inputs.」は、1 や 3 で時と分を分ける根拠になる。

### 補助: プリセットのチップ

よく使う値をチップやボタンで並べる。Digital Wellbeing の Focus mode の「Take a break」が例で、2019 年の記事では 5 分、15 分、30 分だった。全範囲を入力する手段にはならないので、よく選ばれる値に偏りがあれば、ほかのパターンに添える。M2 の `Chip` は `@ExperimentalMaterialApi` が付いている。

## ガイドライン

引用は原文のまま、短く載せる。Material の m2.material.io と m3.material.io は、ヘッドレスの Chrome で描画したあとの本文から引用した。

### Material Design 3

- Time pickers（https://m3.material.io/components/time-pickers/guidelines ）: 「Time pickers are not ideal for nuanced or granular time selection, such as milliseconds for a stopwatch application.」「Hours and minutes should have separate inputs.」時刻の入力用で、24 時間表示かどうかはシステムの設定に従う
- Time pickers のアクセシビリティ（https://m3.material.io/components/time-pickers/accessibility ）: 「Time pickers should allow manual time entry through text input, rather than exclusively through the dial selector.」ダイヤルの読み上げの例として「Hour 7 of 12」が挙がっている
- Sliders（https://m3.material.io/components/sliders/guidelines ）: 「Avoid having too many stop indicators on a slider, because it can become visually crowded and difficult to adjust the value.」
- Menus（https://m3.material.io/components/menus/guidelines ）: 用途として「Text field dropdown menus」「Select menus」を挙げる。「A menu can include a text field to filter options. This pattern is also known as autocomplete.」
- Chips（https://m3.material.io/components/chips/guidelines ）: 「They're best used to help people accomplish their current task faster and easier.」

### Material Design 2

- Time pickers（https://m2.material.io/components/time-pickers ）: Android の実装の節に「minute is a [0, 60) value and hour is a [0, 23] value regardless of which time format you choose.」
- Sliders（https://m2.material.io/components/sliders ）: 「Editable numeric values allow users to set the exact value of a slider.」ただし標準の機能ではないと書かれている
- Menus（https://m2.material.io/components/menus ）: 「Exposed dropdown menus display the currently selected menu item above the list of options.」
- Material Design 1 の Pickers（https://m1.material.io/components/pickers.html ）: 「For inline display, such as on a form, consider using compact controls such as segmented dropdown buttons.」

### Nielsen Norman Group

- 8 Design Guidelines for Input Steppers（https://www.nngroup.com/articles/input-steppers/ ）: 「when users need to change a value from 1 to 50, an input stepper is not a wise choice.」「Clarify the step value and the stepper range.」
- Slider Design: Rules of Thumb（https://www.nngroup.com/articles/gui-slider-controls/ ）: 「But whenever the exact value matters, sliders are not okay.」Kayak の Duration のスライダーを例に挙げている
- Dropdowns: Design Guidelines（https://www.nngroup.com/articles/drop-down-menus/ ）: 「Very long dropdowns that require scrolling make it impossible for users to see all their choices in one glance.」
- Does Your Form Really Need a Dropdown List?（https://www.nngroup.com/articles/dropdown-list/ 、2026-07-17）: 「When the list grows well beyond 15 options, consider a combobox.」「Sometimes users interpret multiple fields as a single unit or review them altogether.」

### GOV.UK Design System

- Select（https://design-system.service.gov.uk/components/select/ ）: 「The select component should only be used as a last resort in public-facing services because research shows that some users find selects very difficult to use.」
- Dates（https://design-system.service.gov.uk/patterns/dates/ ）: 「Allow users to enter the date into a text input as well as use the control.」
- 時刻と時間の長さの正式な部品はない。バックログの issue（https://github.com/alphagov/govuk-design-system/issues/5888 ）には、調査で「Lots of confusion around what midnight is - some users entering 12:00, some 00, some 24.」という結果が報告されている。「翌1:00」のように日をまたぐことをラベルで示す場合の参考になる

### Android の公式のドキュメント

- Semantics（https://developer.android.com/develop/ui/compose/accessibility/semantics ）: 「For custom components that track progress, you might want to notify users of their progress changes, including the current progress value, its range, and step size. You can do so with progressBarRangeInfo semantics」
- `stateDescription` の API リファレンス（https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/package-summary ）: 「If this not set, accessibility services will derive the state from other semantics properties, like ProgressBarRangeInfo, but it is not guaranteed and the format will be decided by accessibility services.」
- `Role.ValuePicker`（https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/Role ）: 「This element is a value picker. It should support the following accessibility actions to enable selection of the next and previous values」
- UIAutomator との相互運用（https://developer.android.com/develop/ui/compose/testing/interoperability ）: 「If you want to access any composable that uses Modifier.testTag, you need to enable the semantic property testTagsAsResourceId for the particular composable's subtree.」
- API defaults（https://developer.android.com/develop/ui/compose/accessibility/api-defaults ）: タッチの対象は 48dp 以上にする
- `NumberPicker`（https://developer.android.com/reference/android/widget/NumberPicker ）: `setDisplayedValues` で表示する値を任意の文字列にできる

### CVS Health の android-compose-accessibility-techniques

picker、ステッパー、ホイールのページはない。

- Exposed Dropdown Selection Menus（https://github.com/cvs-health/android-compose-accessibility-techniques/blob/main/doc/components/ExposedDropdownMenus.md ）: 「This pattern can be made very accessible in its read-only form, but it is less accessible when used to suggest values for an editable text entry field」「make sure any dropdown selection menu ... announces the role "Drop down list."」例は M3 の `menuAnchor` を使っており、M2 の例については「they do not include the necessary accessibility remediations」と書かれている
- Slider and RangeSlider Controls（https://github.com/cvs-health/android-compose-accessibility-techniques/blob/main/doc/components/SliderAndRangeSliderControls.md ）: 「`Slider` and `RangeSlider` controls require remediations in order to be accessible.」既定の「xx percent」の読み上げが合わないなら `stateDescription` を付けるよう書いている
- UX Change Announcements（https://github.com/cvs-health/android-compose-accessibility-techniques/blob/main/doc/interactions/UXChangeAnnouncements.md ）: `liveRegion` の既知の問題と回避策

### web（ARIA Authoring Practices Guide、HTML）

Android には ARIA Authoring Practices Guide に当たる資料が見つからなかった。そこで web の資料を、対応する Compose の semantics と並べて読んだ。

- Spinbutton（https://www.w3.org/WAI/ARIA/apg/patterns/spinbutton/ ）: 「For example, in a widget that enables users to set an alarm, a spinbutton could allow users to select a number from 0 to 59 for the minute of an hour.」大きな刻みの例として、上下の矢印で 1 分、Page Up と Page Down で 10 分動かす操作を挙げている。日、月、年の 3 つの spinbutton で日付を入れる例は、deprecated と書かれている
- Slider の Media Seek の例（https://www.w3.org/WAI/ARIA/apg/patterns/slider/examples/slider-seek/ ）: 値を秒で持ち、`aria-valuetext` で分と秒として読ませる。「If aria-valuetext were not used, assistive technology users would be told that the position is 243」。Compose では `stateDescription` に当たる
- Listbox（https://www.w3.org/WAI/ARIA/apg/patterns/listbox/ ）と Combobox の select-only の例（https://www.w3.org/WAI/ARIA/apg/patterns/combobox/examples/combobox-select-only/ ）: ラベル付きの選択肢から選ぶ。Exposed dropdown menu に当たる
- MDN の spinbutton role（https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Roles/spinbutton_role ）: 「When `aria-valuetext` is an important feature for a spinbutton, consider using `<select>` with `<option>` elements instead.」
- MDN の `<input type="time">`（https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input/time ）: 時刻用で、時は 00〜23。`step` は秒で指定し、既定は 60。`min` が `max` より大きいと、日付をまたぐ範囲になる
- 時間の長さを入力する `<input>` の提案（https://github.com/whatwg/html/issues/5488 ）は Open のままで、「needs implementer interest」のラベルが付いている
- USWDS の Time picker（https://designsystem.digital.gov/components/time-picker/ ）: 「Use the time picker to schedule a time from common, consistent increments, such as planning a meeting time in 30-minute blocks.」combobox の一覧に刻みごとの時刻を並べる。`data-max-time` の既定は 23:59、`data-step` の既定は 30 分

### iOS（参考）

- Apple Human Interface Guidelines の Pickers（https://developer.apple.com/design/human-interface-guidelines/pickers ）: 「Countdown timer — Displays hours and minutes, up to a maximum of 23 hours and 59 minutes.」分の間隔は 60 を割り切れる値に広げられる
- `UIDatePicker.minuteInterval`（https://developer.apple.com/documentation/uikit/uidatepicker/minuteinterval ）: 間隔は 60 を割り切れる値で、最大 30
- SwiftUI の `WheelPickerStyle`（https://developer.apple.com/documentation/swiftui/wheelpickerstyle ）: 任意の選択肢をホイールで見せる

### 各デザインシステムの time picker

調べた範囲では、24 時間を超える時間の長さを入力できる部品はなかった。刻みの設定は、MUI X の `timeSteps`、Ant Design の `hourStep` と `minuteStep`、Ionic の `minuteValues` と `hourValues`、Fluent UI の `increment` がある。MUI X は、選択肢が少ないときに 1 列のリストに切り替える（`thresholdToRenderTimeInASingleColumn`）。Ionic のホイールは各列を `slider` の role で公開している。

## 事例

実機では確かめていない。AOSP のソースは、Google や Pixel が配布しているアプリと同じとは限らない。

| アプリと機能 | 入力の UI | 刻み、最大値、24 時間超 | 出典 | 確かさ |
|---|---|---|---|---|
| Android のサイレント モードの時間（`EnableZenModeDialog`、`ZenDurationDialog`） | ラジオボタンの「For N」の横に − と + | 15、30、45 分、1〜12 時間。最小 15 分、最大 12 時間、既定 1 時間。24 時間超なし | https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SettingsLib/src/com/android/settingslib/notification/modes/ と `ZenModeConfig.java` | AOSP のソース |
| Google の時計のタイマー（AOSP の DeskClock） | 0〜9 のキーパッド | 刻みは 1 秒、最大 99 時間 99 分 99 秒。24 時間超あり | https://android.googlesource.com/platform/packages/apps/DeskClock/+/refs/heads/main/src/com/android/deskclock/timer/TimerSetupView.kt | AOSP のソース。配布中のアプリが同じかは未確認 |
| Google の時計のアラーム（AOSP の DeskClock） | `android.widget.TimePicker` | 時刻 | DeskClock の `alarms/TimePickerDialogFragment.kt` | AOSP のソース |
| Google の時計の設定（スヌーズの長さ、自動で止めるまでの時間） | `ListPreference`（固定のリスト） | 自動で止めるまでの時間は 1、5、10、15、20、25 分となし | DeskClock の `res/xml/settings.xml` | AOSP のソース |
| Google の時計の Bedtime | 就寝と起床の時刻を設定し、睡眠時間は結果として表示 | 時間の長さは直接入れない | https://support.google.com/clock/answer/9887159 | 公式のヘルプ |
| 設定アプリの画面消灯 | ラジオボタンの一覧 | 15 秒〜30 分の 7 項目 | AOSP の Settings の `res/values/arrays.xml` | AOSP のソース。メーカーが変えている可能性あり |
| AOSP のカレンダーの通知 | `Spinner` | 0 分〜1 週間の 16 項目。刻みは不均一。24 時間超あり | AOSP の Calendar の `res/values/arrays.xml`、`res/layout/edit_reminder_item.xml` | AOSP のソース |
| Google カレンダーの通知と予定の長さ | 通知は「Custom」で分、時間、日、週。予定の長さは既定値のリスト | 記事によって食い違う | https://support.google.com/calendar/answer/37242 と記事 | 部品は未確認 |
| Digital Wellbeing のアプリタイマー | ヘルプは「choose how long you can use it」とだけ書く | 記事では 0 分〜23 時間 | https://support.google.com/android/answer/9346420 、https://www.technipages.com/set-timers-android-apps/ | 部品と刻みは未確認 |
| Digital Wellbeing の Focus mode の「Take a break」 | 5、15、30 分のプリセット | 2019 年の記事 | https://www.androidcentral.com/digital-wellbeings-focus-mode-now-widely-rolling-out-all-users | 記事。今の選択肢は未確認 |
| Google Health アプリの睡眠目標 | 「Use the + or - buttons to add or remove time.」 | 既定 7 時間。刻みと上限は記載なし | https://support.google.com/googlehealth/answer/14236407 | 公式のヘルプ |
| Samsung の時計のタイマー | 「Use the keypad to select the hours, minutes, and seconds」 | 最大 99:59:59。24 時間超あり | https://www.samsung.com/us/support/answer/ANS00089064 | 公式のヘルプ。ホイールかキーパッドかは未確認 |
| Samsung の Digital Wellbeing のアプリタイマー | 日、時間、分を選ぶ。画像の説明は「a time picker set to twelve forty five」 | 記載なし | https://www.samsung.com/us/support/answer/ANS00085547 | 公式のヘルプ。部品は未確認 |
| Samsung Health の睡眠目標 | 円形の時計で就寝と起床の時刻を指定 | 記載なし | https://www.samsung.com/us/support/answer/ANS00090402/ | 公式のヘルプ |
| PayByPhone | 分、時間、日で時間の長さを入力。延長は + と − | 上限は駐車場の運営者が設定 | https://support.paybyphone.com/hc/en-001/articles/12154907909393 | 検索結果の抜粋のみ（ページは 403） |
| ParkMobile | 決まった枠の選択か、時間と分の選択 | 刻みと上限はゾーンごと | https://support.parkmobile.io/hc/articles/36854745256731 | 検索結果の抜粋のみ（ページは 403） |
| EasyPark | 「time wheel」を回して終了時刻を指定 | 記載なし | https://easypark.com/en-nl/help/parking/management-en-settings-how-do-i-set-an-end-time--17101770070556 | 検索結果の抜粋のみ（ページは 404） |

調べた AOSP のコードの中では、時間の長さの入力に `android.widget.NumberPicker` を使っている例は見つからなかった。サイレント モードは + と −、DeskClock はキーパッドと固定のリスト、カレンダーは `Spinner` だった。

## 参考文献

ガイドラインと事例の節で示した URL のほかに、次のソースを読んだ。

- AndroidX（androidx-main、コミット 07f09ade、2026-11-12）。https://cs.android.com/androidx/platform/frameworks/support/ の次のファイル
  - `compose/material/material/src/commonMain/kotlin/androidx/compose/material/Slider.kt`（`sliderSemantics`）
  - `compose/material/material/src/commonMain/kotlin/androidx/compose/material/ExposedDropdownMenu.kt`（`Modifier.expandable`）
  - `compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ScrollField.kt`
  - `compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/TimePicker.kt`（`TimeScroll`、`TimePickerState` の範囲）
  - `compose/material3/material3/samples/src/main/java/androidx/compose/material3/samples/ScrollFieldSamples.kt`
  - `compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt`（`progressBarRangeInfo` を `SeekBar` として公開する処理、スクロールのアクション 1 回の移動量、パーセントの既定の読み上げ）
  - `compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsProperties.kt`（`Role` の一覧）
  - `compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsNode.kt`（`clearAndSetSemantics` の子がアクセシビリティのツリーに出ないこと）
  - `test/uiautomator/uiautomator/src/main/java/androidx/test/uiautomator/UiObject2.java`
  - `wear/compose/compose-material3/src/main/java/androidx/wear/compose/material3/Picker.kt`
- AOSP の `NumberPicker`: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/NumberPicker.java
- Appium の UiAutomator2 server（https://github.com/appium/appium-uiautomator2-server ）の `SendKeysToElement.java`、`AxNodeInfoHelper.java`
- リリースノート: https://developer.android.com/jetpack/androidx/releases/compose-material 、https://developer.android.com/jetpack/androidx/releases/compose-material3 、https://developer.android.com/jetpack/androidx/releases/test-uiautomator
- ライブラリ: https://github.com/kez-lab/Compose-Pickers 、https://github.com/Seo-4d696b75/compose-m3-picker 。ほかに https://github.com/commandiron/WheelPickerCompose （最後のコミットは 2023-06）と https://github.com/zj565061763/compose-wheel-picker （最後のコミットは 2024-11）も見たが、どちらも semantics の記述はなかった

## 確認できていないこと

- 推奨しなかったパターン（スライダー、キーパッド、テキストフィールド）を TalkBack が何と読み上げるか。ソースから読み取っただけで、確かめていない
- 実機での TalkBack と UIAutomator2 の動き。推奨した 4 つのパターンは、エミュレータでだけ確かめた（[docs/sample-app.md](sample-app.md) を参照）
- Samsung の時計のタイマーが、ホイールとキーパッドのどちらか。ヘルプには「keypad」とある
- Digital Wellbeing のアプリタイマーの部品、刻み、上限
- Google カレンダーの通知の「Custom」の部品と上限、予定の長さの既定値の選択肢
- 配布中の Google の時計のアラームが `MaterialTimePicker` を使っているか、タイマーが AOSP と同じか
- Google Health アプリ、Samsung Health、駐車アプリの刻みと上限。駐車アプリのヘルプは本文を読めず、検索結果の抜粋だけで書いた
- SAP Fiori の、決まった時間の長さには time picker でなく select を使う、という記述。sap.com を読めず、検索結果の抜粋でしか見ていない
- Wear の `Picker` を、スマートフォンで使わないよう明示した公式の記述。Material と混ぜない注意しか見つからなかった
- `ScrollField` と `TimeScroll` が最初に入った material3 の alpha のバージョン
- カーシェアやレンタルのアプリ、バッテリー セーバーのスケジュールは調べていない
