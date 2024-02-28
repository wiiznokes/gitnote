package com.example.gitnote.ui.model

import com.example.gitnote.MyApp
import com.example.gitnote.R


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

enum class ColumnCount {
    Automatic,
    One,
    Two,
    Tree,
    Four,
    Five,
    Six;

    override fun toString(): String {
        return when (this) {
            Automatic -> MyApp.appModule.uiHelper.getString(
                R.string.automatic
            )

            One -> "1"
            Two -> "2"
            Tree -> "3"
            Four -> "4"
            Five -> "5"
            Six -> "6"
        }
    }
}