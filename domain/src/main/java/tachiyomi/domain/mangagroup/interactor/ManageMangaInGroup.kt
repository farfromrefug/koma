package tachiyomi.domain.mangagroup.interactor

import tachiyomi.domain.mangagroup.repository.MangaGroupRepository

class ManageMangaInGroup(
    private val mangaGroupRepository: MangaGroupRepository,
) {

    suspend fun addToGroup(mangaId: Long, groupId: Long) {
        return mangaGroupRepository.addMangaToGroup(mangaId, groupId)
    }

    suspend fun removeFromGroup(mangaId: Long) {
        return mangaGroupRepository.removeMangaFromGroup(mangaId)
    }

    suspend fun moveBetweenGroups(mangaId: Long, newGroupId: Long) {
        // Remove from current group (if any) and add to new group
        return mangaGroupRepository.addMangaToGroup(mangaId, newGroupId)
    }

    suspend fun getMangaInGroup(groupId: Long): List<Long> {
        return mangaGroupRepository.getMangaInGroup(groupId)
    }
}
