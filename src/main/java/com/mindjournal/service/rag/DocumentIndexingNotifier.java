package com.mindjournal.service.rag;

import com.mindjournal.entity.Document;

public interface DocumentIndexingNotifier {

    void notifyIndexed(Document document, int indexedChunks);
}
