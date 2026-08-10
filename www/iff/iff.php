<?php
// Carrega o arquivo que já possui a classe IFFArchive e a função find_iff_item
require_once __DIR__ . '/find_iff_item.php';
// SHOP FLAGS
define('SHOP_FLAG_DISPLAY', 85);
define('SHOP_FLAG_ONLY_DISPLAY', 128);
define('SHOP_FLAG_UNKNOWN03', 3);
define('SHOP_FLAG_UNKNOWN64', 64);
define('SHOP_FLAG_COOKIES_0', 33);
define('SHOP_FLAG_PANG', 32);
define('SHOP_FLAG_ACTIVE', 37);
define('SHOP_FLAG_PERSONALSHOP_ACTIVE', 18);
define('SHOP_FLAG_UNKNOWN16', 16);
define('SHOP_FLAG_UNKNOWN8', 8);
define('SHOP_FLAG_TRADEABLE', 7);
define('SHOP_FLAG_UNKNOWN5', 5);
define('SHOP_FLAG_COUPON', 4);
define('SHOP_FLAG_NONGIFTABLE1', 69);
define('SHOP_FLAG_NONGIFTABLE', 2);
define('SHOP_FLAG_NONE', 0);
define('SHOP_FLAG_BANNER_SPECIAL', 3);
define('SHOP_FLAG_BANNER_HOT', 64);
define('SHOP_FLAG_COMBINE', 97);
define('SHOP_FLAG_COMBINE96', 96);
define('SHOP_FLAG_COMBINE98', 99);
define('SHOP_FLAG_COMBINE99', 99);
define('SHOP_FLAG_BANNER_NEW', 2); 
define('SHOP_FLAG_GIFTABLE', 2);
define('MONEY_FLAG_BANNER_NEW', 1);
define('MONEY_FLAG_ACTIVE', 2);
define('MONEY_FLAG_21', 21);

function find_desc(int $search_id): ?array {
    $found = PangyaIFF\Parser\find_iff_desc($search_id, 'Desc.iff');
    
    return $found;
}
  function find_all(int $search_id): ?array {
        $iff_files = [
             'Card.iff',
            'SetItem.iff',
            'Part.iff',
            'Item.iff',
			'Skin.iff',
			'ClubSet.iff',
			'Character.iff',
			'Caddie.iff',
			'AuxPart.iff',
			'Mascot.iff',
        ];

        foreach ($iff_files as $filename) {
            // Utiliza a classe IFFArchive existente
            $hasInZip = \PangyaIFF\Parser\IFFArchive::has($filename);

            if (!$hasInZip) {
                continue; 
            }

            $found = PangyaIFF\Parser\find_iff_item($search_id, $filename);
            
            if ($found) {
                $found['_source'] = $filename;
                return $found;
            } else {
            }
        }
        return null;
    }
	
	
 function buildItemUI(array $item): array {
    $flags = getItemFlags($item);
$_hide =(isHide($item) ===true?1:0);
    $ui = [ 
        'IsPang'     => $flags['IsPang'],
        'IsCookie'     => $flags['IsCookie'],
        'IsNormal'     => $flags['IsNormal'],
        'IsSaleable'     => $flags['IsSaleable'],
        'can_send_mail_and_personal_shop'     => $flags['can_send_mail_and_personal_shop'],
        'IsNew'        => $flags['IsNew'],
        'IsGift'       => $flags['IsGift'],
        'IsHot'        => $flags['IsHot'],
        'IsDisplay'    => $flags['IsOnlyDisplay'],
        'IsPSQ'        => $flags['IsPSQ'],
        'IsShop'       => $flags['IsShop'],
        'IsHide'  => $_hide,
    ];

    // --- Mesmo comportamento do C# ---
    if (
        !$ui['IsNormal'] &&
        !$ui['IsNew'] &&
        !$ui['IsGift'] &&
        !$ui['IsHot'] &&
        !$ui['IsDisplay'] &&
        !$ui['IsPSQ'] &&
        !$ui['IsHide']
    ) {
        $ui['IsHide'] = 1;
    } else {
        if ($ui['IsHide'] && $ui['IsPSQ']) {
            $ui['IsHide'] = 0;
            $ui['IsPSQ'] = 1;
        }
    }

    if ($ui['IsNew'] && $ui['IsHot']) {
        if ($flags['IsHot']) {
            $ui['IsNew'] = 0;
        }
    }

    return $ui;
}

