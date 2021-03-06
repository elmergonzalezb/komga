package org.gotson.komga.infrastructure.jooq

import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.persistence.SeriesMetadataRepository
import org.gotson.komga.jooq.Tables
import org.gotson.komga.jooq.tables.records.SeriesMetadataRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL.lower
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class SeriesMetadataDao(
  private val dsl: DSLContext
) : SeriesMetadataRepository {

  private val d = Tables.SERIES_METADATA
  private val g = Tables.SERIES_METADATA_GENRE
  private val st = Tables.SERIES_METADATA_TAG

  override fun findById(seriesId: String): SeriesMetadata =
    findOne(seriesId).toDomain(findGenres(seriesId), findTags(seriesId))

  override fun findByIdOrNull(seriesId: String): SeriesMetadata? =
    findOne(seriesId)?.toDomain(findGenres(seriesId), findTags(seriesId))

  private fun findOne(seriesId: String) =
    dsl.selectFrom(d)
      .where(d.SERIES_ID.eq(seriesId))
      .fetchOneInto(d)

  private fun findGenres(seriesId: String) =
    dsl.select(g.GENRE)
      .from(g)
      .where(g.SERIES_ID.eq(seriesId))
      .fetchInto(g)
      .mapNotNull { it.genre }
      .toSet()

  private fun findTags(seriesId: String) =
    dsl.select(st.TAG)
      .from(st)
      .where(st.SERIES_ID.eq(seriesId))
      .fetchInto(st)
      .mapNotNull { it.tag }
      .toSet()

  override fun insert(metadata: SeriesMetadata) {
    dsl.transaction { config ->
      config.dsl().insertInto(d)
        .set(d.SERIES_ID, metadata.seriesId)
        .set(d.STATUS, metadata.status.toString())
        .set(d.TITLE, metadata.title)
        .set(d.TITLE_SORT, metadata.titleSort)
        .set(d.SUMMARY, metadata.summary)
        .set(d.READING_DIRECTION, metadata.readingDirection?.toString())
        .set(d.PUBLISHER, metadata.publisher)
        .set(d.AGE_RATING, metadata.ageRating)
        .set(d.LANGUAGE, metadata.language)
        .set(d.STATUS_LOCK, metadata.statusLock)
        .set(d.TITLE_LOCK, metadata.titleLock)
        .set(d.TITLE_SORT_LOCK, metadata.titleSortLock)
        .set(d.SUMMARY_LOCK, metadata.summaryLock)
        .set(d.READING_DIRECTION_LOCK, metadata.readingDirectionLock)
        .set(d.PUBLISHER_LOCK, metadata.publisherLock)
        .set(d.AGE_RATING_LOCK, metadata.ageRatingLock)
        .set(d.LANGUAGE_LOCK, metadata.languageLock)
        .set(d.GENRES_LOCK, metadata.genresLock)
        .set(d.TAGS_LOCK, metadata.tagsLock)
        .execute()

      insertGenres(config.dsl(), metadata)
      insertTags(config.dsl(), metadata)
    }
  }

  override fun update(metadata: SeriesMetadata) {
    dsl.transaction { config ->
      config.dsl().update(d)
        .set(d.STATUS, metadata.status.toString())
        .set(d.TITLE, metadata.title)
        .set(d.TITLE_SORT, metadata.titleSort)
        .set(d.SUMMARY, metadata.summary)
        .set(d.READING_DIRECTION, metadata.readingDirection?.toString())
        .set(d.PUBLISHER, metadata.publisher)
        .set(d.AGE_RATING, metadata.ageRating)
        .set(d.LANGUAGE, metadata.language)
        .set(d.STATUS_LOCK, metadata.statusLock)
        .set(d.TITLE_LOCK, metadata.titleLock)
        .set(d.TITLE_SORT_LOCK, metadata.titleSortLock)
        .set(d.SUMMARY_LOCK, metadata.summaryLock)
        .set(d.READING_DIRECTION_LOCK, metadata.readingDirectionLock)
        .set(d.PUBLISHER_LOCK, metadata.publisherLock)
        .set(d.AGE_RATING_LOCK, metadata.ageRatingLock)
        .set(d.LANGUAGE_LOCK, metadata.languageLock)
        .set(d.GENRES_LOCK, metadata.genresLock)
        .set(d.TAGS_LOCK, metadata.tagsLock)
        .set(d.LAST_MODIFIED_DATE, LocalDateTime.now(ZoneId.of("Z")))
        .where(d.SERIES_ID.eq(metadata.seriesId))
        .execute()

      config.dsl().deleteFrom(g)
        .where(g.SERIES_ID.eq(metadata.seriesId))
        .execute()

      config.dsl().deleteFrom(st)
        .where(st.SERIES_ID.eq(metadata.seriesId))
        .execute()

      insertGenres(config.dsl(), metadata)
      insertTags(config.dsl(), metadata)
    }
  }

  private fun insertGenres(dsl: DSLContext, metadata: SeriesMetadata) {
    if (metadata.genres.isNotEmpty()) {
      dsl.batch(
        dsl.insertInto(g, g.SERIES_ID, g.GENRE)
          .values(null as String?, null)
      ).also { step ->
        metadata.genres.forEach {
          step.bind(metadata.seriesId, it)
        }
      }.execute()
    }
  }

  private fun insertTags(dsl: DSLContext, metadata: SeriesMetadata) {
    if (metadata.tags.isNotEmpty()) {
      dsl.batch(
        dsl.insertInto(st, st.SERIES_ID, st.TAG)
          .values(null as String?, null)
      ).also { step ->
        metadata.tags.forEach {
          step.bind(metadata.seriesId, it)
        }
      }.execute()
    }
  }

  override fun delete(seriesId: String) {
    dsl.transaction { config ->
      with(config.dsl()) {
        deleteFrom(g).where(g.SERIES_ID.eq(seriesId)).execute()
        deleteFrom(st).where(st.SERIES_ID.eq(seriesId)).execute()
        deleteFrom(d).where(d.SERIES_ID.eq(seriesId)).execute()
      }
    }
  }

  override fun delete(seriesIds: Collection<String>) {
    dsl.transaction { config ->
      with(config.dsl()) {
        deleteFrom(g).where(g.SERIES_ID.`in`(seriesIds)).execute()
        deleteFrom(st).where(st.SERIES_ID.`in`(seriesIds)).execute()
        deleteFrom(d).where(d.SERIES_ID.`in`(seriesIds)).execute()
      }
    }
  }

  override fun count(): Long = dsl.fetchCount(d).toLong()


  private fun SeriesMetadataRecord.toDomain(genres: Set<String>, tags: Set<String>) =
    SeriesMetadata(
      status = SeriesMetadata.Status.valueOf(status),
      title = title,
      titleSort = titleSort,
      summary = summary,
      readingDirection = readingDirection?.let {
        SeriesMetadata.ReadingDirection.valueOf(readingDirection)
      },
      publisher = publisher,
      ageRating = ageRating,
      language = language,
      genres = genres,
      tags = tags,

      statusLock = statusLock,
      titleLock = titleLock,
      titleSortLock = titleSortLock,
      summaryLock = summaryLock,
      readingDirectionLock = readingDirectionLock,
      publisherLock = publisherLock,
      ageRatingLock = ageRatingLock,
      languageLock = languageLock,
      genresLock = genresLock,
      tagsLock = tagsLock,

      seriesId = seriesId,

      createdDate = createdDate.toCurrentTimeZone(),
      lastModifiedDate = lastModifiedDate.toCurrentTimeZone()
    )
}
