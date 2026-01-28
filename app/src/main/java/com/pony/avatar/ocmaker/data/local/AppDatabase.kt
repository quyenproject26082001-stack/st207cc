package com.pony.avatar.ocmaker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pony.avatar.ocmaker.data.local.dao.UserDao
import com.pony.avatar.ocmaker.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}