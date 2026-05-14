package com.unifiedmedia.cms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.pipeline.appliers.BookDetailApplier;
import com.unifiedmedia.cms.pipeline.core.PipelineEngine;
import com.unifiedmedia.cms.pipeline.core.PipelineNode;
import com.unifiedmedia.cms.pipeline.loaders.BookDetailLoader;
import com.unifiedmedia.cms.pipeline.mergers.CreatorRelationMerger;
import com.unifiedmedia.cms.pipeline.mergers.TagRelationMerger;
import com.unifiedmedia.cms.pipeline.nodes.io.ArchiveNode;
import com.unifiedmedia.cms.pipeline.nodes.io.FileSnifferNode;
import com.unifiedmedia.cms.pipeline.nodes.processing.LlmAnalyzerNode;
import com.unifiedmedia.cms.pipeline.nodes.scraper.DoubanScraperNode;
import com.unifiedmedia.cms.pipeline.nodes.scraper.TmdbScraperNode;
import com.unifiedmedia.cms.pipeline.spi.CandidateApplier;
import com.unifiedmedia.cms.pipeline.spi.MediaDetailLoader;
import com.unifiedmedia.cms.pipeline.spi.RelationMerger;
import com.unifiedmedia.cms.plugin.NodeRegistry;
import com.unifiedmedia.cms.plugin.Pf4jPluginManager;
import com.unifiedmedia.cms.plugin.PluginWatcherService;
import com.unifiedmedia.cms.repository.*;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

@Configuration
public class PipelineAutoConfiguration {

    @Value("${cms.plugin.dir:./plugins}")
    private String pluginDirPath;

    private boolean isDevMode() {
        return "development".equalsIgnoreCase(System.getProperty("pf4j.mode", "deployment"));
    }

    private Path resolvePluginDir() {
        Path dir;
        if (isDevMode()) {
            dir = Path.of(System.getProperty("user.dir"), "..", "plugins").toAbsolutePath().normalize();
        } else {
            dir = Path.of(pluginDirPath).toAbsolutePath().normalize();
        }
        try {
            if (!java.nio.file.Files.exists(dir)) java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException e) { throw new RuntimeException("Cannot create plugin dir: " + dir, e); }
        return dir;
    }

    // ── Registry ──
    @Bean
    public NodeRegistry nodeRegistry() { return new NodeRegistry(); }

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

    // ── PF4J Plugin Manager ──
    @Bean
    public Pf4jPluginManager pf4jPluginManager(NodeRegistry registry) {
        Pf4jPluginManager pm = new Pf4jPluginManager(resolvePluginDir(), registry);
        pm.loadAllPlugins();
        return pm;
    }

    @Bean
    public PluginWatcherService pluginWatcherService(Pf4jPluginManager pm) throws Exception {
        return new PluginWatcherService(pm, resolvePluginDir());
    }

    @Bean
    public com.unifiedmedia.cms.plugin.PluginController pluginController(NodeRegistry registry, Pf4jPluginManager pm) {
        return new com.unifiedmedia.cms.plugin.PluginController(registry, pm);
    }

    // ── Built-in Nodes (unmigrated) ──
    @Bean public FileSnifferNode fileSnifferNode(NodeRegistry r) {
        var n = new FileSnifferNode(); r.registerBuiltIn(n); return n; }
    @Bean public ArchiveNode archiveNode(AssetRepository ar, AssetFileRepository afr, BookDetailRepository bdr,
                                    List<RelationMerger> mergers, NodeRegistry r) {
        var n = new ArchiveNode(ar, afr, bdr, mergers); r.registerBuiltIn(n); return n; }
    @Bean public LlmAnalyzerNode llmAnalyzerNode(ObjectMapper om, OllamaChatModel ollama,
                                            OpenAiChatModel openai, NodeRegistry r) {
        var n = new LlmAnalyzerNode(om, ollama, openai); r.registerBuiltIn(n); return n; }
    @Bean public DoubanScraperNode doubanScraperNode(NodeRegistry r) {
        var n = new DoubanScraperNode(); r.registerBuiltIn(n); return n; }
    @Bean public TmdbScraperNode tmdbScraperNode(NodeRegistry r) {
        var n = new TmdbScraperNode(); r.registerBuiltIn(n); return n; }

    // 其余 9 个节点已迁移至插件 JAR，由 PF4J 加载
}
