package echo.music.iad1tya.utils

import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Makes artwork load reliably.
 *  - Missing image (404/410): walk down to the next smaller YouTube size that exists, then the .jpg twin of a .webp.
 *  - Any other failure (flaky network, timeouts, 5xx, rate limit): retry the same URL with growing pauses,
 *    so a thumbnail never stays blank just because the first attempt hit a bad moment.
 */
class ThumbnailFallbackInterceptor : Interceptor {
  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val first = chain.proceed()
    if (first !is ErrorResult) return first
    val url = chain.request.data as? String ?: return first
    if (!url.startsWith("http")) return first

    val status = (first.throwable as? HttpException)?.response?.code
    val missing = status == 404 || status == 410

    if (missing) {
      for (candidate in smallerVariants(url)) {
        val r = attempt(chain, candidate)
        if (r is SuccessResult) return r
      }
      return first
    }

    // transient failure: same URL, three more tries with backoff
    for (pause in longArrayOf(600, 1500, 3500)) {
      try { delay(pause) } catch (e: CancellationException) { throw e }
      val r = attempt(chain, url)
      if (r is SuccessResult) return r
      val code = ((r as? ErrorResult)?.throwable as? HttpException)?.response?.code
      if (code == 404 || code == 410) {
        for (candidate in smallerVariants(url)) {
          val rr = attempt(chain, candidate)
          if (rr is SuccessResult) return rr
        }
        break
      }
    }
    return first
  }

  private suspend fun attempt(chain: Interceptor.Chain, data: String): ImageResult =
    chain.withRequest(chain.request.newBuilder().data(data).build()).proceed()

  private fun smallerVariants(url: String): List<String> {
    val m = Regex("(maxresdefault|sddefault|hqdefault|mqdefault|default)(\\.jpg|\\.webp)").find(url) ?: return emptyList()
    val order = listOf("maxresdefault", "sddefault", "hqdefault", "mqdefault", "default")
    val out = ArrayList<String>()
    for (q in order.drop(order.indexOf(m.groupValues[1]) + 1)) out += url.replace(m.value, q + m.groupValues[2])
    if (m.groupValues[2] == ".webp") out += url.replace("/vi_webp/", "/vi/").replace(".webp", ".jpg")
    return out
  }
}
