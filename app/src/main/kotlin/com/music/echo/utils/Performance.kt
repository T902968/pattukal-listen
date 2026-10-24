package echo.music.iad1tya.utils

import androidx.compose.runtime.Composable
import echo.music.iad1tya.constants.BatterySaverKey
import echo.music.iad1tya.constants.LowEndModeKey
import echo.music.iad1tya.constants.ReduceAnimationsKey

/** True when the user turned on Low-end mode or Battery saver: heavy visuals should be skipped. */
@Composable
fun rememberLitePerformance(): Boolean {
  val (lowEnd, _) = rememberPreference(LowEndModeKey, false)
  val (battery, _) = rememberPreference(BatterySaverKey, false)
  return lowEnd || battery
}

/** True when decorative animations should stop (Reduce animations, Low-end mode or Battery saver). */
@Composable
fun rememberReduceMotion(): Boolean {
  val (reduce, _) = rememberPreference(ReduceAnimationsKey, false)
  return reduce || rememberLitePerformance()
}
