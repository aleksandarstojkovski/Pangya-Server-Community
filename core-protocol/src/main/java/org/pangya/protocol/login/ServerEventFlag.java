package org.pangya.protocol.login;

/**
 * C# {@code uEventFlag} helpers used when Auth updates rates
 * ({@code GameService.setRatePang} / {@code setRateExp} / etc.).
 */
public final class ServerEventFlag {

    private int value;

    public ServerEventFlag(int initial) {
        this.value = initial & 0xffff;
    }

    public int value() {
        return value & 0xffff;
    }

    /** C# {@code setRatePang}: bit 1 when pang &gt;= 200. */
    public void setRatePang(int pang) {
        setBit(1, pang >= 200);
    }

    /** C# {@code setRateExp}: bit 2 at 200, bit 4 above 200. */
    public void setRateExp(int exp) {
        setBit(2, false);
        setBit(4, false);
        if (exp > 200) {
            setBit(4, true);
        } else if (exp == 200) {
            setBit(2, true);
        }
    }

    /** C# {@code setAngelEvent}: bit 3 when angel event active. */
    public void setAngelEvent(int angelEvent) {
        setBit(3, angelEvent > 0);
    }

    /** C# {@code setRateClubMastery}: bit 7 when club mastery &gt;= 200. */
    public void setRateClubMastery(int clubMastery) {
        setBit(7, clubMastery >= 200);
    }

    private void setBit(int bit, boolean on) {
        if (on) {
            value |= (1 << bit);
        } else {
            value &= ~(1 << bit);
        }
    }
}
