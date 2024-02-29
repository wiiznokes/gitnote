package com.example.gitnote.ui.model

import com.example.gitnote.MyApp
import com.example.gitnote.R
import com.example.gitnote.data.room.Note


enum class SortType {
    Modification,
    AlphaNumeric;

    override fun toString(): String {
        val res = when (this) {
            Modification -> R.string.modification_sort_type
            AlphaNumeric -> R.string.alpha_numeric_sort_type
        }
        return MyApp.appModule.uiHelper.getString(res)
    }
}

enum class SortOrder {
    Ascending,
    Descending;

    override fun toString(): String {
        val res = when (this) {
            Ascending -> R.string.ascending_sort_order
            Descending -> R.string.descending_sort_order
        }
        return MyApp.appModule.uiHelper.getString(res)
    }
}

enum class NoteMinWidth(val size: Int) {
    Default(200),
    C250(250),
    C300(300),
    C350(350),
    C400(400),
    C500(500),
    C600(600);
    override fun toString(): String = this.size.toString()
}

data class GridNote(
    val note: Note,
    val title: String,
    val selected: Boolean,
)