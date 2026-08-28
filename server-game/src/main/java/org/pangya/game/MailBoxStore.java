package org.pangya.game;

import org.pangya.protocol.game.GamePackets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory C# {@code PlayerMailBox} cache. MSSQL {@code ProcColocaMsgNoGiftTable}
 * is not in the Java schema; text-only send still needs a store so {@code 0x143}/{@code 0x144}
 * can round-trip after {@code 0x145}.
 */
final class MailBoxStore {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConcurrentHashMap<Long, List<MailEntry>> boxes = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    MailEntry add(long toUid, String fromId, String msg) {
        return add(toUid, fromId, msg, 0, List.of());
    }

    MailEntry add(long toUid, String fromId, String msg, int itemNum) {
        return add(toUid, fromId, msg, itemNum, List.of());
    }

    MailEntry add(long toUid, String fromId, String msg, List<MailAttachment> items) {
        List<MailAttachment> copy = items == null ? List.of() : List.copyOf(items);
        return add(toUid, fromId, msg, copy.size(), copy);
    }

    /** C# {@code addNewEmailArrived} stand-in when Auth pushes mail id. */
    MailEntry addArrived(long toUid, int mailId, String fromId, String msg) {
        int id = mailId > 0 ? mailId : nextId.getAndIncrement();
        MailEntry entry = new MailEntry(
                id,
                fromId == null ? "Auth" : fromId,
                msg == null ? "" : msg,
                LocalDate.now().format(DATE),
                0,
                new ArrayList<>());
        boxes.computeIfAbsent(toUid, uid -> new ArrayList<>()).add(entry);
        return entry;
    }

    private MailEntry add(long toUid, String fromId, String msg, int itemNum, List<MailAttachment> items) {
        MailEntry entry = new MailEntry(
                nextId.getAndIncrement(),
                fromId == null ? "" : fromId,
                msg == null ? "" : msg,
                LocalDate.now().format(DATE),
                itemNum,
                new ArrayList<>(items));
        boxes.computeIfAbsent(toUid, uid -> new ArrayList<>()).add(entry);
        return entry;
    }

    /**
     * C# {@code GetPage}: empty box returns empty even for a huge page; a page
     * past the last existing page throws (Channel catch then writes the default).
     */
    List<MailEntry> page(long uid, int page) {
        if (page <= 0) {
            throw new IllegalArgumentException("page");
        }
        List<MailEntry> all = sorted(uid);
        if (all.isEmpty()) {
            return List.of();
        }
        int total = totalPages(all.size());
        if (page > total) {
            throw new IllegalArgumentException("page");
        }
        int start = (page - 1) * GamePackets.MAIL_PER_PAGE;
        int end = Math.min(start + GamePackets.MAIL_PER_PAGE, all.size());
        return List.copyOf(all.subList(start, end));
    }

    int totalPages(long uid) {
        int n = sorted(uid).size();
        if (n <= 0) {
            return 0;
        }
        return totalPages(n);
    }

    List<MailEntry> unread(long uid) {
        List<MailEntry> out = new ArrayList<>();
        for (MailEntry entry : sorted(uid)) {
            if (entry.lidaYn != 0) {
                continue;
            }
            out.add(entry);
            if (out.size() >= GamePackets.MAIL_UNREAD_LIMIT) {
                break;
            }
        }
        return out;
    }

    Optional<MailEntry> get(long uid, int id, boolean markRead) {
        List<MailEntry> box = boxes.get(uid);
        if (box == null || box.isEmpty()) {
            return Optional.empty();
        }
        synchronized (box) {
            for (MailEntry entry : box) {
                if (entry.id == id) {
                    if (markRead) {
                        if (entry.lidaYn == 0) {
                            entry.lidaYn = 1;
                        }
                        entry.visitCount++;
                    }
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * C# {@code leftItensFromEmail}: clear attachments. Empty box or id &le; 0 throws
     * (Channel catch {@code 0x5500100}). Missing id is a no-op.
     */
    void leftItems(long uid, int emailId) {
        if (emailId <= 0) {
            throw new IllegalArgumentException("id");
        }
        List<MailEntry> box = boxes.get(uid);
        if (box == null || box.isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        synchronized (box) {
            for (MailEntry entry : box) {
                if (entry.id == emailId) {
                    entry.items.clear();
                    entry.itemNum = 0;
                    return;
                }
            }
        }
    }

    boolean isEmpty(long uid) {
        List<MailEntry> box = boxes.get(uid);
        return box == null || box.isEmpty();
    }

    /**
     * C# {@code deleteEmail}: empty box or id &le; 0 throws; missing ids are skipped.
     */
    void delete(long uid, int[] ids) {
        if (ids == null) {
            throw new IllegalArgumentException("ids");
        }
        for (int id : ids) {
            if (id <= 0) {
                throw new IllegalArgumentException("id");
            }
        }
        List<MailEntry> box = boxes.get(uid);
        if (box == null || box.isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        synchronized (box) {
            for (int id : ids) {
                box.removeIf(entry -> entry.id == id);
            }
        }
    }

    private List<MailEntry> sorted(long uid) {
        List<MailEntry> box = boxes.get(uid);
        if (box == null || box.isEmpty()) {
            return List.of();
        }
        List<MailEntry> copy;
        synchronized (box) {
            copy = new ArrayList<>(box);
        }
        copy.sort(Comparator.comparingInt((MailEntry e) -> e.id).reversed());
        return copy;
    }

    private static int totalPages(int count) {
        return (count + GamePackets.MAIL_PER_PAGE - 1) / GamePackets.MAIL_PER_PAGE;
    }

    static byte[] toListBytes(MailEntry entry) {
        return GamePackets.mailBoxEntry(entry.id, entry.fromId, entry.msg, entry.visitCount, entry.lidaYn, entry.itemNum);
    }

    static final class MailAttachment {
        final int typeid;
        final int qntd;

        MailAttachment(int typeid, int qntd) {
            this.typeid = typeid;
            this.qntd = qntd;
        }
    }

    static final class MailEntry {
        final int id;
        final String fromId;
        final String msg;
        final String giftDate;
        int itemNum;
        final List<MailAttachment> items;
        int visitCount;
        int lidaYn;

        MailEntry(
                int id,
                String fromId,
                String msg,
                String giftDate,
                int itemNum,
                List<MailAttachment> items) {
            this.id = id;
            this.fromId = fromId;
            this.msg = msg;
            this.giftDate = giftDate;
            this.itemNum = itemNum;
            this.items = items;
        }
    }
}
