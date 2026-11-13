# android-duration-input-lab

時間の長さ（N時間M分）を入力する UI のパターンを、手で触って比べるためのサンプルのアプリです。パターンを選んだ経緯は [docs/research.md](research.md) にあります。

UI は M2（`androidx.compose.material`）で作りました。material3 を使うのは、ホイールの 1 つ（`ScrollField`）だけです。

## パターンの一覧

最初の画面のボタンから、パターンごとの画面を開きます。

| 画面 | ファイル | 使った部品 |
|---|---|---|
| 1. 時と分の 2 つのドロップダウン | `DropdownsScreen.kt`、`OptionDropdown.kt` | M2 の `ExposedDropdownMenuBox`（read-only） |
| 2. 値をすべて並べた 1 つのリスト | `SingleListScreen.kt`、`OptionDropdown.kt` | 1 と同じ |
| 3. ホイール（material3 の ScrollField） | `ScrollFieldWheelScreen.kt` | material3 の `ScrollField` |
| 3. ホイール（M2 で自作） | `CustomWheelScreen.kt` | `LazyColumn` と `snapFlingBehavior`。`ScrollField` と同じ semantics を付けた |
| 4. ステッパー | `StepperScreen.kt` | M2 の `OutlinedButton` と `Text` |

どの画面にも、値の設定を切り替える欄を置きました。

- 時の範囲: 0〜25、0〜48
- 分の刻み: 5 分、10 分、15 分
- 24 時以降を「翌」で表示: オンにすると、25:30 を「翌1:30」と表示する

選べる値は 0:00 からで、最大は、時を範囲の上限にし、分を最後の刻みにした値です。たとえば、0〜25 と 10 分なら、最大は 25:50 です。選んだ値は画面に文字で表示し、testTag の `selected_value` で UIAutomator2 から読めます。testTag は `testTagsAsResourceId = true` で resource-id として出しています。

設定を切り替えると、時か分、または両方が 0 に戻ります。どちらが戻るかは画面によって違います。

### 自作のホイールの手触りの欄

「3. ホイール（M2 で自作）」の画面の下に、手触りを変える欄があります。画面を上にスクロールすると見えます。

| 欄 | 変えるもの | 既定 |
|---|---|---|
| exponentialDecay を使う | オフなら `rememberSplineBasedDecay()`、オンなら `exponentialDecay(frictionMultiplier)` を、`snapFlingBehavior` の `decayAnimationSpec` に渡す | オフ |
| 摩擦（frictionMultiplier） | `exponentialDecay` の `frictionMultiplier`。0.1〜3.0 を 0.1 刻みで変える。大きいほど早く止まる。オンのときだけ変えられる | 1.0 |
| 項目の高さ（dp） | 項目の高さ。32〜80 を 4 刻みで変える | 48 |
| 見えている項目の数 | 3、5、7、9 | 5 |

既定の組み合わせは、`rememberSnapFlingBehavior(state)` の中身と同じです。`snapAnimationSpec` は `spring(stiffness = Spring.StiffnessMediumLow)` のままです。`splineBasedDecay` には摩擦を変える引数がないので、摩擦を変えるときは `exponentialDecay` に切り替えます。

今の値は、欄の上に次の形で表示されます。気に入った値を、そのままコードに書き写せます。

```
decayAnimationSpec = exponentialDecay(frictionMultiplier = 0.8f)
itemHeight = 48.dp
visibleItemCount = 5
```

時の列を同じ速さで 1 回フリックしたとき（`adb shell input swipe 167 1050 167 850 40`）に動いた項目の数を、0 時から測りました。既定は 12 時まで、`exponentialDecay` の摩擦 0.8 は 11 時、1.7 は 6 時まで動きました。0.4 は 2 回とも端の 25 時まで動きました。ただし最初の 1 回は、0.4 で 1 時までしか動かず、この結果は再現しませんでした。

## バージョン

| もの | バージョン |
|---|---|
| Android Gradle Plugin | 9.3.3 |
| Gradle | 9.7.1 |
| Kotlin（Compose Compiler Gradle plugin） | 2.4.21 |
| Compose BOM | 2026.09.00（`androidx.compose.material` 1.12.1） |
| `androidx.compose.material3` | 1.5.0-rc01。`ScrollField` が入ったのは 1.5.0 で、2026-11-12 の時点では rc01 が最新 |
| `androidx.test.uiautomator` | 2.4.1 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 30 |

