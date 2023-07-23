package com.example.todolist.roomDB

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todolist.dataClasses.TodoData
import com.example.todolist.roomDB.TodoDao

@Database(entities = [TodoData::class], version = 1)
abstract class TodoDatabase:RoomDatabase() {
    abstract fun todoDao(): TodoDao
}