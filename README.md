# Wrong Button（v0.5）

一個看起來像普通 Android App 的畫面裡，住著一顆有感情、會說謊、而且記得玩家做過什麼的按鈕。
全部 300 關、20 章都可以玩。

- **第一季**（1–120 關，8 章）：信任問題、讀臉、手勢、它記得妳、填好再送出、改版、後台、相信我。
- **第二季**（121–300 關，12 章，每章獨立）：外面、同類、暗黑模式、聽不懂、重播、換妳做、看不見、播放中、設定、檔案、試算表、迷路。
- 前 100 關免費，其餘 200 關需要 Pro（目前是模擬購買）。
設計與每一關的細節見 [`docs/DESIGN.md`](docs/DESIGN.md) 和 [`docs/levels/`](docs/levels/)。

## 建置與測試

```bash
./gradlew assembleDebug test
```

- 需要 JDK 17 以上與 Android SDK（platform 37、build-tools 37.0.0）。在 `local.properties` 設定 `sdk.dir`。
- 測試全部在 JVM 上執行（Compose UI 測試用 Robolectric），不需要模擬器。
- `assembleDebug` 會順便執行 `verifyDebugPermissions`：合併後的 manifest 只能出現白名單裡的權限（`app/build.gradle.kts` 的 `allowedPermissions`），其他一律讓建置失敗。目前白名單只有 androidx.core 自己定義的 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，不向使用者請求任何權限。
- CI：`.github/workflows/android.yml` 在每次 push 與 PR 時執行同一組指令。

| 項目 | 版本 |
|---|---|
| Android Gradle Plugin | 9.5.1（內建 Kotlin 支援） |
| Gradle | 9.9.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00（material3 1.4.0） |
| Navigation 3 | 1.2.1 |
| Lifecycle | 2.11.0 |
| DataStore | 1.3.0 |
| kotlinx.serialization | 1.13.0 |
| minSdk / compileSdk / targetSdk | 26 / 37 / 37 |

## 架構

```
app/src/main/kotlin/tw/dynity/wrongbutton/
├─ app/            MainActivity、手寫依賴容器、Navigation 3 導覽
├─ core/engine/    Level／LevelScope 介面、LevelHost（外框、提示、暫停、結算、統計、存檔）
├─ core/character/ ButtonCharacter（Canvas 繪製）、FaceGeometry（說謊特徵，純函式）
├─ core/memory/    PlayerMemory、MemoryOps（純函式）、DataStore 儲存
├─ core/dialogue/  台詞事件、[mood|tell] 標記、{變數} 模板、最近 5 句不重複的挑選器
├─ core/audio/     音效（SoundPool）、背景音樂（MediaPlayer，三首循環曲）、震動
├─ core/settings/  設定、「減少動態效果」、「你／妳」稱呼（PronounResources）
├─ core/monetization/ Pro 與廣告（目前全是模擬）：HintGate、MockAdService、MonetizationRepository
├─ core/ui/        遊戲內覆蓋層對話框、滑掉手勢、material3 實驗 API 的包裝
├─ ui/             主題、主選單、選關、紀錄、設定、Pro 頁與模擬廣告
└─ levels/         每關一個檔案：上半部是純規則（可單元測試），下半部是 Composable
```

### 第二季：新增一章

1. 寫 `tools/s2/chNN.py`：章名、整章共用的台詞池，以及每關的標題、任務、三階提示、進場／第一次錯／通關台詞、關卡專用字串和設計說明。
2. 執行 `python3 tools/s2/gen.py NN`，產生 `strings_chNN.xml`、`dialogue_chNN.xml`、`levels/s2/ChNNMeta.kt` 和 `docs/levels/chNN.md`。
3. 寫 `levels/s2/ChNN*.kt`：每關一個 `object LevelXXX : S2Level(ChNNMeta.lXXX)`，只需要實作 `Content`。
4. 把它加進 `Levels.chapters`（`standalone = true`），並寫一個 `ChapterNNPlaythroughTest`。

開發時可以用 `WB_SCREENSHOTS=121,122 ./gradlew testDebugUnitTest --tests '*ScreenshotTool*'` 把指定關卡的畫面存到 `app/build/screenshots/`。

### 新增一關

1. 在 `levels/` 新增 `LevelNNXxx.kt`，實作 `Level`，取一個沒用過的 `id`。`id` 是永久的存檔鍵，發布後不能改。
2. 在 `res/values/strings_chNN.xml` 加上該關的字串與三階提示，在 `dialogue_chNN.xml` 加上台詞表（`lNN_enter`、`lNN_first_mistake` …每種事件至少 2 句）。每章一個檔案，Android 建置時會自動合併。
3. 把它放進 `Levels.chapters` 的某一章。玩家看到的「第 N 關」是它在清單裡的位置；解鎖規則是前一關通關才開。

