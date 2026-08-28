package org.pangya.game.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 * C# {@code Pangya_GameServer.UTIL.Lottery}: rate-weighted draw with removal.
 * Used by {@link org.pangya.game.catalog.CoinCubeGenerator} and other game systems.
 */
public final class Lottery {

    public static final class Entry<T> {
        private int prob;
        private T value;
        private final long[] offset = new long[2];
        private boolean active = true;

        public T value() {
            return value;
        }

        public int prob() {
            return prob;
        }
    }

    private final Random rnd;
    private final List<Entry<Object>> ctx = new ArrayList<>();
    private final List<Long> randValues = new ArrayList<>();
    private final TreeMap<Long, Entry<Object>> roleta = new TreeMap<>();
    private long probLimit;

    public Lottery() {
        this(null);
    }

    /** C# {@code Lottery(ulong _value_rand)} — optional deterministic seed for tests. */
    public Lottery(Long seed) {
        if (seed != null) {
            rnd = new Random(seed);
        } else {
            rnd = new Random();
        }
        initialize();
    }

    public void clear() {
        ctx.clear();
    }

    /** C# {@code Push(uint _prob, object _value)}. */
    public <T> void push(int prob, T value) {
        Entry<Object> entry = new Entry<>();
        entry.prob = prob;
        entry.value = value;
        entry.active = true;
        ctx.add(entry);
    }

    /** C# {@code getCountItem}. */
    public int countItems() {
        return ctx.size();
    }

    /** C# {@code spinRoleta(bool _remove_item_draw)}. */
    @SuppressWarnings("unchecked")
    public <T> Entry<T> spinRoleta(boolean removeDrawn) {
        fillRoleta();
        shuffleValuesRand();

        int pick = rnd.nextInt(4);
        long lucky = Math.floorMod(
                randValues.get(pick) * Integer.toUnsignedLong(rnd.nextInt()),
                probLimit == 0 ? 1 : probLimit + 1);

        var ceiling = roleta.ceilingEntry(lucky);
        if (ceiling == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Entry<T> drawn = (Entry<T>) ceiling.getValue();
        if (removeDrawn && drawn != null) {
            drawn.active = false;
        }
        return drawn;
    }

    private void initialize() {
        randValues.clear();
        for (int i = 0; i < 5; i++) {
            randValues.add(rnd.nextLong() & Long.MAX_VALUE);
        }
        shuffleValuesRand();
    }

    private void fillRoleta() {
        if (ctx.isEmpty()) {
            throw new IllegalStateException("lottery ctx empty");
        }
        roleta.clear();
        List<Entry<Object>> shuffled = new ArrayList<>(ctx);
        shuffle(shuffled, createStrongRandom());
        probLimit = 0;
        for (Entry<Object> entry : shuffled) {
            if (!entry.active) {
                continue;
            }
            int weight = entry.prob <= 0 ? 100 : entry.prob;
            entry.offset[0] = probLimit == 0 ? probLimit : probLimit + 1;
            entry.offset[1] = probLimit += weight;
            roleta.put(entry.offset[0], entry);
            roleta.put(entry.offset[1], entry);
        }
    }

    private void shuffleValuesRand() {
        shuffle(randValues, createStrongRandom());
        shuffle(randValues, new Random(rnd.nextInt()));
    }

    private static Random createStrongRandom() {
        byte[] buffer = new byte[8];
        new SecureRandom().nextBytes(buffer);
        long seed64 = 0;
        for (int i = 0; i < 8; i++) {
            seed64 = (seed64 << 8) | (buffer[i] & 0xffL);
        }
        int seed32 = (int) (seed64 ^ (seed64 >>> 32));
        return new Random(seed32);
    }

    private static <T> void shuffle(List<T> list, Random rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}
