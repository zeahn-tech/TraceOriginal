package com.example

import android.app.Application
import androidx.room.Room
import com.example.auth.SessionManager
import com.example.data.local.TraceNetDatabase
import com.example.data.repository.TraceNetRepository
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TraceNetApplication : Application() {
    companion object {
        lateinit var instance: TraceNetApplication
            private set
    }

    lateinit var database: TraceNetDatabase
        private set
    
    lateinit var repository: TraceNetRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
        database = Room.databaseBuilder(
            this,
            TraceNetDatabase::class.java,
            "tracenet_db"
        )
        // NOTE: Destructive migration removed to prevent accidental data loss on schema changes.
        .build()
        val syncManager = com.example.data.firebase.FirebaseSyncManager(this, database.dao())
        repository = TraceNetRepository(database.dao(), syncManager)

        // Clean up predefined sample/mock data from local database on startup
        // NOTE: Destructive one-time cleanup removed for production safety.
    }
}

