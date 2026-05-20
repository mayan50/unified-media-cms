package com.unifiedmedia.cms.pipeline.core;

import lombok.Data;

import java.util.*;

/**
 * 资产草稿 — 纯内存 POJO，无任何 JPA 或数据库依赖。
 * <p>
 * 插件开发者在 {@code execute()} 中修改此对象的字段或调用影子方法（addTag 等），
 * 并在节点上声明 {@link EntityDataOperator}，底层 Orchestrator 会自动完成持久化。
 * <p>
 * <b>字段分类：</b>
 * <ul>
 *   <li>基础字段 — 直接映射到 Asset 实体</li>
 *   <li>影子字段 (pending*) — 收集写入意图，sync 后清空</li>
 *   <li>快照字段 (current*) — 从已持久化实体中提取，供下游只读查询</li>
 * </ul>
 */
@Data
public class AssetDraft {

    // ── 基础字段 ──
    private String title;
    private String summary;
    private String coverUrl;
    private Integer publishYear;
    private String mediaType;

    // ── 影子字段：写入意图 ──
    private Set<String> pendingTagNames = new HashSet<>();
    private Set<String> pendingCategoryNames = new HashSet<>();
    /** Key = 角色（如"作者"、"译者"），Value = 名字列表 */
    private Map<String, List<String>> pendingCreators = new HashMap<>();

    // ── 快照字段：已持久化数据，供下游只读 ──
    private Set<String> currentTags = new HashSet<>();
    private Set<String> currentCategories = new HashSet<>();

    // ── 字段锁（从实体同步，节点据此判断是否允许覆盖）──
    private Set<String> lockedFields = new HashSet<>();

    // ── 影子字段操作方法 ──

    public void addTag(String name) {
        if (name != null && !name.isBlank()) this.pendingTagNames.add(name.trim());
    }

    public void addCategory(String name) {
        if (name != null && !name.isBlank()) this.pendingCategoryNames.add(name.trim());
    }

    public void addCreator(String role, String name) {
        if (role == null || name == null || name.isBlank()) return;
        this.pendingCreators.computeIfAbsent(role.trim(), k -> new ArrayList<>()).add(name.trim());
    }

    // ── 字段锁查询 ──

    public boolean isFieldLocked(String fieldName) {
        return lockedFields.contains(fieldName);
    }
}
