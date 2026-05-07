-- Migration: V1.1 - DAG Engine Support
-- Run this on existing database to add DAG columns

-- pipeline_templates: add graph_payload column
ALTER TABLE pipeline_templates ADD COLUMN IF NOT EXISTS graph_payload JSONB;

-- pipeline_templates: make node_names nullable (now optional when graph_payload exists)
ALTER TABLE pipeline_templates ALTER COLUMN node_names DROP NOT NULL;

-- pipeline_tasks: add DAG execution columns
ALTER TABLE pipeline_tasks ADD COLUMN IF NOT EXISTS template_id UUID REFERENCES pipeline_templates(id);
ALTER TABLE pipeline_tasks ADD COLUMN IF NOT EXISTS execution_graph JSONB;
ALTER TABLE pipeline_tasks ADD COLUMN IF NOT EXISTS stuck_node_id VARCHAR(100);

-- Update default template with DAG graph_payload
UPDATE pipeline_templates
SET graph_payload = '{
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
}'::jsonb
WHERE name = 'default-book-pipeline';
