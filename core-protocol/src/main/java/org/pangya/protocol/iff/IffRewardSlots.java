package org.pangya.protocol.iff;

/** C# {@code GrandPrixData.Reward}: five typeid/qntd/time slots. */
public record IffRewardSlots(int[] typeids, int[] qntd, int[] time) {

    public static final int SLOTS = 5;

    public IffRewardSlots {
        typeids = copy(typeids);
        qntd = copy(qntd);
        time = copy(time);
    }

    static IffRewardSlots read(byte[] data, int base) {
        int[] typeids = new int[SLOTS];
        int[] qntd = new int[SLOTS];
        int[] time = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            typeids[i] = readU32(data, base + i * 4);
            qntd[i] = readU32(data, base + 20 + i * 4);
            time[i] = readU32(data, base + 40 + i * 4);
        }
        return new IffRewardSlots(typeids, qntd, time);
    }

    private static int[] copy(int[] values) {
        if (values == null || values.length != SLOTS) {
            return new int[SLOTS];
        }
        return values.clone();
    }

    private static int readU32(byte[] data, int offset) {
        return java.nio.ByteBuffer.wrap(data, offset, 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }
}
