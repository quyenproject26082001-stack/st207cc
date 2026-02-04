package com.catmaker.leuleu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.catmaker.leuleu.data.local.dao.UserDao
import com.catmaker.leuleu.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}