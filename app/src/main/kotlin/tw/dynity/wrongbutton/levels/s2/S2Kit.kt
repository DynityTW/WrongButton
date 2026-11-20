package tw.dynity.wrongbutton.levels.s2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.system.CaptureVolumeKeys
import tw.dynity.wrongbutton.core.system.LocalSystemSignals
import tw.dynity.wrongbutton.core.system.VolumeKey
import tw.dynity.wrongbutton.levels.PressableButton

/** 第二季用到的圖示（Material Icons 的路徑，Apache 2.0）。 */
object S2Icons {
    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()

    val Phone = icon("Phone", "M17,1.01L7,1c-1.1,0 -2,0.9 -2,2v18c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3c0,-1.1 -0.9,-1.99 -2,-1.99zM17,19H7V5h10v14z")
    val Airplane = icon("Airplane", "M21,16v-2l-8,-5V3.5C13,2.67 12.33,2 11.5,2S10,2.67 10,3.5V9l-8,5v2l8,-2.5V19l-2,1.5V22l3.5,-1 3.5,1v-1.5L13,19v-5.5l8,2.5z")
    val Speaker = icon("Speaker", "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z")
    val Rotate = icon("Rotate", "M16.48,2.52c3.27,1.55 5.61,4.72 5.97,8.48h1.5C23.44,4.84 18.29,0 12,0l-0.66,0.03 3.81,3.81 1.33,-1.32zM10.23,1.75c-0.59,-0.59 -1.54,-0.59 -2.12,0L1.75,8.11c-0.59,0.59 -0.59,1.54 0,2.12l12.02,12.02c0.59,0.59 1.54,0.59 2.12,0l6.36,-6.36c0.59,-0.59 0.59,-1.54 0,-2.12L10.23,1.75zM14.83,21.19L2.81,9.17l6.36,-6.36 12.02,12.02 -6.36,6.36zM7.52,21.48C4.25,19.94 1.91,16.76 1.55,13H0.05C0.56,19.16 5.71,24 12,24l0.66,-0.03 -3.81,-3.81 -1.33,1.32z")
    val Widgets = icon("Widgets", "M13,13v8h8v-8h-8zM3,21h8v-8H3v8zM3,3v8h8V3H3zM16.66,1.69L11,7.34 16.66,13l5.66,-5.66 -5.66,-5.65z")
    val Door = icon("Door", "M19,19V5c0,-1.1 -0.9,-2 -2,-2H7c-1.1,0 -2,0.9 -2,2v14H3v2h18v-2H19zM15,13h-2v-2h2V13z")
    val Lamp = icon("Lamp", "M9,21c0,0.55 0.45,1 1,1h4c0.55,0 1,-0.45 1,-1v-1L9,20v1zM12,2C8.14,2 5,5.14 5,9c0,2.38 1.19,4.47 3,5.74L8,17c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1v-2.26c1.81,-1.27 3,-3.36 3,-5.74 0,-3.86 -3.14,-7 -7,-7z")
}

/** 一張「示意圖」卡片：大圖示加一行字。整張卡片對 TalkBack 是一段描述。 */
@Composable
fun Picture(icon: ImageVector, caption: String, description: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .widthIn(max = 360.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(56.dp))
            Text(caption, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

/** The Button 的「照片」：不是本人，點了算錯。 */
@Composable
fun ButtonPhoto(scope: LevelScope, caption: String, notHere: Int, tag: String, size: Dp = 96.dp) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .testTag(tag)
            .clickable(role = Role.Image) { scope.mistake(tag, Line.Res(notHere), Mood.Smug) },
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ButtonCharacter(Mood.Normal, tw.dynity.wrongbutton.core.character.Tell.Honest, size, describe = false)
            Text(caption, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** 監聽實體音量鍵。這個 Composable 在畫面上時，音量鍵不會改系統音量。 */
@Composable
fun VolumeKeys(onKey: (VolumeKey) -> Unit) {
    val signals = LocalSystemSignals.current
    val handler by rememberUpdatedState(onKey)
    CaptureVolumeKeys(signals)
    LaunchedEffect(signals) { signals.volumeKeys.collect { handler(it) } }
}

/**
 * 「條件達成之後才能按」的 The Button。
 * 條件還沒達成時按它算錯並說 [notReady]；條件一達成就說 [ready]（一次）。
 */
@Composable
fun GatedButton(
    scope: LevelScope,
    ready: Boolean,
    notReady: Int,
    readyLine: Int?,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier,
    onPress: () -> Unit = { scope.complete() },
) {
    LaunchedEffect(ready) {
        if (ready && readyLine != null) scope.say(Line.Res(readyLine))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        PressableButton(scope, size, onClick = {
            if (ready) {
                scope.cue(Cue.Tap)
                onPress()
            } else {
                scope.mistake("gated", Line.Res(notReady), Mood.Guilty)
            }
        })
    }
}

/** 一行狀態文字（例如「燈：開」）。 */
@Composable
fun StatusLine(text: String, tag: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(tag),
    )
}

@Composable
fun res(id: Int, vararg args: Any): String = stringResource(id, *args)
