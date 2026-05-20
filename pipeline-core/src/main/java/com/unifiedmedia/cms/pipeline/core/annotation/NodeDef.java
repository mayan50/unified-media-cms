package com.unifiedmedia.cms.pipeline.core.annotation;

import com.unifiedmedia.cms.pipeline.core.NodeType;

import java.lang.annotation.*;

/**
 * 节点元数据声明注解 — 全局统一的节点自描述机制。
 * <p>
 * 无论节点来自 Spring 容器还是 PF4J 插件，都通过此注解声明其静态元数据。
 * 引擎在注册阶段通过反射读取，无需实例化节点 Bean。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NodeDef {

    /** 唯一标识，如 "DoubanScraperNode" */
    String name();

    /** 展示名称，如 "豆瓣刮削" */
    String label() default "";

    /** 图标（emoji），如 "🌐" */
    String icon() default "";

    /** 节点类型 */
    NodeType type();

    /** 功能描述 */
    String description() default "";

    /** 是否在前端画布中隐藏（系统级节点） */
    boolean hiddenFromUI() default false;
}
