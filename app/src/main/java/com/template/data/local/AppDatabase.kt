package com.template.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.template.data.local.dao.AddressDao
import com.template.data.local.dao.MessageDao
import com.template.data.local.entity.ChatMessageEntity
import com.template.data.local.entity.SavedAddressEntity

@Database(
    entities = [ChatMessageEntity::class, SavedAddressEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun addressDao(): AddressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Версия 2 держала список демо-диалогов, в версии 3 его нет: переписка привязана к
         * заказу, а не к воображаемому собеседнику. Удаление таблицы сохраняет сообщения и
         * сохранённые адреса — раньше переход версии стирал базу целиком, и у водителя
         * пропадала история адресов вместе с приложением.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS chat_conversations")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "warehouse_chat_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // Путь 1→3 остаётся разрушающим: такие сборки были только у разработчика
                    .fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
