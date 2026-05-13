package com.unifiedmedia.cms.config;

import com.unifiedmedia.cms.pipeline.appliers.BookDetailApplier;
import com.unifiedmedia.cms.pipeline.core.PipelineEngine;
import com.unifiedmedia.cms.pipeline.core.PipelineNode;
import com.unifiedmedia.cms.pipeline.loaders.BookDetailLoader;
import com.unifiedmedia.cms.pipeline.mergers.CreatorRelationMerger;
import com.unifiedmedia.cms.pipeline.mergers.TagRelationMerger;
import com.unifiedmedia.cms.pipeline.nodes.control.RouterNode;
import com.unifiedmedia.cms.pipeline.nodes.control.WaitNode;
import com.unifiedmedia.cms.pipeline.nodes.control.RenameNode;
import com.unifiedmedia.cms.pipeline.nodes.io.ArchiveNode;
import com.unifiedmedia.cms.pipeline.nodes.io.FileSnifferNode;
import com.unifiedmedia.cms.pipeline.nodes.parser.ChapterParserNode;
import com.unifiedmedia.cms.pipeline.nodes.parser.EpubMetaParserNode;
import com.unifiedmedia.cms.pipeline.nodes.processing.FormatConverterNode;
import com.unifiedmedia.cms.pipeline.nodes.processing.LlmAnalyzerNode;
import com.unifiedmedia.cms.pipeline.nodes.processing.MetadataApplyNode;
import com.unifiedmedia.cms.pipeline.nodes.processing.TxtExtractorNode;
import com.unifiedmedia.cms.pipeline.nodes.processing.VideoFrameNode;
import com.unifiedmedia.cms.pipeline.nodes.scraper.DoubanScraperNode;
import com.unifiedmedia.cms.pipeline.nodes.scraper.TmdbScraperNode;
import com.unifiedmedia.cms.pipeline.spi.CandidateApplier;
import com.unifiedmedia.cms.pipeline.spi.MediaDetailLoader;
import com.unifiedmedia.cms.pipeline.spi.RelationMerger;
import com.unifiedmedia.cms.repository.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PipelineAutoConfiguration {

    // ── Engine ──
    @Bean
    public PipelineEngine pipelineEngine() { return new PipelineEngine(); }

    // ── Loaders ──
    @Bean
    public BookDetailLoader bookDetailLoader(BookDetailRepository repo) { return new BookDetailLoader(repo); }

    // ── Relations ──
    @Bean
    public TagRelationMerger tagRelationMerger(TagRepository repo) { return new TagRelationMerger(repo); }

    @Bean
    public CreatorRelationMerger creatorRelationMerger(CreatorRepository cr, AssetCreatorRepository acr) {
        return new CreatorRelationMerger(cr, acr);
    }

    // ── Appliers ──
    @Bean
    public BookDetailApplier bookDetailApplier() { return new BookDetailApplier(); }

    // ── IO Nodes ──
    @Bean
    public FileSnifferNode fileSnifferNode() { return new FileSnifferNode(); }

    @Bean
    public ArchiveNode archiveNode(AssetRepository ar, AssetFileRepository afr, BookDetailRepository bdr,
                                    List<RelationMerger> mergers) {
        return new ArchiveNode(ar, afr, bdr, mergers);
    }

    // ── Parser Nodes ──
    @Bean
    public ChapterParserNode chapterParserNode() { return new ChapterParserNode(); }

    @Bean
    public EpubMetaParserNode epubMetaParserNode() { return new EpubMetaParserNode(); }

    // ── Processing Nodes ──
    @Bean
    public TxtExtractorNode txtExtractorNode() { return new TxtExtractorNode(); }

    @Bean
    public LlmAnalyzerNode llmAnalyzerNode(
            com.fasterxml.jackson.databind.ObjectMapper om,
            org.springframework.ai.ollama.OllamaChatModel ollama,
            org.springframework.ai.openai.OpenAiChatModel openai) {
        return new LlmAnalyzerNode(om, ollama, openai);
    }

    @Bean
    public FormatConverterNode formatConverterNode() { return new FormatConverterNode(); }

    @Bean
    public VideoFrameNode videoFrameNode() { return new VideoFrameNode(); }

    @Bean
    public MetadataApplyNode metadataApplyNode(List<CandidateApplier> appliers) {
        return new MetadataApplyNode(appliers);
    }

    // ── Scraper Nodes ──
    @Bean
    public DoubanScraperNode doubanScraperNode() { return new DoubanScraperNode(); }

    @Bean
    public TmdbScraperNode tmdbScraperNode() { return new TmdbScraperNode(); }

    // ── Control Nodes ──
    @Bean
    public RouterNode routerNode() { return new RouterNode(); }

    @Bean
    public WaitNode waitNode() { return new WaitNode(); }

    @Bean
    public RenameNode renameNode() { return new RenameNode(); }
}
