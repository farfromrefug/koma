# Summary of Changes

This PR addresses two issues reported in the problem statement:

## Issue 1: Deep Directory Chapter Lookup

**Problem**: When local source scans for chapters in deep/nested directories (e.g., `Manga/Volume 1/Chapter 01.cbz`), the chapters would be detected and added to the manga, but users would get "chapter not found" errors when trying to open them.

**Root Cause**: 
- `getFilesInMangaDirectory()` recursively scanned subdirectories and returned files
- `processChapterFile()` only stored the filename in the chapter URL (e.g., `Chapter 01.cbz`)
- `getFormat()` tried to find the file using only `findFile(chapterName)`, which failed for files in subdirectories

**Solution**:
1. Added `getRelativePath()` method to `LocalSourceFileSystem` to get the full relative path from the manga directory (e.g., `Volume 1/Chapter 01.cbz`)
2. Updated `processChapterFile()` to store the full relative path in the chapter URL
3. Added `findFileByRelativePath()` method to navigate through nested directories
4. Updated `getFormat()` to use the new method that handles multi-level paths

**Files Changed**:
- `source-local/src/androidMain/kotlin/tachiyomi/source/local/LocalSource.kt`
- `source-local/src/androidMain/kotlin/tachiyomi/source/local/io/LocalSourceFileSystem.kt`
- `source-local/src/commonMain/kotlin/tachiyomi/source/local/io/LocalSourceFileSystem.kt`

## Issue 2: Chapter Paging Support

**Problem**: When extensions use APIs that paginate chapter lists (e.g., return 50 chapters per page), only the first page of chapters would be loaded, making the rest inaccessible.

**Solution**:
Created a new interface `PaginatedChapterListSource` that extensions can implement to properly support paginated chapter lists:

1. **ChaptersPage Model**: Similar to `MangasPage`, holds a list of chapters and a `hasNextPage` flag
2. **PaginatedChapterListSource Interface**: 
   - Defines `getChapterList(manga: SManga, page: Int): ChaptersPage` for paginated fetching
   - Provides default implementation of `getChapterList(manga: SManga)` that automatically loads all pages
   - Ensures backward compatibility with existing code
3. **Helper Extensions**:
   - `getChapterListFlow()`: Returns a Flow for progressive chapter loading in UI
   - `supportsPaginatedChapterList()`: Check if a source supports pagination
4. **Comprehensive Documentation**: 
   - Examples showing how to implement the interface
   - Different patterns for detecting the next page (API flag, pagination elements, page size)
   - Real-world scenario explanations

**Files Changed**:
- `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/model/ChaptersPage.kt` (new)
- `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/PaginatedChapterListSource.kt` (new)
- `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/SourceExtensions.kt` (new)
- `source-api/PAGINATED_CHAPTERS.md` (new)

## Benefits

### Deep Directory Support
- ✅ Chapters in nested directories now work correctly
- ✅ No changes needed to manga organization structure
- ✅ Maintains compatibility with existing flat directory structures

### Chapter Paging Support
- ✅ Extensions can now properly support APIs with paginated chapter lists
- ✅ Users can access all chapters, not just the first page
- ✅ Default implementation ensures backward compatibility
- ✅ Optional Flow-based API for progressive loading in UI
- ✅ No breaking changes to existing sources or app code

## Testing Recommendations

1. **Deep Directory Testing**:
   - Create a test manga with chapters in nested directories (e.g., `TestManga/Volume 1/Chapter 01.cbz`)
   - Verify chapters are detected correctly
   - Verify chapters can be opened and read
   - Test with multiple nesting levels

2. **Paging Testing**:
   - Create a mock extension implementing `PaginatedChapterListSource`
   - Return multiple pages of chapters (e.g., 3 pages with 10 chapters each)
   - Verify all 30 chapters are loaded
   - Verify pagination stops when `hasNextPage = false`

## Backward Compatibility

Both changes maintain full backward compatibility:
- Existing local sources with flat directories continue to work
- Existing extensions that don't implement `PaginatedChapterListSource` continue to work unchanged
- All existing app code that calls `getChapterList(manga)` continues to work
