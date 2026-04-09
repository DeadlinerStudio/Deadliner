package com.aritxonly.deadliner.model

enum class DDLState {
    ACTIVE,
    COMPLETED,
    ARCHIVED,
    ABANDONED,
    ABANDONED_ARCHIVED;

    companion object {
        fun fromWire(value: String): DDLState {
            return when (value.trim().lowercase()) {
                "active" -> ACTIVE
                "completed" -> COMPLETED
                "archived" -> ARCHIVED
                "abandoned" -> ABANDONED
                "abandonedarchived", "abandoned_archived" -> ABANDONED_ARCHIVED
                else -> throw IllegalArgumentException("Invalid DDL state: $value")
            }
        }

        fun fromLegacyFlags(
            isCompleted: Boolean,
            isArchived: Boolean
        ): DDLState {
            return when {
                isArchived -> ARCHIVED
                isCompleted -> COMPLETED
                else -> ACTIVE
            }
        }

        fun fromStoredValue(
            rawState: String?,
            isCompleted: Boolean,
            isArchived: Boolean
        ): DDLState {
            val legacy = fromLegacyFlags(isCompleted, isArchived)
            val candidate = rawState?.trim().orEmpty()
            if (candidate.isEmpty()) return legacy

            val parsed = runCatching { fromWire(candidate) }.getOrNull() ?: return legacy

            // Older Android migrations could backfill the new state column with the
            // SQLite default "active" before legacy booleans were projected over.
            // When that happens, prefer the legacy flags to avoid resurfacing old tasks.
            if (parsed == ACTIVE && legacy != ACTIVE) {
                return legacy
            }
            return parsed
        }
    }

    fun toWire(): String = name.lowercase()

    fun isMainListVisible(): Boolean = this == ACTIVE || this == COMPLETED || this == ABANDONED

    fun isArchiveListVisible(): Boolean = this == ARCHIVED || this == ABANDONED_ARCHIVED

    fun isCompletedFamily(): Boolean = this == COMPLETED || this == ARCHIVED

    fun isAbandonedFamily(): Boolean = this == ABANDONED || this == ABANDONED_ARCHIVED

    fun isArchivedFamily(): Boolean = this == ARCHIVED || this == ABANDONED_ARCHIVED

    fun isActionable(): Boolean = this == ACTIVE

    fun canManualArchive(): Boolean = this == COMPLETED || this == ABANDONED
}
