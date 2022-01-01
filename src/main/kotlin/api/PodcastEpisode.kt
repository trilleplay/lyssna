package api

import java.time.Instant

data class PodcastEpisode(
    override val title: String,
    override val description: String?,
    override val audioUrl: String,
    override val imageUrl: String?,
    override val podcastName: String,
    override val publishedAt: Instant?,
    override val audioLength: Long?
) : IStreamable
