# Wrong Button（v0.2 垂直切片）

一個看起來像普通 Android App 的畫面裡，住著一顆有感情、會說謊、而且記得玩家做過什麼的按鈕。
這個切片包含 10 關，從「不信任它」走到「相信它」。

## 建置與測試

```bash
./gradlew assembleDebug test
```

- 需要 JDK 17 以上與 Android SDK（platform 37、build-tools 37.0.0）。在 `local.properties` 設定 `sdk.dir`。
- 測試全部在 JVM 上執行（Compose UI 測試用 Robolectric），不需要模擬器。
- `assembleDebug` 會順便執行 `verifyDebugNoPermissions`：合併後的 manifest 只要出現任何權限宣告或請求，建置就會失敗。

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
├─ core/audio/     SoundPlayer／MusicPlayer 介面、程式合成的佔位音、震動
├─ core/settings/  設定與「減少動態效果」
├─ core/ui/        遊戲內覆蓋層對話框、滑掉手勢、material3 實驗 API 的包裝
├─ ui/             主題、主選單、選關、紀錄、設定
└─ levels/         每關一個檔案：上半部是純規則（可單元測試），下半部是 Composable
```

### 新增一關

1. 在 `levels/` 新增 `LevelNNXxx.kt`，實作 `Level`。
2. 在 `res/values/` 加上該關的字串、三階提示與台詞表（`lNN_enter`、`lNN_first_mistake` …每種事件至少 2 句）。
3. 把它加進 `Levels.all`。

引擎不需要修改。`ResourcesTest` 會自動檢查新關卡的台詞與提示是否齊全。

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
- `LevelRulesTest`：第 1、4、5、6、7、8、9、10 關的規則判定。
- `Level01FlowTest`：從主選單玩第 1 關到結算。
- `FullPlaythroughTest`：只用單指、不限時，從第 1 關連續玩到第 10 關。需要讀臉的關卡只用 TalkBack 讀得到的資訊來推理。
- `RotationTest`：旋轉（Activity 重建）後關卡狀態與統計不遺失。
- `ResourcesTest`：台詞與提示是否齊全。

設計上的假設與偏離規格之處，見 [`docs/ASSUMPTIONS.md`](docs/ASSUMPTIONS.md)。
