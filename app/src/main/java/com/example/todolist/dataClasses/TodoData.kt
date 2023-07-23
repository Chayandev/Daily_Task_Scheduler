package com.example.todolist.dataClasses

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName="todos")
data class TodoData(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var itemText:String,
    var isChecked:Boolean,
    var time:String,
    val date:String,
    var category:String,
    var isExpired:Boolean,
    var isNotified:Boolean
    )
