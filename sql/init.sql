-- Unified Media CMS - V1.0 DDL
-- PostgreSQL

-- 1. 基础设施：存储节点表
CREATE TABLE storage_nodes (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,  -- 'LOCAL', 'MINIO', 'S3'
    connection_config JSONB NOT NULL,
    is_readonly BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 核心检索维度表 (一等公民)
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    media_type VARCHAR(50),  -- 'BOOK', 'VIDEO', null=通用
    UNIQUE (name, media_type)
);

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE creators (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

-- 3. 逻辑资产与物理文件表 (1:N)
CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    library_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    media_type VARCHAR(50) NOT NULL,     -- V1 强制为 'BOOK'
    publish_year INTEGER,
    cover_url TEXT,
    summary TEXT,
    status VARCHAR(50) NOT NULL,         -- 'PROCESSING', 'PENDING_MANUAL', 'COMPLETED'
    tech_specs JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE media_files (
    id UUID PRIMARY KEY,
    asset_id UUID REFERENCES media_assets(id) ON DELETE CASCADE,
    storage_node_id UUID REFERENCES storage_nodes(id) ON DELETE RESTRICT,
    file_format VARCHAR(50) NOT NULL,    -- 'TXT', 'EPUB'
    relative_path TEXT NOT NULL,
    file_size BIGINT,
    is_primary BOOLEAN DEFAULT true,
    UNIQUE (storage_node_id, relative_path)
);

-- 3.5 资产详情子表 (按 media_type 1:1)
CREATE TABLE book_details (
    id UUID PRIMARY KEY,
    asset_id UUID UNIQUE REFERENCES media_assets(id) ON DELETE CASCADE,
    subtitle VARCHAR(500),
    publisher VARCHAR(200),
    published_date VARCHAR(50),
    language VARCHAR(50),
    isbn_10 VARCHAR(20),
    isbn_13 VARCHAR(20),
    asin VARCHAR(20),
    pages INTEGER,
    series_name VARCHAR(300),
    series_number DECIMAL(4,1),
    total_books INTEGER,
    douban_id VARCHAR(50),
    extra_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3.6 外部标识符 (1:N，跨类型共享)
CREATE TABLE external_identifiers (
    id UUID PRIMARY KEY,
    asset_id UUID REFERENCES media_assets(id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL,     -- 'douban', 'goodreads', 'google', 'amazon', 'tmdb', 'imdb'
    identifier VARCHAR(100) NOT NULL,
    url TEXT,
    UNIQUE (asset_id, source)
);

-- 3.7 外部评分 (1:N，跨类型共享)
CREATE TABLE external_ratings (
    id UUID PRIMARY KEY,
    asset_id UUID REFERENCES media_assets(id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL,     -- 'douban', 'goodreads', 'amazon', 'tmdb'
    score DECIMAL(3,1),
    count INTEGER,
    UNIQUE (asset_id, source)
);

-- 维度关联表
CREATE TABLE asset_categories (
    asset_id UUID,
    category_id UUID,
    PRIMARY KEY (asset_id, category_id)
);

CREATE TABLE asset_tags (
    asset_id UUID,
    tag_id UUID,
    PRIMARY KEY (asset_id, tag_id)
);

CREATE TABLE asset_creators (
    asset_id UUID,
    creator_id UUID,
    role VARCHAR(50),
    PRIMARY KEY (asset_id, creator_id, role)
);

-- 4. 状态机与留痕表
CREATE TABLE pipeline_tasks (
    id UUID PRIMARY KEY,
    name VARCHAR(200),
    asset_id UUID REFERENCES media_assets(id),
    current_status VARCHAR(50) NOT NULL, -- 'QUEUED', 'RUNNING', 'PENDING_MANUAL', 'FAILED', 'COMPLETED'
    template_id UUID REFERENCES pipeline_templates(id),
    execution_graph JSONB,               -- 任务提交时从模板 graph_payload 快照
    stuck_node_id VARCHAR(100),          -- 仲裁挂起时的节点实例 ID
    task_context JSONB,                  -- 序列化的 TaskContext，启动时反序列化
    input_storage_node_id UUID,
    input_path_text TEXT,
    output_storage_node_id UUID,
    output_path_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pipeline_task_logs (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES pipeline_tasks(id) ON DELETE CASCADE,
    node_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,         -- 'SUCCESS', 'SKIPPED', 'FAILED'
    output_payload JSONB,
    error_message TEXT,
    execution_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 流水线模板表 (动态装配，DAG 图)
CREATE TABLE pipeline_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    graph_payload JSONB NOT NULL,        -- DAG 图定义 {"nodes": [...], "edges": [...]}
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 全局设置表
CREATE TABLE app_settings (
    id UUID PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value JSONB NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_media_assets_library ON media_assets(library_id);
CREATE INDEX idx_media_assets_status ON media_assets(status);
CREATE INDEX idx_media_assets_media_type ON media_assets(media_type);
CREATE INDEX idx_media_files_asset ON media_files(asset_id);
CREATE INDEX idx_pipeline_tasks_status ON pipeline_tasks(current_status);
CREATE INDEX idx_pipeline_task_logs_task ON pipeline_task_logs(task_id);
CREATE INDEX idx_pipeline_task_logs_created ON pipeline_task_logs(created_at);

-- 默认图书流水线模板 (DAG 图)
INSERT INTO pipeline_templates (id, name, description, graph_payload, is_default, created_at)
VALUES (
    gen_random_uuid(),
    'default-book-pipeline',
    '默认图书处理流水线：嗅探 → 提取 → AI分析 → 刮削 → 归档',
    '{
      "nodes": [
        {"id":"FileSnifferNode_0","name":"FileSnifferNode","label":"文件嗅探","icon":"🔍","config":{},"condition":""},
        {"id":"RouterNode_1","name":"RouterNode","label":"条件分支","icon":"🔀","config":{},"condition":"detectedFormat == ''TXT''"},
        {"id":"TxtExtractorNode_2","name":"TxtExtractorNode","label":"TXT 采样","icon":"📄","config":{},"condition":""},
        {"id":"EpubMetaParserNode_3","name":"EpubMetaParserNode","label":"EPUB 解析","icon":"📖","config":{},"condition":""},
        {"id":"LlmAnalyzerNode_4","name":"LlmAnalyzerNode","label":"大模型分析","icon":"🤖","config":{},"condition":""},
        {"id":"DoubanScraperNode_5","name":"DoubanScraperNode","label":"豆瓣刮削","icon":"🌐","config":{},"condition":""},
        {"id":"FormatConverterNode_6","name":"FormatConverterNode","label":"TXT→EPUB","icon":"🔄","config":{},"condition":""},
        {"id":"ArchiveNode_7","name":"ArchiveNode","label":"归档写入","icon":"📦","config":{},"condition":""}
      ],
      "edges": [
        {"id":"e-0","source":"FileSnifferNode_0","target":"RouterNode_1","sourcePort":"default"},
        {"id":"e-1","source":"RouterNode_1","target":"TxtExtractorNode_2","sourcePort":"true"},
        {"id":"e-2","source":"RouterNode_1","target":"EpubMetaParserNode_3","sourcePort":"false"},
        {"id":"e-3","source":"TxtExtractorNode_2","target":"LlmAnalyzerNode_4","sourcePort":"default"},
        {"id":"e-4","source":"EpubMetaParserNode_3","target":"LlmAnalyzerNode_4","sourcePort":"default"},
        {"id":"e-5","source":"LlmAnalyzerNode_4","target":"DoubanScraperNode_5","sourcePort":"default"},
        {"id":"e-6","source":"DoubanScraperNode_5","target":"FormatConverterNode_6","sourcePort":"default"},
        {"id":"e-7","source":"FormatConverterNode_6","target":"ArchiveNode_7","sourcePort":"default"}
      ]
    }'::jsonb,
    true,
    CURRENT_TIMESTAMP
);
