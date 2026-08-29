package org.pangya.protocol.iff;

/**
 * C# {@code FlagShop} ({@code IFFShopData.flag_shop}, 4 bytes at record+128).
 * {@link #isCash()} and {@link #isGift()} drive {@code requestDeleteActiveItem}.
 */
public record IffShopFlags(int shopFlag, int moneyFlag) {

    private static final int SHOP_FLAG_NON_GIFTABLE = 2;
    private static final int SHOP_FLAG_PANG = 32;
    private static final int SHOP_FLAG_COOKIES_0 = 33;
    private static final int SHOP_FLAG_COMBINE96 = 96;
    private static final int SHOP_FLAG_COMBINE97 = 97;

    private static final int MONEY_FLAG_NONE = 0;
    private static final int MONEY_FLAG_ACTIVE = 1;
    private static final int MONEY_FLAG_BANNER_NEW = 8;

    /** C# {@code FlagShop.IsCash}. */
    public boolean isCash() {
        if (shopFlag == 6 && moneyFlag == MONEY_FLAG_NONE) {
            return false;
        }
        if (moneyFlag == MONEY_FLAG_NONE && shopFlag == MONEY_FLAG_NONE) {
            return false;
        }
        if (moneyFlag == MONEY_FLAG_NONE && shopFlag == SHOP_FLAG_NON_GIFTABLE) {
            return false;
        }
        if (shopFlag == SHOP_FLAG_PANG) {
            return false;
        }
        if (shopFlag == SHOP_FLAG_COMBINE96) {
            return false;
        }
        if (shopFlag == SHOP_FLAG_COMBINE97) {
            return true;
        }
        int cookieBit = shopFlag & 2;
        if (cookieBit == 0) {
            return isShop();
        }
        if ((shopFlag & 1) == 1) {
            return true;
        }
        return false;
    }

    /** C# {@code FlagShop.IsGift}. */
    public boolean isGift() {
        if ((shopFlag & 6) == 4) {
            return true;
        }
        if ((shopFlag == 32 || shopFlag == SHOP_FLAG_COOKIES_0) && moneyFlag == MONEY_FLAG_NONE) {
            return true;
        }
        if (shopFlag == SHOP_FLAG_COOKIES_0 && moneyFlag == MONEY_FLAG_ACTIVE) {
            return true;
        }
        if (shopFlag == SHOP_FLAG_COOKIES_0 && moneyFlag == MONEY_FLAG_BANNER_NEW) {
            return true;
        }
        if (shopFlag == 32 && moneyFlag == MONEY_FLAG_BANNER_NEW) {
            return false;
        }
        return false;
    }

    /** C# {@code FlagShop.IsShop} (subset used by {@link #isCash()}). */
    private boolean isShop() {
        if (shopFlag == 6 && moneyFlag == MONEY_FLAG_NONE) {
            return false;
        }
        if (moneyFlag == MONEY_FLAG_NONE && shopFlag == MONEY_FLAG_NONE) {
            return false;
        }
        if (moneyFlag == MONEY_FLAG_NONE && shopFlag == SHOP_FLAG_NON_GIFTABLE) {
            return false;
        }
        if (moneyFlag == 21 && shopFlag == SHOP_FLAG_NON_GIFTABLE) {
            return false;
        }
        if (shopFlag == 1 && moneyFlag == MONEY_FLAG_BANNER_NEW) {
            return false;
        }
        if (shopFlag == MONEY_FLAG_NONE && moneyFlag == MONEY_FLAG_BANNER_NEW) {
            return false;
        }
        if (shopFlag == MONEY_FLAG_NONE && moneyFlag == MONEY_FLAG_ACTIVE) {
            return false;
        }
        return isNew() || isHot() || isSpecial() || isNormal() || isSaleable() || isDisplay();
    }

    private boolean isDisplay() {
        return shopFlag == 128 && moneyFlag == MONEY_FLAG_NONE;
    }

    private boolean isNormal() {
        return (shopFlag == 38 && moneyFlag == MONEY_FLAG_NONE)
                || (shopFlag == 32 && moneyFlag == MONEY_FLAG_ACTIVE)
                || (shopFlag == 32 && moneyFlag == MONEY_FLAG_BANNER_NEW)
                || (shopFlag == SHOP_FLAG_COOKIES_0 && moneyFlag == MONEY_FLAG_ACTIVE)
                || (shopFlag == SHOP_FLAG_COOKIES_0 && moneyFlag == MONEY_FLAG_BANNER_NEW)
                || (shopFlag == SHOP_FLAG_COMBINE97 && moneyFlag == MONEY_FLAG_NONE)
                || (shopFlag == SHOP_FLAG_COMBINE96 && moneyFlag == MONEY_FLAG_NONE)
                || (shopFlag == 98 && moneyFlag == MONEY_FLAG_NONE)
                || (shopFlag == 33 && moneyFlag == MONEY_FLAG_NONE)
                || (shopFlag == 34 && moneyFlag == MONEY_FLAG_NONE)
                || (shopFlag == SHOP_FLAG_PANG && moneyFlag == MONEY_FLAG_NONE)
                || (shopFlag == 21 && moneyFlag == MONEY_FLAG_NONE);
    }

    private boolean isSaleable() {
        int result = shopFlag & 5;
        if (result == 1 || result == 0) {
            if (shopFlag == MONEY_FLAG_NONE || moneyFlag == MONEY_FLAG_NONE) {
                if (shopFlag == SHOP_FLAG_COOKIES_0) {
                    return true;
                }
                if (shopFlag == 34) {
                    return true;
                }
                return false;
            }
            return false;
        }
        if (result == 4) {
            return true;
        }
        return false;
    }

    private boolean isNew() {
        if ((moneyFlag & 1) == 1) {
            return true;
        }
        if (moneyFlag == MONEY_FLAG_NONE) {
            if (shopFlag == 34 || shopFlag == 33) {
                return false;
            }
            if (shopFlag == SHOP_FLAG_NON_GIFTABLE && moneyFlag == MONEY_FLAG_NONE) {
                return false;
            }
            return shopFlag == SHOP_FLAG_NON_GIFTABLE;
        }
        if (moneyFlag == MONEY_FLAG_ACTIVE) {
            if (shopFlag == 33 || shopFlag == 34) {
                return true;
            }
            return shopFlag == SHOP_FLAG_NON_GIFTABLE;
        }
        return false;
    }

    private boolean isHot() {
        return (moneyFlag & 2) == 2;
    }

    private boolean isSpecial() {
        if ((moneyFlag & 3) == 3) {
            return true;
        }
        return (shopFlag == 32 && moneyFlag == 3) || (shopFlag == 33 && moneyFlag == 3);
    }
}
