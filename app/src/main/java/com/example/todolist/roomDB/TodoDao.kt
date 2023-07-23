package com.example.todolist.roomDB

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.todolist.dataClasses.TodoData

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY time ASC")
    fun getAllTodos(): List<TodoData>

    //    @Query("SELECT * FROM todos WHERE id IN (:userIds)")
//    fun loadAllByIds(userIds: IntArray): List<TodoData>

    @Insert
    suspend fun insert(todo: TodoData)

    @Update
    suspend fun update(todo: TodoData)

    @Update
    suspend fun updateTodos(todos: List<TodoData>)

    @Delete
    suspend fun delete(todo: TodoData)
}