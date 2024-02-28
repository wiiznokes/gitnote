package com.example.gitnote.ui.destination

import android.os.Parcelable
import com.example.gitnote.data.room.Note
import com.example.gitnote.ui.model.EditType
import kotlinx.parcelize.Parcelize


sealed interface AppDestination : Parcelable {
    @Parcelize
    data object Grid : AppDestination

    @Parcelize
    data class Edit(val note: Note, val editType: EditType) : AppDestination

    @Parcelize
    data class Settings(val settingsDestination: SettingsDestination) : AppDestination

}