minSdk を 30 にしたのは、テストで `AccessibilityNodeInfo.getStateDescription()` を使えるようにするためです。

## ビルドと実行

`local.properties` に `sdk.dir` を書いてから、エミュレータか実機をつないで実行します。

```sh
./gradlew :app:installDebug
adb shell am start -n com.github.appachan.durationinputlab/.MainActivity
```

UIAutomator2 のテストは次のとおりです。テストが終わると、アプリはアンインストールされます。

```sh
./gradlew :app:connectedDebugAndroidTest
adb logcat -d -s DurationInputLab:I
```

テストは、値を入れるのにかかった時間と操作の回数を、logcat の `DurationInputLab` タグに出します。

## 確かめた結果

2026-11-12 に、エミュレータの AVD `Pixel_9_API_35`（Android 15、`google_apis_playstore`、1080×2424）で確かめました。自作のホイールを直したあと、同じ日に UIAutomator2 のテストと、自作のホイールの TalkBack の操作を確かめ直しました。実機では確かめていません。

### UIAutomator2

時の範囲 0〜25 と 10 分で、25:30、0:00、最大の 25:50 を、この順に入れました。0〜48 では最大の 48:50 だけを入れました。すべてのテストが通っています。

時間は、操作を始めてから `selected_value` が目標の値になるまでです。1 回の実行での値なので、目安として見てください。

| パターン | 使った API | 25:30 | 0:00 | 25:50 | 48:50 |
|---|---|---|---|---|---|
| 1. 2 つのドロップダウン | `UiObject2.click()`、画面外の項目は `UiDevice.swipe` | 4.9 秒（スワイプ 2 回） | 6.2 秒（2 回） | 5.6 秒（2 回） | 7.2 秒（5 回） |
| 2. 1 つのリスト | 1 と同じ | 12.1 秒（19 回） | 12.6 秒（19 回） | 12.2 秒（19 回） | 21.6 秒（36 回） |
| 3. ScrollField | `ACTION_SET_PROGRESS` | 0.20 秒 | 0.21 秒 | 0.17 秒 | 0.14 秒 |
| 3. ScrollField | `UiDevice.swipe` | 2.0 秒（4 回） | 2.0 秒（4 回） | 1.0 秒（2 回） | 0.96 秒（2 回） |
| 3. M2 で自作 | `ACTION_SET_PROGRESS` | 2.1 秒 | 2.1 秒 | 0.48 秒 | 2.1 秒 |
| 3. M2 で自作 | `UiDevice.swipe` | 3.4 秒（8 回） | 3.3 秒（8 回） | 4.1 秒（9 回） | 5.5 秒（14 回） |
| 4. ステッパー | `UiObject2.click()` | 1.6 秒（タップ 28 回） | 2.2 秒（28 回） | 1.6 秒（30 回） | 4.3 秒（53 回） |

`ACTION_SET_PROGRESS` は、`UiObject2.getAccessibilityNodeInfo()`（2.4.0 から public）で取った `AccessibilityNodeInfo` の `performAction` で実行しました。スクロールもスワイプも要りません。

自作のホイールの `ACTION_SET_PROGRESS` は、同じテストを実行するたびに 0.2〜2.1 秒の間でばらつきました。手触りの欄を足したあとに 4 回実行すると、12 回の入力のうち 11 回が約 2 秒でした。欄を足す前のコードで 1 回実行すると 2.0、0.86、0.54 秒で、欄のうち画面全体の `verticalScroll` だけを外すと 1.4、2.0、0.5 秒でした。原因は未確認です。

#### スワイプでの入力が遅かった原因と直し方

前の結果では、自作のホイールの 25:30 にスワイプ 28 回、約 38 秒かかっていました。テストに時間のログを入れて測ると、1 回のスワイプの約 1.3 秒のほとんどは `UiDevice.swipe` の呼び出しで、`waitForIdle()` は数 ms でした。`UiDevice.swipe` は、指を 1 ステップ動かすたびに `waitForNextFrame()` で次のフレームを待ちます（`InteractionController.java`）。テストは 1 区間 20 ステップを 2 区間で指定していたので、40 フレームを待っていました。さらに、1 回のスワイプで 1 項目しか動かしていませんでした。

