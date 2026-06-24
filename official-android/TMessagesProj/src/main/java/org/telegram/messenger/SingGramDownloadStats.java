package org.telegram.messenger;

import android.os.SystemClock;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class SingGramDownloadStats {

    private static final int MAX_ITEMS = 24;
    private static final long ACTIVE_TIMEOUT_MS = 15000;
    private static final long KEEP_DONE_MS = 180000;

    private static final Object lock = new Object();
    private static final LinkedHashMap<String, Item> items = new LinkedHashMap<>();

    private static class Item {
        String fileName;
        long downloadedSize;
        long totalSize;
        long speedBytesPerSecond;
        long lastUpdateTime;
        boolean completed;
        boolean failed;
    }

    public static class ItemSnapshot {
        public final String fileName;
        public final long downloadedSize;
        public final long totalSize;
        public final long speedBytesPerSecond;
        public final long lastUpdateTime;
        public final boolean active;
        public final boolean completed;
        public final boolean failed;

        private ItemSnapshot(Item item, long now) {
            fileName = item.fileName;
            downloadedSize = item.downloadedSize;
            totalSize = item.totalSize;
            speedBytesPerSecond = item.speedBytesPerSecond;
            lastUpdateTime = item.lastUpdateTime;
            completed = item.completed;
            failed = item.failed;
            active = !completed && !failed && now - lastUpdateTime <= ACTIVE_TIMEOUT_MS;
        }
    }

    public static class Snapshot {
        public final int activeCount;
        public final long speedBytesPerSecond;
        public final ArrayList<ItemSnapshot> items;
        public final long updatedAt;

        private Snapshot(int activeCount, long speedBytesPerSecond, ArrayList<ItemSnapshot> items, long updatedAt) {
            this.activeCount = activeCount;
            this.speedBytesPerSecond = speedBytesPerSecond;
            this.items = items;
            this.updatedAt = updatedAt;
        }
    }

    public static void onProgress(String fileName, long downloadedSize, long totalSize) {
        if (TextUtils.isEmpty(fileName)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        synchronized (lock) {
            Item item = items.remove(fileName);
            if (item == null) {
                item = new Item();
                item.fileName = fileName;
            }
            if (item.lastUpdateTime > 0 && downloadedSize >= item.downloadedSize) {
                long deltaTime = now - item.lastUpdateTime;
                long deltaBytes = downloadedSize - item.downloadedSize;
                if (deltaTime >= 250 && deltaBytes >= 0) {
                    item.speedBytesPerSecond = deltaBytes * 1000L / deltaTime;
                }
            } else {
                item.speedBytesPerSecond = 0;
            }
            item.downloadedSize = Math.max(0, downloadedSize);
            item.totalSize = Math.max(0, totalSize);
            item.lastUpdateTime = now;
            item.completed = item.totalSize > 0 && item.downloadedSize >= item.totalSize;
            item.failed = false;
            items.put(fileName, item);
            prune(now);
        }
    }

    public static void onFinished(String fileName) {
        markDone(fileName, true);
    }

    public static void onFailed(String fileName) {
        markDone(fileName, false);
    }

    public static Snapshot getSnapshot() {
        long now = SystemClock.elapsedRealtime();
        synchronized (lock) {
            prune(now);
            ArrayList<ItemSnapshot> result = new ArrayList<>();
            int activeCount = 0;
            long speed = 0;
            ArrayList<Item> ordered = new ArrayList<>(items.values());
            for (int i = ordered.size() - 1; i >= 0; i--) {
                ItemSnapshot snapshot = new ItemSnapshot(ordered.get(i), now);
                if (snapshot.active) {
                    activeCount++;
                    speed += snapshot.speedBytesPerSecond;
                }
                result.add(snapshot);
            }
            return new Snapshot(activeCount, speed, result, now);
        }
    }

    public static void clear() {
        synchronized (lock) {
            items.clear();
        }
    }

    private static void markDone(String fileName, boolean completed) {
        if (TextUtils.isEmpty(fileName)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        synchronized (lock) {
            Item item = items.remove(fileName);
            if (item == null) {
                item = new Item();
                item.fileName = fileName;
            }
            item.lastUpdateTime = now;
            item.speedBytesPerSecond = 0;
            item.completed = completed;
            item.failed = !completed;
            items.put(fileName, item);
            prune(now);
        }
    }

    private static void prune(long now) {
        Iterator<Item> iterator = items.values().iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if ((item.completed || item.failed) && now - item.lastUpdateTime > KEEP_DONE_MS) {
                iterator.remove();
            }
        }
        while (items.size() > MAX_ITEMS) {
            Iterator<String> keyIterator = items.keySet().iterator();
            if (!keyIterator.hasNext()) {
                break;
            }
            keyIterator.next();
            keyIterator.remove();
        }
    }
}
