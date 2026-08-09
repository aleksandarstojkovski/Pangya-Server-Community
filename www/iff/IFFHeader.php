<?php
//desenvolvimento Luiz MK, luizinrc@hotmail.com
namespace PangyaIFF\Parser;
 
const IFF_HEADER_SIZE = 8; // ushort count, ushort reserved, uint version (little-endian)
$IFF_HEADER_FORMAT = 'vcount/vreserved/Vversion'; // v = unsigned short (little-endian), V = unsigned long (32-bit little-endian)
 