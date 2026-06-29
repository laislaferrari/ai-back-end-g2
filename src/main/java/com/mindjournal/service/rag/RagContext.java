package com.mindjournal.service.rag;

import com.mindjournal.dto.SourceDTO;
import java.util.List;

public record RagContext(
    String context,
    List<SourceDTO> sources
) {}
