package com.example.todolist.roomDB

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: TodoDatabase? = null

    fun getInstance(context: Context): TodoDatabase {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.applicationContext,
                TodoDatabase::class.java,
                "todo_database"
            ).build()
        }
        return instance as TodoDatabase
    }
}