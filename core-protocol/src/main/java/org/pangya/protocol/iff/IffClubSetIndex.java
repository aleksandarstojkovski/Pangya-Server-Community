package org.pangya.protocol.iff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** C# {@code sIff.findClubSet}: typeid → workshop stats/slots. */
public record IffClubSetIndex(Map<Integer, IffClubSetRecord> byTypeid) {

    public Optional<IffClubSetRecord> find(int typeid) {
        return Optional.ofNullable(byTypeid.get(typeid));
    }

    /** C# {@code sIff.findClubSetOriginal}: clubsets sharing {@code text_pangya}. */
    public List<IffClubSetRecord> findOriginals(int specialTypeid) {
        IffClubSetRecord special = byTypeid.get(specialTypeid);
        if (special == null || special.textPangya() == 0) {
            return List.of();
        }
        int text = special.textPangya();
        List<IffClubSetRecord> out = new ArrayList<>();
        for (IffClubSetRecord row : byTypeid.values()) {
            if (row.textPangya() == text) {
                out.add(row);
            }
        }
        out.sort(Comparator.comparingInt(IffClubSetRecord::typeid));
        return List.copyOf(out);
    }

    public boolean contains(int typeid) {
        return byTypeid.containsKey(typeid);
    }

    public boolean isEmpty() {
        return byTypeid.isEmpty();
    }

    public int size() {
        return byTypeid.size();
    }

    public static IffClubSetIndex empty() {
        return new IffClubSetIndex(Map.of());
    }
}
