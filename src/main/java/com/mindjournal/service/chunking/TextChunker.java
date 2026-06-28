package com.mindjournal.service.chunking;

import java.util.List;

public interface TextChunker {

    List<String> chunk(String text);
}
