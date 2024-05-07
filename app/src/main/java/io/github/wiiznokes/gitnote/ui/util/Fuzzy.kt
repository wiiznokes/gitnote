package io.github.wiiznokes.gitnote.ui.util

import io.github.wiiznokes.gitnote.data.room.Note
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult

// todo: maybe replace with a Kotlin impl (https://github.com/solo-studios/kt-fuzzy)

private const val MIN_SCORE = 50

// todo: highlight matching part (maybe by return List<Note, FuzzyMatch>> to avoid wrapping the note struct
fun fuzzySort(
    query: String,
    notes: List<Note>
): List<Note> {

    val names = notes.map { it.nameWithoutExtension() }
    val contents = notes.map { it.content }

    val namesRes = FuzzySearch.extractSorted(query, names)

    val contentsRes = FuzzySearch.extractSorted(query, contents)

    assert(notes.size >= namesRes.size && notes.size >= contentsRes.size)

    return mergeSortedLists(
        notes = notes,
        sortedList1 = namesRes,
        sortedList2 = contentsRes,
    )
}


// todo: maybe use iterator instead
private fun mergeSortedLists(
    notes: List<Note>,
    sortedList1: List<ExtractedResult>,
    sortedList2: List<ExtractedResult>
): List<Note> {

    val alreadyAddedNotes = BooleanArray(notes.size) { false }
    val mergedList = mutableListOf<Note>()

    var index1 = 0
    var index2 = 0

    fun maybeAdd(fuzzyResult: ExtractedResult) {
        if (alreadyAddedNotes[fuzzyResult.index] || fuzzyResult.score < MIN_SCORE) {
            return
        }
        alreadyAddedNotes[fuzzyResult.index] = true
        mergedList.add(notes[fuzzyResult.index])
        return
    }

    while (index1 < sortedList1.size && index2 < sortedList2.size) {
        val fuzzyResult1 = sortedList1[index1]
        val fuzzyResul2 = sortedList2[index2]

        if (fuzzyResult1.score >= fuzzyResul2.score) {
            maybeAdd(fuzzyResult1)
            index1++

        } else {
            maybeAdd(fuzzyResul2)
            index2++
        }
    }

    // Add remaining elements from list1, if any
    while (index1 < sortedList1.size) {
        maybeAdd(sortedList1[index1])
        index1++
    }

    // Add remaining elements from list2, if any
    while (index2 < sortedList2.size) {
        maybeAdd(sortedList2[index2])
        index2++
    }

    return mergedList
}
