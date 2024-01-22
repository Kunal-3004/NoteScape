package com.example.notescl.model

import android.os.Parcelable
import android.text.Spanned
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Entity(tableName = "notes")
@Parcelize
data class Note(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title:String,
    val content:String,
    val date:String,
    val userId: String?,
    var imagePath: String?
):Parcelable
