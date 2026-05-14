package com.unifiedmedia.cms.plugin;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 目录监听 + 防抖 — 文件稳定 3 秒后才触发热重载。
 */
@Slf4j
public class PluginWatcherService {

    private final Pf4jPluginManager pf4jPluginManager;
    private final Path watchDir;
    private final ScheduledExecutorService debounceScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<Path, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();
    private final WatchService watchService;
    private volatile boolean running = true;

    public PluginWatcherService(Pf4jPluginManager pf4jPluginManager, Path watchDir) throws Exception {
        this.pf4jPluginManager = pf4jPluginManager;
        this.watchDir = watchDir;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    @PostConstruct
    public void start() {
        try {
            Files.createDirectories(watchDir);
            watchDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            log.info("[PluginWatcher] Watching: {}", watchDir);
        } catch (Exception e) {
            log.error("[PluginWatcher] Failed to register watch dir", e);
            return;
        }
        Thread watcher = new Thread(this::watchLoop, "plugin-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void watchLoop() {
        while (running) {
            WatchKey key;
            try { key = watchService.poll(5, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            if (key == null) continue;

            for (WatchEvent<?> event : key.pollEvents()) {
                Path name = (Path) event.context();
                Path full = watchDir.resolve(name);
                String fileName = name.toString().toLowerCase();
                if (!fileName.endsWith(".jar")) continue;

                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    pendingTasks.remove(full);
                    debounceReload(full);
                } else {
                    debounceReload(full);
                }
            }
            key.reset();
        }
    }

    private void debounceReload(Path changedFile) {
        ScheduledFuture<?> existing = pendingTasks.get(changedFile);
        if (existing != null) existing.cancel(false);
        pendingTasks.put(changedFile, debounceScheduler.schedule(() -> {
            try {
                log.info("[PluginWatcher] File stable, reloading: {}", changedFile);
                pendingTasks.remove(changedFile);
                pf4jPluginManager.reloadAllPlugins();
            } catch (Exception e) {
                log.error("[PluginWatcher] Reload failed", e);
            }
        }, 3, TimeUnit.SECONDS));
    }

    @PreDestroy
    public void stop() {
        running = false;
        debounceScheduler.shutdown();
        try { watchService.close(); } catch (Exception ignored) {}
    }
}
