<?php
//desenvolvimento Luiz MK, luizinrc@hotmail.com
namespace PangyaIFF\Parser;

require_once __DIR__ . '/IFFArchive.php';

function read_header(string $path) : array {
    global $IFF_HEADER_FORMAT;
    $f = fopen($path, 'rb');
    if (!$f) throw new \Exception("Cannot open file: $path");
    $raw = fread($f, IFF_HEADER_SIZE);
    fclose($f);
    if (strlen($raw) < IFF_HEADER_SIZE) throw new \Exception("Invalid IFF header");
    $h = unpack($IFF_HEADER_FORMAT, $raw);
    return $h;
}



function trim_nulls(string $s): string {
    return rtrim($s, "\0");
}

/**
 * Abre um stream de leitura para um arquivo IFF "lógico" (ex.: Item.iff),
 * tentando nesta ordem:
 *   1) A entrada correspondente dentro do pacote pangya_jp.iff (ZIP) —
 *      lida sob demanda, sem extrair nada para disco.
 *   2) O arquivo solto em disco, em $legacy_path (compatível com os
 *      projetos antigos que ainda têm Item.iff/Part.iff/etc. extraídos).
 *
 * Isso é o único lugar que decide "de onde vêm os bytes" — os parsers
 * (find_iff_item, list_iff_items, etc.) não precisam saber se o dado
 * veio do ZIP ou de um arquivo solto, só recebem um resource normal.
 *
 * @return resource|null
 */
function open_iff_stream(string $entry_name, string $legacy_path) {
    $stream = IFFArchive::openEntryStream($entry_name);
    if ($stream !== null) {
        return $stream;
    }

    if (is_file($legacy_path)) {
        $f = fopen($legacy_path, 'rb');
        return $f !== false ? $f : null;
    }

    return null;
}

/** Tamanho em bytes de um stream aberto com open_iff_stream(). */
function iff_stream_size($stream): int {
    $stat = fstat($stream);
    return $stat['size'] ?? 0;
}