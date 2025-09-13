package io.github.wiiznokes.gitnote.ui.utils

import io.github.wiiznokes.gitnote.data.room.Note
import java.util.Vector

private const val TAG = "Fuzzy"

// todo: highlight matching part (maybe by return List<Note, FuzzyMatch>> to avoid wrapping the note struct
fun fuzzySort(
    query: String,
    notes: List<Note>
): List<Note> {

    val names = notes.map { it.nameWithoutExtension() }
    val contents = notes.map { it.content }


    val v1 = Vector<Int>()
    val v2 = Vector<Int>()

    val done = BooleanArray(notes.size) { false }


    for ((pos, name) in names.withIndex()) {

        if (name.contains(query, ignoreCase = true)) {
            done[pos] = true

            if (name.startsWith(query)) {
                v1.add(pos)
            } else {
                v2.add(pos)
            }
        }

    }

    for ((pos, name) in contents.withIndex()) {
        if (done[pos]) {
            continue
        }
        if (name.contains(query, ignoreCase = true)) {

            v2.add(pos)
        }

    }

    val v1n = v1.map { pos ->
        notes[pos]
    }

    val v2n = v2.map { pos ->
        notes[pos]
    }

    val res = v1n + v2n

    return res
}


