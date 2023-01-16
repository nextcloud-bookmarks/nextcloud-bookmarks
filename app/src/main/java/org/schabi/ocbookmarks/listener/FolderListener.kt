package org.schabi.ocbookmarks.listener

import org.schabi.ocbookmarks.REST.model.Folder

interface FolderListener {
    fun changeFolderCallback(f: Folder)
}