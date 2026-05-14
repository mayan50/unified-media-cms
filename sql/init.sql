-- Unified Media CMS - DDL
-- PostgreSQL

-- 1. 基础设施

CREATE TABLE storage_nodes (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,       -- LOCAL, MINIO, S3
    connection_config JSONB NOT NULL,
    is_readonly BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_settings (
    id UUID PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value JSONB NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 资产主表

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    media_type VARCHAR(50) NOT NULL,          -- BOOK, COMIC, VIDEO, UNKNOWN
    publish_year INTEGER,
    cover_url VARCHAR(2000),
    summary TEXT,
    locked_fields JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE asset_files (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    storage_node_id UUID NOT NULL REFERENCES storage_nodes(id) ON DELETE RESTRICT,
    file_format VARCHAR(50) NOT NULL,          -- TXT, EPUB, PDF, MP4
    relative_path TEXT NOT NULL,
    file_size BIGINT,
    is_primary BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (storage_node_id, relative_path)
);

-- 3. 图书详情 (1:1)

CREATE TABLE book_details (
    id UUID PRIMARY KEY,
    asset_id UUID UNIQUE REFERENCES assets(id) ON DELETE CASCADE,
    subtitle VARCHAR(500),
    publisher VARCHAR(200),
    published_date DATE,
    language VARCHAR(50),
    pages INTEGER,
    word_count BIGINT,
    chapter_count INTEGER,
    series_name VARCHAR(300),
    series_number DECIMAL(4,1),
    total_books INTEGER,
    completion_status VARCHAR(20),             -- ongoing, completed
    rating DECIMAL(3,1),
    locked_fields JSONB DEFAULT '[]'::jsonb,
    extra_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 维度表

CREATE TABLE creators (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    media_type VARCHAR(50),                    -- BOOK, COMIC, VIDEO, null=通用
    UNIQUE (name, media_type)
);

CREATE TABLE languages (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    native_name VARCHAR(50),
    short_name VARCHAR(20)
);

-- 5. 资产-维度关联

CREATE TABLE asset_creators (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    creator_id UUID NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT '作者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (asset_id, creator_id, role)
);

CREATE TABLE asset_tags (
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (asset_id, tag_id)
);

CREATE TABLE asset_categories (
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (asset_id, category_id)
);

CREATE TABLE external_ids (
    id UUID PRIMARY KEY,
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL,               -- douban, isbn10, isbn13, asin
    identifier VARCHAR(100) NOT NULL,
    url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (asset_id, source)
);

-- 6. 管线模板

CREATE TABLE templates (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    graph_payload JSONB NOT NULL,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. 批处理作业

CREATE TABLE batch_jobs (
    id UUID PRIMARY KEY,
    name VARCHAR(200),
    template_id UUID REFERENCES templates(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',   -- PENDING, RUNNING, COMPLETED, FAILED
    execution_graph JSONB NOT NULL,
    input_storage_node_id UUID REFERENCES storage_nodes(id) ON DELETE SET NULL,
    input_path TEXT,
    output_storage_node_id UUID REFERENCES storage_nodes(id) ON DELETE SET NULL,
    output_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. 文件任务

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES batch_jobs(id) ON DELETE CASCADE,
    file_path TEXT,
    asset_id UUID REFERENCES assets(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',   -- PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
    last_completed_node VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task_node_logs (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    node_name VARCHAR(100) NOT NULL,
    node_label VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    log_output TEXT,
    error_message VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. 索引

CREATE INDEX idx_assets_media_type ON assets(media_type);
CREATE INDEX idx_asset_files_asset ON asset_files(asset_id);
CREATE INDEX idx_asset_files_storage ON asset_files(storage_node_id);
CREATE INDEX idx_book_details_asset ON book_details(asset_id);
CREATE INDEX idx_external_ids_asset ON external_ids(asset_id);
CREATE INDEX idx_external_ids_source ON external_ids(source);
CREATE INDEX idx_asset_creators_asset ON asset_creators(asset_id);
CREATE INDEX idx_batch_jobs_status ON batch_jobs(status);
CREATE INDEX idx_batch_jobs_template ON batch_jobs(template_id);
CREATE INDEX idx_tasks_job ON tasks(job_id);
CREATE INDEX idx_tasks_asset ON tasks(asset_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_task_node_logs_task ON task_node_logs(task_id);

-- 10. 默认模板

INSERT INTO templates (id, name, description, graph_payload, is_default, created_at)
VALUES (
    gen_random_uuid(),
    'default-book-pipeline',
    '默认图书处理流水线：嗅探 → 条件分支 → 提取/解析 → AI分析 → 刮削 → 转换 → 归档',
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
