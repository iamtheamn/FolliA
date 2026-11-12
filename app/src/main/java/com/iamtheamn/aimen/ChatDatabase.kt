package com.iamtheamn.aimen

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: Int,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllConversations(): List<ConversationEntity>

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY id ASC")
    suspend fun getMessagesForConversation(convId: Int): List<MessageEntity>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE conversations SET title = :newTitle WHERE id = :convId")
    suspend fun updateConversationTitle(convId: Int, newTitle: String)

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :convId")
    suspend fun updateConversationPinned(convId: Int, isPinned: Boolean)

    @Query("DELETE FROM conversations WHERE id = :convId")
    suspend fun deleteConversation(convId: Int)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteMessagesForConversation(convId: Int)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Insert
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert
    suspend fun insertMessages(messages: List<MessageEntity>)

    /** Remplace tout le contenu de la base de façon atomique (tout ou rien). */
    @Transaction
    suspend fun replaceAll(conversations: List<ConversationEntity>, messages: List<MessageEntity>) {
        deleteAllMessages()
        deleteAllConversations()
        insertConversations(conversations)
        insertMessages(messages)
    }
}

@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                )
                    // Seuls les anciens schémas (v0.5 / v0.6) sont recréés à neuf. À partir de la
                    // version 3, tout changement de schéma doit fournir une Migration explicite :
                    // plus aucune perte silencieuse des conversations.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}