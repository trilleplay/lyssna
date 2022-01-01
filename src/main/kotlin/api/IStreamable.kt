package api

import java.time.Instant

interface IStreamable {
    val title: String
    val description: String?
    val audioUrl: String
    val imageUrl: String?
    val podcastName: String
    val publishedAt: Instant?
    val audioLength: Long?
}