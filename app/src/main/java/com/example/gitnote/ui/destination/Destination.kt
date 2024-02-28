package com.example.gitnote.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Destination : Parcelable {

    @Parcelize
    data class Init(val initDestination: InitDestination) : Destination

    @Parcelize
    data class App(val appDestination: AppDestination) : Destination

}

// path
// url
// username
// token


// utile pour l'user
// link pour creer repo
// lihk pour creer token
// link pour voir ses repos

// permission -> main -> fileExplo -> Process -> Destination.Grid
// Destination.Grid -> Init.main