function isHide(array $item): bool {	 
    // --- Bit flags ---
$price_type = $item['price_type'];
$money_flag = $item['money_flag'];
$shop = getItemFlags($item);
$flags = new IFFShopFlags($item['price_type'], $item['money_flag']);
 
    $now = getdate();

    $year  = $item['date_start_year'] ?? 0;
    $month = $item['date_start_month'] ?? 0;
    $day   = $item['date_start_day'] ?? 0;
    $timeActive = $item['active_item_time'] ?? false;

    // === 1. Verifica se há tempo ativo configurado ===
    if ($timeActive) {
        if ($year >= $now['year'] && $day >= $now['mday'] && $month >= $now['mon'])
            return 0;

        if ($now['year'] > $year && $now['mday'] > $day && $now['mon'] > $month && !$shop['IsShop'])
            return 1;

        if ($year < $now['year'] && $shop['IsShop'])
            return 1;
    }

    // === 2. Se data expirada e não for shop ===
    if ($now['year'] > $year && $now['mday'] > $day && $now['mon'] > $month && !$shop['IsShop']) {
        if ($year == 0) {
            // date.Clear() equivalente → zera data
            $date = ['year' => 0, 'month' => 0, 'day' => 0];

            // Condições baseadas em flags (igual ao C#)
            if ($item['money_flag'] == 0 && $item['price_type'] == 0) return 1;
            if ($item['money_flag'] == 0 && $item['price_type'] == SHOP_FLAG_BANNER_NEW) return 0;
            if ($item['price_type'] == 6 && $item['money_flag'] == 0) return 1;
            if ($item['money_flag'] == 0 && $item['price_type'] == 0) return 1;
            if ($item['money_flag'] == 0 && $item['price_type'] == SHOP_FLAG_BANNER_NEW) return 0;
            if ($item['money_flag'] == MONEY_FLAG_21 && $item['price_type'] == SHOP_FLAG_BANNER_NEW) return 1;
            if ($item['price_type'] == SHOP_FLAG_GIFTABLE && $item['money_flag'] == MONEY_FLAG_BANNER_NEW) return 1;
            if ($item['price_type'] == 0 && $item['money_flag'] == MONEY_FLAG_BANNER_NEW) return 1;
            if ($item['price_type'] == 0 && $item['money_flag'] == MONEY_FLAG_ACTIVE) return 1;
            if ($item['price_type'] == SHOP_FLAG_COMBINE96 && $item['money_flag'] == MONEY_FLAG_BANNER_NEW) return 1;

            return 0;
        } else {
            return 1;
        }
    }

    // === 3. Outras condições especiais ===
    if ($item['price_type'] == SHOP_FLAG_COMBINE96 && $item['money_flag'] == MONEY_FLAG_BANNER_NEW)
        return 0;

    if ($year > 0 && $day > 0 && $now['year'] > $year && $now['mday'] > $day && $shop['IsShop'])
        return 1;

    if (
        !$shop['IsNormal'] &&
        (!$shop['IsSaleable'] || !$shop['can_send_mail_and_personal_shop'] || !$shop['IsDuplication']) &&
        !$shop['IsNew'] &&
        !$flags->IsGiftItem() == 1 &&
        !$shop['IsHot'] &&
        !$shop['IsDisplay']
    ) {
        return 1;
    }

    if ($item['price_type'] == 6 && $item['money_flag'] == 0)
        return 1;

    if ($item['price_type'] == SHOP_FLAG_GIFTABLE && $item['money_flag'] == MONEY_FLAG_ACTIVE)
        return 1;

    if ($item['price_type'] == SHOP_FLAG_COMBINE96 && $item['money_flag'] == MONEY_FLAG_ACTIVE)
        return 0;

    return 0;
} 

