<?php
//desenvolvimento Luiz MK, luizinrc@hotmail.com
namespace PangyaIFF\Parser;
 
const IFF_DESC_SIZE = 516;
 
$IFF_DESC_FORMAT =
    'Vtype_id/' .  // 4 bytes (uint32)
    'a512info';    // 512 bytes (string bruta)
