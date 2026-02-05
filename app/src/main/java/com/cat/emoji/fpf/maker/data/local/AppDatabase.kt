package com.cat.emoji.fpf.maker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cat.emoji.fpf.maker.data.local.dao.UserDao
import com.cat.emoji.fpf.maker.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}