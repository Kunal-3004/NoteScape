package com.example.notescl.database

import android.app.Application
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notescl.model.Note
import java.util.concurrent.locks.Lock

@Database(entities = [Note::class], version = 3)
abstract class NoteDatabase: RoomDatabase(){

    abstract fun getNoteDao():NoteDao

    companion object{
        @Volatile
        private var instance:NoteDatabase?=null
        private var LOCK =Any()

        operator fun invoke(context: Context)= instance?:
        synchronized(LOCK){
            instance?:
            createDatabase(context).also{
                instance=it
            }

        }

        private fun createDatabase(context: Context)=
            Room.databaseBuilder(
                context.applicationContext,
                NoteDatabase::class.java,
                "note_db"
            ).fallbackToDestructiveMigration()
                .build()

    }
}