テストを次のように直しました。

- 1 区間を 6 ステップにした。指を止めてから離す区間も、ステップの数で長さが決まる
- 1 回のスワイプで、ホイールの高さの 8 割に収まるだけの項目を動かす。自作のホイールでは 4 項目になる

`ScrollField` は、1 回のスワイプで 1 項目のままです。`ScrollField` は `VerticalPager` に `flingBehavior` を渡さないので、`PagerDefaults.flingBehavior` の `PagerSnapDistance.atMost(1)` が効き、1 回の操作で 1 ページまでしか動きません。3 項目分動かすと、何度もスワイプし直すことになりました（0:00 で 10 回）。

`ScrollField` のスワイプが少ないのは、項目が循環するので近い向きに回せるためです。0 から 25 へは 1 つ戻るだけで済みます。

確かめる途中で、次のことが分かりました。

- ドロップダウンのメニューは別のウィンドウ（Popup）に出ます。ルートの `testTagsAsResourceId = true` はそこに届かず、項目に resource-id が出ませんでした。メニューの modifier にも `testTagsAsResourceId = true` を付けると出ました
- メニューの画面外の項目は、アクセシビリティのツリーに出ません。スクロールして探す必要があります
- `UiObject2.scrollUntil` では、メニューの項目を探せませんでした。スワイプのたびに `TYPE_VIEW_SCROLLED` を待ち、1 度も届かないまま 5 回で止まります。logcat には「Expected event not received within: 1000 ms among: []」「scrollUntil reached max retries for null events.」と出ました。テストでは `UiDevice.swipe` で自分でスワイプを繰り返しました
- スワイプした直後に項目をタップすると、値が選ばれないことがありました。タップの前に、メニューの `UiObject2` に対して `waitForStable()` で待つと選ばれました
- メニューは、前回開いたときのスクロールの位置で開きます。上にある項目を探すときは、逆向きにスワイプする必要があります。見えている項目を `findObjects` で取って向きを決めると、スワイプを速くしたあとに、名前を読む前に項目がツリーから消えて `StaleObjectException` になりました。今は、前回選んだ値と比べて向きを決めています
- Compose は、`Role.RadioButton` を付けた `selectable` の選択の状態を、`checked` として公開します。`By.selected(true)` では見つからず、`By.checked(true)` で見つかりました
- `ScrollField` の `setProgress` に渡す値は、項目の番号ではなくページの番号です。テストでは、今のページ（`rangeInfo.current`）から差を足して求めました
- `rememberScrollFieldState` は、項目数が変わってもページを引き継ぎます。時の範囲を 0〜25 から 0〜48 に切り替えると、選んでいた 0 時が 18 時に変わりました。アプリでは、設定が変わったら `key` で作り直すようにしました

### TalkBack

TalkBack 15.0.0.639625893 で確かめました。読み上げの文は、TalkBack の開発者向けの設定で「診断モード」をオンにし、logcat の `talkback` タグの `SpeechControllerImpl: Speaking fragment text="..."` の行から取りました。画面の字幕（開発者向けの設定の「音声出力の表示」）も併せて見ています。

TalkBack の操作は、エミュレータのコンソールから `adb emu event mouse` でタッチを送りました。次の 2 つは使えませんでした。

- `adb shell input` のタッチは、TalkBack のタッチガイドを通らず、アプリに直接届きました
- `/dev/input/event1` に `sendevent` で書き込もうとしたら、権限がなく失敗しました

また、TalkBack を有効にしたまま `uiautomator dump` を実行すると、そのあとに「TalkBack on」と読み上げられ、TalkBack が再起動したように見えました。そのため、TalkBack で確かめている間は `uiautomator dump` を使わず、値は TalkBack の読み上げで確かめました。

読み上げの文は、logcat に出たとおりに書きます。「|」の左は TalkBack が読んだきっかけです。

#### 1. 2 つのドロップダウン

手当てをする前は、M2 の `ExposedDropdownMenuBox` を TalkBack から開けませんでした。

