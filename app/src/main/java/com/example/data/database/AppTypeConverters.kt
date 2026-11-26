package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.models.AttendanceStatus
import com.example.data.models.ConnectionType
import com.example.data.models.FileType
import com.example.data.models.HafalanStatus
import com.example.data.models.MessageStatus

class AppTypeConverters {
    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus =
        runCatching { AttendanceStatus.valueOf(value) }.getOrDefault(AttendanceStatus.HADIR)

    @TypeConverter
    fun fromHafalanStatus(value: HafalanStatus): String = value.name

    @TypeConverter
    fun toHafalanStatus(value: String): HafalanStatus =
        runCatching { HafalanStatus.valueOf(value) }.getOrDefault(HafalanStatus.BELUM_SETOR)

    @TypeConverter
    fun fromFileType(value: FileType): String = value.name

    @TypeConverter
    fun toFileType(value: String): FileType =
        runCatching { FileType.valueOf(value) }.getOrDefault(FileType.TEXT)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus =
        runCatching { MessageStatus.valueOf(value) }.getOrDefault(MessageStatus.SENT)

    @TypeConverter
    fun fromConnectionType(value: ConnectionType): String = value.name

    @TypeConverter
    fun toConnectionType(value: String): ConnectionType =
        runCatching { ConnectionType.valueOf(value) }.getOrDefault(ConnectionType.INTERNET_CLOUD)
}