引擎不需要修改。`ResourcesTest` 會自動檢查新關卡的台詞與提示是否齊全。

### 音效與音樂

`res/raw/` 裡的 7 個音效與 3 首音樂，全部由 `python3 tools/audio/gen_audio.py` 合成（numpy＋ffmpeg 的 libvorbis），不使用任何外部取樣。要調整音色或旋律就改這支腳本再重新產生；要換成外部素材，替換同名的 .ogg 即可。

### 存檔

- 記憶存在 `files/datastore/player_memory.json`（DataStore＋kotlinx.serialization）。
- 每次正常開啟時，會把讀到的內容另存成 `player_memory.json.bak`。主檔讀不進來時**不會直接清空**：先把壞掉的檔案另存成 `.broken`，再改讀 `.bak`，都不行才用空白存檔。
- `PlayerMemory.SCHEMA` 是存檔格式版本。改欄位名稱或型別時，要把它加一，並在 `MemoryMigrations` 加一步遷移（`SaveSafetyTest` 有範例）。只新增有預設值的欄位不需要遷移。
- Pro 與看過廣告的提示另外存在 `monetization.json`，「讓 The Button 忘記我」不會清掉它們。

### Pro 與廣告（模擬）

- 前 `Levels.FREE_LEVELS`（100）關免費，之後需要 Pro。進度解鎖（`isUnlocked`）和 Pro（`requiresPro`）是分開的兩件事。
- 每一階提示第一次看要先看一則模擬廣告，看過就記下來；Pro 不用看。沒有廣告可播時一律直接給提示，不會卡住玩家。
- 關卡畫面裡絕對不放廣告；模擬廣告只在玩家自己按「看廣告顯示提示」後出現，並清楚標示「模擬廣告」。
- 接上真的 Google Play Billing 與 AdMob 時，只要換掉 `Store` 與 `AdService` 的實作，並把它們需要的權限加進白名單。

### 說謊特徵

- 說實話：瞳孔置中、嘴形左右對稱。
- 說謊：瞳孔看向一側（至少是眼睛半徑的 40%），一邊嘴角翹起（至少是臉高的 4.5%）。

這兩個量寫死在 `FaceGeometry`，不受情緒影響，也不受「減少動態效果」影響。`FaceGeometryTest` 會驗證這些下限。
TalkBack 會讀出外觀描述（「它的眼睛看向旁邊，一邊嘴角翹起」），但不會直接說「它在說謊」。

### 台詞標記

台詞放在 `res/values/dialogue.xml`，前綴標記用來決定表情：

```xml
<item>[smug|lie]今天我是顧問。我說的話絕對可靠。</item>
```

- 情緒：`normal`、`smug`、`guilty`、`angry`、`sad`、`serious`、`sleeping`。
- 真假：`honest`（預設）、`lie`（誇張版）、`subtle`（細微版）。

**規則：內容是假話的台詞一定要標 `lie` 或 `subtle`；沒有標記的台詞必須是真話。** 說謊特徵能否被學會，靠的就是這條規則全遊戲一致。

## 測試涵蓋

- `MemoryTest`：記憶讀寫、重開 App 後資料仍在、「以最新一次為準」、檔案損毀時的回復。
- `DialogueTest`：最近 5 句不重複、全域備用池、標記解析、變數帶入。
- `FaceGeometryTest`：說謊特徵在靜態畫面上的可辨識下限。
- `LevelRulesTest`、`Chapter1RulesTest`：各關的規則判定、關卡順序與解鎖。
- `Level01FlowTest`：從主選單玩第 1 關到結算。
- `FullPlaythroughTest`、`ChapterNPlaythroughTest`（第 2–20 章）：只用單指、不限時，依順序玩完每一章。需要讀臉的關卡只用 TalkBack 讀得到的資訊來推理。
- `SaveSafetyTest`、`MonetizationTest`、`UnlockRulesTest`、`PronounTest`：存檔備份與遷移、提示廣告與 Pro、解鎖規則、你／妳替換。
- `RotationTest`：旋轉（Activity 重建）後關卡狀態與統計不遺失。
- `ResourcesTest`：台詞與提示是否齊全。
- `AndroidRegexTest`：原始碼裡的正規表示式在 Android（ICU）上是否合法。

設計上的假設與偏離規格之處，見 [`docs/ASSUMPTIONS.md`](docs/ASSUMPTIONS.md)。
