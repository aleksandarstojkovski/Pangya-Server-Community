package org.pangya.protocol.iff;

/** Parsed subset of C# {@code Caddie} used by holiday-pay checks. */
public record IffCaddieRecord(int typeid, int valorMensal, IffShopFlags shopFlags) {

    /** C# {@code requestPayCaddieHolyDay}: {@code IsCash || valor_mensal > 0}. */
    public boolean canPayHoliday() {
        return shopFlags.isCash() || valorMensal > 0;
    }
}