function getItemFlags(array $item_data): array
{
    $flags = new IFFShopFlags($item_data['price_type'], $item_data['money_flag']);
$isPSQ =$flags->IsPSQ();
if($flags->IsTradeable())
                {
                    $isPSQ = true;
                }
                else if ($flags->can_send_mail_and_personal_shop())
                {
                    $isPSQ = true;
                }
				else
                $isPSQ = false;
    return [
        'IsPang'  => $flags->isPang() === true? 1:0,
        'IsCookie'  => $flags->isCash() === true? 0: 1,
        'IsDup'  => $flags->isDuplication() === true? 1:0,
        'IsShop'         => $flags->isShop() === true? 1:0,
        'IsNormal'         => $flags->isNormal() === true? 1:0,
        'can_send_mail_and_personal_shop'         => $flags->can_send_mail_and_personal_shop() === true? 1:0,
        'IsSaleable'         => $flags->isSaleable() === true? 1:0,
        'IsPSQ'          => $isPSQ === true? 1:0,
        'IsNew'          => $flags->isNew() === true? 1:0,
        'IsHot'          => $flags->isHot() === true? 1:0,
        'IsGift'     => $flags->IsGiftItem() === true? 0:1,
        'IsOnlyGift'     => $flags->isOnlyGift() === true? 1:0,
        'IsOnlyDisplay'     => $flags->isDisplayOnly() === true? 1:0,
    ];
}

 class IFFShopFlags
{
    private int $shopFlag;
    private int $moneyFlag;

    public function __construct(int $shopFlag, int $moneyFlag)
    {
        $this->shopFlag = $shopFlag;
        $this->moneyFlag = $moneyFlag;
    }

    // ---------- AUX ---------- //
    private function getBit(int $b, int $bit): bool
    {
        return ($b & (1 << $bit)) != 0;
    }

    private function setBit(int $b, int $bit, bool $value): int
    {
        if ($value) {
            return $b | (1 << $bit);
        }
        return $b & ~(1 << $bit);
    }

    // ---------- SHOP FLAGS ---------- //
    public function can_send_mail_and_personal_shop(): bool
    {
        return $this->getBit($this->shopFlag, 1);
    }
 
    public function isDuplication(): bool
    {
        return $this->getBit($this->shopFlag, 2);
    }

    public function isSaleable(): bool
    {
        $result = $this->shopFlag & 5;
        if ($result === 0 || $result === 1) {
            if ($this->shopFlag === 0 || $this->moneyFlag === 0) {
                return in_array($this->shopFlag, [0, 34]); // Cookies_0 ou 34
            }
            return false;
        }
        return true;
    }

    public function isGift(): bool
    {
        $result = ($this->shopFlag & 6) === 4;
        if ($result) return true;

        if (in_array($this->shopFlag, [32, 0]) && in_array($this->moneyFlag, [0, 2, 1])) {
            return true;
        }

        if ($this->shopFlag === 32 && $this->moneyFlag === 1) return false;

        return false;
    }
	
	public function IsGiftItem(): bool
{
    // Saleable ou giftable nunca os 2 juntos por que é a flag composta Somente Purchase
    $is_giftable  = $this->IsGift() ? 1 : 0;
    $is_saleable  = $this->isSaleable() ? 1 : 0;

    if ($this->IsCash() && (($is_saleable ^ $is_giftable) == 1)) {
        return true;
    } elseif ($this->IsGift()) {
        return true;
    }

    return false;
}

	
	

    public function isDisplayOnly(): bool
    {
        return $this->shopFlag === 128 && $this->moneyFlag === 0; // Only_Display
    }
 

        public function IsOnlyPurchase(): bool
        {
            return ($this->isSaleable()
                    && $this->IsGift());
        }

        public function IsOnlyGift(): bool
        {
            return ($this->IsCash()
                    && $this->isSaleable() && $this->IsGift() == false);
        }

    public function isPang(): bool
    {
        $result = $this->shopFlag & 1;
        return !($result === 1);
    }

    public function isCash(): bool
    {
        $result = $this->shopFlag & 1;
        return ($result === 0);
    }

    // ---------- MONEY FLAGS ---------- //
    public function isNew(): bool
    {
        return ($this->moneyFlag & 1) === 1;
    }

    public function isHot(): bool
    {
        return ($this->moneyFlag & 2) === 2;
    }

    public function isSpecial(): bool
    {
        return ($this->moneyFlag & 3) === 3;
    }

    public function isPack(): bool
    {
        return ($this->moneyFlag & 4) === 4;
    }

    public function isNormal(): bool
    {
        return !$this->isNew() && !$this->isHot() && !$this->isGift() && !$this->isDisplayOnly();
    }

    public function isTradeable(): bool
    {
        return $this->shopFlag === 6 && !$this->isCash();
    }	
	
    public function isPSQ(): bool
    {
        return in_array($this->shopFlag, [2, 98, 7]);
    }

    public function isShop(): bool
    {
        return $this->isNew() || $this->isHot() || $this->isSpecial() || $this->isNormal() || $this->isSaleable() || $this->isDisplayOnly();
    }
}