- 時の欄に触れると「0. 時」と読まれ、role は読まれませんでした
- ダブルタップしても、メニューは開きませんでした
- UI の階層では、メニューを開く `onClick` を持つ要素は「プルダウン メニュー」という content-desc を持ち、`focusable="false"` でした。TalkBack は、この要素を飛ばして中の `EditText`（read-only の `TextField`）にフォーカスしていました
- この要素には `Role.DropdownList` を付けていましたが、クラス名は `android.view.View` のままでした。理由は未確認です

M3 の `menuAnchor` と同じく、`TextField` に `Role.DropdownList` と、メニューを開く `onClick` を付けました（`OptionDropdown.kt`）。手当てをしたあとの動きは次のとおりです。

| 操作 | 読み上げ |
|---|---|
| 時の欄に触れる | 「0. プルダウンリスト. 時」 |
| ダブルタップ | メニューが開く。ただし読み上げはなく、フォーカスは時の欄に残った（5 回のうち 4 回。残りの 1 回は、先頭の項目「0」にフォーカスが移った） |
| メニューの項目に触れる | 「3. ウィンドウ（ポップアップウィンドウ）」 |
| 右へスワイプ | 「1」「2」…「25」。画面外の項目へは、TalkBack がスクロールして移った。何件中の何件目かは読まれない |
| 項目をダブルタップ | メニューが閉じ、フォーカスが欄に戻って「3. プルダウンリスト. 時」 |
| 選んだ値に触れる | 「3:00」 |

フォーカスが欄に残ったときは、右へスワイプすると、分の欄、選んだ値の順に進んでから、メニューの項目（「00. ウィンドウ（ポップアップウィンドウ）」）に届きました。

#### 2. 1 つのリスト

1 と同じ部品なので、読み上げ方も同じでした。

| 操作 | 読み上げ |
|---|---|
| 欄に触れる | 「0:00. プルダウンリスト. 時間」 |
| ダブルタップ | メニューが開く。読み上げはない |
| 項目に触れる | 「0:10. ウィンドウ（ポップアップウィンドウ）」 |
| 右へスワイプ | 「0:20」「有効にするにはダブルタップします」 |
| ダブルタップ | 「0:30. プルダウンリスト. 時間」 |

#### 3. ホイール（ScrollField）

列全体が 1 つの要素として読まれ、上下のスワイプで値が変わりました。

| 操作 | 読み上げ |
|---|---|
| 時の列に触れる | 「0時間. 時. スライダー」 |
| 上へスワイプ | 「1時間」「「上にスワイプ」または「下にスワイプ」を調整」 |
| 下へスワイプ | 1 つ減る。0時間から下へスワイプすると「25時間」で、循環する |
| 右へスワイプ | 「0分. 分. スライダー」 |
| 「翌」の表示をオンにして、時の列で下へスワイプ | 「翌1時」。選んだ値は「翌1:00」 |

#### 3. ホイール（M2 で自作）

`ScrollField` と同じ読み上げになりました。違うのは、循環しない点です。

| 操作 | 読み上げ |
|---|---|
| 時の列に触れる | 「0時間. 時. スライダー」 |
| 上へスワイプ | 「1時間」「「上にスワイプ」または「下にスワイプ」を調整」、もう一度で「2時間」 |
| 下へスワイプ | 「1時間」「0時間」。0時間から下へスワイプしても「0時間」のまま |
| 右へスワイプ | 「0分. 分. スライダー」 |

前の結果では、1 回目の上へのスワイプで値が読まれませんでした。直したあとは、画面を開き直して 1 回目の上へのスワイプを 10 回試し、10 回とも「1時間」と読まれました。

##### 1 回目のスワイプで値が読まれなかった原因と直し方

直す前のコードで同じ試行を繰り返すと、値が読まれなかったのは 5 回のうち 1 回で、毎回は起きませんでした。読まれなかったときの TalkBack のログでは、次の順に処理されていました。

