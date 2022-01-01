package api

import dev.stalla.PodcastRssParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.text.StringEscapeUtils
import java.time.Duration
import java.time.Instant

@Serializable
private data class UserStorage(val feeds: List<String>)

class PodcastManager() {
    private val httpClient = HttpClient(CIO) {
        install(UserAgent) {
            agent = "Lyssna"
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10*1000
        }
    }

    private var preferences = java.util.prefs.Preferences.userRoot()


    fun addRssUrl(url: String) {
        val storage = Json.decodeFromString<UserStorage>(preferences.get("podcasts", """{"feeds": []}"""));

        val allUrls = storage.feeds.toMutableList()
        allUrls.add(url)
        preferences.put("podcasts", Json.encodeToString<UserStorage>(UserStorage(
            feeds = allUrls.toList()
        )))
    }

    suspend fun getPodcasts(): Iterable<IStreamable> {
        val recentEpisodes: MutableList<PodcastEpisode> = mutableListOf()

        val storage = Json.decodeFromString<UserStorage>(preferences.get("podcasts", """{"feeds": []}"""));
        val tagsToBeRemoved = "</?p[^>]*>".toRegex()
        storage.feeds.forEach { rssUrl ->
            val response: HttpResponse = httpClient.get(rssUrl)
            val parsed = PodcastRssParser.parse(response.readText().byteInputStream())
            parsed?.episodes?.forEach { episode ->
                recentEpisodes.add(PodcastEpisode(
                    title = episode.title,
                    description = StringEscapeUtils.unescapeHtml4(episode.description ?: "").replace(tagsToBeRemoved, ""),
                    audioUrl = episode.enclosure.url,
                    imageUrl = episode.itunes?.image?.href,
                    podcastName = parsed.title,
                    publishedAt = Instant.from(episode.pubDate),
                    audioLength = episode.itunes?.duration?.rawDuration?.seconds
                ))
            }
        }
        val filtered = recentEpisodes.filter { Duration.between(it.publishedAt ?: Instant.now(), Instant.now()).toDays() < 180L}.toMutableList()
        filtered.sortByDescending { it.publishedAt }
        return filtered
    }
}
