package com.unifiedmedia.cms.pipeline.payload;

import java.util.List;
import java.util.Map;

/**
 * 防腐层 DTO — 所有刮削节点产出的统一载体。
 * 管道内绝不流转第三方 API 的裸 Map。
 */
public record ScrapeCandidate(
    String source,
    String externalId,
    String title,
    String summary,
    String coverUrl,
    List<String> authors,
    List<String> tags,
    Map<String, String> extraData
) {}