1. 時の列に触れたときに予約された、操作のヒント（「上にスワイプ」または「下にスワイプ」を調整）が読まれ始める
2. 値の変化のイベント（`CONTENT_CHANGE_TYPE_STATE_DESCRIPTION`）が届き、TalkBack は「1時間」を出力として作る
3. 同じ要素から `CONTENT_CHANGE_TYPE_UNDEFINED`（ログでは `contentChangeType=0`）のイベントが届き、TalkBack は空の出力を `ttsClearQueueGroup=2` 付きで処理する
4. 「1時間」は読まれない

読まれたときは、2 の「1時間」がヒントより先に読まれていました。読まれなかったときは、ヒントを読んでいる間にキューに入った「1時間」が、3 で消されたと考えます。ログから読み取ったもので、TalkBack のソースでは確かめていません。

3 のイベントは Compose が送るものです。`stateDescription` か `progressBarRangeInfo` が変わると、`CONTENT_CHANGE_TYPE_STATE_DESCRIPTION` のすぐあとに `CONTENT_CHANGE_TYPE_UNDEFINED` を送ります（`AndroidComposeViewAccessibilityDelegateCompat.android.kt`。コメントには、ViewRootImpl の不具合（b/192295060）を避ける一時的な処理とある）。これは `ScrollField` でも同じです。

`ScrollField` と違ったのは、値が変わるまでの時間です。TalkBack が `ACTION_SCROLL_FORWARD` を実行してから値の変化のイベントが届くまで、`ScrollField` は 9〜12ms、自作のホイールは 144〜167ms でした（それぞれ 3 回）。`ScrollField` は `setProgress` を受けた時点で行き先のページ（`targetPage`）を値にします。自作のホイールは、スクロールが半分を過ぎて中央の項目が変わった時点で値を変えていたので、ヒントの読み上げと重なる時間の幅が広くなっていました。

自作のホイールも、`setProgress` で動かしている間は行き先の項目を値にするようにしました。直したあとは、値の変化のイベントが届くまで 20〜51ms でした（10 回）。ヒントと値の変化が重なれば、`ScrollField` と同じく値が読まれない可能性は残ります。`ScrollField` では 5 回とも読まれ、読まれないことは観察していません。

#### 4. ステッパー

| 操作 | 読み上げ |
|---|---|
| 時の − に触れる | 「1 時間減らす. −. ボタン. 無効」 |
| 右へスワイプ | 「0時間」「1 時間増やす. +. ボタン」 |
| + をダブルタップ | 「1時間」、もう一度で「2時間」 |
| 分の + をダブルタップ | 「10分」 |
| 選んだ値に触れる | 「2:10」 |

ボタンは、指定した `contentDescription` と、中の文字（「−」「+」）の両方が読まれました。押したあとの値は、値の `Text` に付けた `liveRegion` で読まれました。

## 確かめられなかったこと

- 実機での動き。エミュレータでしか確かめていない
- TalkBack の音声そのもの。logcat の文と字幕で確かめ、音は聞いていない
- TalkBack のキーボード ショートカットでの操作。コンソールから物理キーボードのイベントとして Alt+Enter を送ったが、効いたかを判断できなかった
- スイッチアクセス
- TalkBack での「翌」の読み上げは、`ScrollField` でしか確かめていない
- 時の範囲 0〜48 での TalkBack の操作
- `Role.DropdownList` を付けた要素のクラス名が `android.view.View` のままだった理由
- `UiObject2.scrollUntil` がメニューで `TYPE_VIEW_SCROLLED` を受け取れなかった理由。Popup のウィンドウだからか、Compose の `verticalScroll` だからかは切り分けていない
- スワイプの直後のタップで値が選ばれなかった理由
- ドロップダウンを開いたあとに、TalkBack のフォーカスがメニューに移るときと移らないときがある理由
- TalkBack が、キューに入った値の読み上げを `CONTENT_CHANGE_TYPE_UNDEFINED` のイベントで消す仕組み。ログから読み取っただけで、TalkBack のソースでは確かめていない
- 自作のホイールで、TalkBack のヒントと値の変化が重なったときに値が読まれないこと。直したあとの 10 回では起きなかった
- 自作のホイールの `ACTION_SET_PROGRESS` での入力にかかる時間がばらつく理由
- 自作のホイールの手触りが iOS のホイールに近づくかどうか。手触りの欄で値を変えられるようにしたが、どの値が近いかは確かめていない
