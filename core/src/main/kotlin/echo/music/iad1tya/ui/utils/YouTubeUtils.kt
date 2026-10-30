package echo.music.iad1tya.ui.utils

fun String.resize(
  width: Int? = null,
  height: Int? = null,
): String {
  if (width == null && height == null) return this

  if (this.contains("i.ytimg.com")) {
    // maxresdefault is huge and often missing (404, then a retry), which made artwork load slowly.
    // sddefault (640x480) is sharp enough for a phone screen and exists for almost every video.
    val targetQuality = if (width != null && width >= 1200) "sddefault.jpg" else "hqdefault.jpg"
    return this.replace(
      Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg"),
      targetQuality
    )
  }

  if (this.contains("googleusercontent.com") && this.contains("=w")) {
    val baseUrl = this.split("=w")[0]
    val size = if ((width ?: 0) >= 1000 || (height ?: 0) >= 1000) 1200 else 500
    return "$baseUrl=w$size-h$size"
  }

  if (this.contains("yt3.ggpht.com")) {

    val baseUrl = this.split("=")[0].split("-s")[0]
    return "$baseUrl=s${width ?: height}"
  }

  "https://lh\\d\\.googleusercontent\\.com/.*".toRegex().matchEntire(this)?.let {
    val size = if ((width ?: 0) >= 1000 || (height ?: 0) >= 1000) 1200 else 500
    return "${this.split("=")[0]}=w$size-h$size"
  }

  return this
}
