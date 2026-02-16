package tachiyomi.domain.source.interactor

import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.domain.source.repository.SourcePagingSource
import tachiyomi.domain.source.repository.SourceRepository

class GetRemoteManga(
    private val repository: SourceRepository,
) {

    operator fun invoke(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource {
        return when {
            query == QUERY_POPULAR -> repository.getPopular(sourceId)
            query == QUERY_LATEST -> repository.getLatest(sourceId)
            query.startsWith(QUERY_HOME_SECTION_PREFIX) -> {
                val sectionId = query.removePrefix(QUERY_HOME_SECTION_PREFIX)
                repository.getHomeSection(sourceId, sectionId)
            }
            else -> repository.search(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "eu.kanade.domain.source.interactor.POPULAR"
        const val QUERY_LATEST = "eu.kanade.domain.source.interactor.LATEST"
        const val QUERY_HOME_SECTION_PREFIX = "eu.kanade.domain.source.interactor.HOME_SECTION:"
    }
}
