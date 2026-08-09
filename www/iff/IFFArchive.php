<?php
//desenvolvimento Luiz MK, luizinrc@hotmail.com
namespace PangyaIFF\Parser;

class IFFArchive {
    private static ?string $archivePath = null;
    private static ?\ZipArchive $zip = null;
    private static bool $initialized = false;

    /**
     * Define explicitamente o caminho para o arquivo ZIP do pacote IFF.
     */
    public static function setArchivePath(string $path): void {
        self::$archivePath = $path;
        self::$zip = null; // força reinicialização na próxima leitura
        self::$initialized = false;
    }

    /**
     * Retorna o caminho do arquivo do archive. Se não foi configurado,
     * aplica o fallback padrão relativo à pasta do parser (esperando em root/data/pangya_jp.iff).
     */
    public static function getArchivePath(): string {
        if (self::$archivePath === null) {
            // Fallback padrão: sobe uma pasta a partir de 'iff/' e aponta para 'data/pangya_jp.iff'
            self::$archivePath = realpath(__DIR__ . '/../data/pangya_jp.iff') ?: (__DIR__ . '/../data/pangya_jp.iff');
        }
        return self::$archivePath;
    }

    /**
     * Inicializa e abre a instância do ZipArchive se o arquivo existir.
     */
    private static function init(): bool {
        if (self::$initialized) {
            return self::$zip !== null;
        }

        self::$initialized = true;
        $path = self::getArchivePath();

        if (!class_exists('ZipArchive')) {
            return false;
        }

        if (is_file($path)) {
            self::$zip = new \ZipArchive();
            if (self::$zip->open($path) === TRUE) {
                return true;
            }
            self::$zip = null;
        }

        return false;
    }

    /**
     * Verifica se uma entrada (ex.: Item.iff) existe dentro do pacote ZIP.
     */
    public static function has(string $entryName): bool {
        if (!self::init()) {
            return false;
        }
        // Tenta localizar a entrada considerando variações comuns de caminho/nome
        $index = self::$zip->locateName($entryName, \ZipArchive::FL_NOCASE);
        if ($index === false) {
            // Tenta procurar dentro de subpasta data/ se houver
            $index = self::$zip->locateName('data/' . $entryName, \ZipArchive::FL_NOCASE);
        }
        return $index !== false;
    }

    /**
     * Abre um stream de leitura sob demanda para uma entrada específica do ZIP,
     * sem precisar extrair o arquivo inteiro para o disco.
     * 
     * @return resource|null
     */
    public static function openEntryStream(string $entryName) {
        if (!self::init()) {
            return null;
        }

        $targetName = $entryName;
        $index = self::$zip->locateName($targetName, \ZipArchive::FL_NOCASE);
        
        if ($index === false) {
            $targetName = 'data/' . $entryName;
            $index = self::$zip->locateName($targetName, \ZipArchive::FL_NOCASE);
        }

        if ($index === false) {
            return null;
        }

        // O ZipArchive permite abrir um stream direto do arquivo compactado
        $stream = self::$zip->getStream(self::$zip->getNameIndex($index));
        return $stream !== false ? $stream : null;
    }

    /**
     * Retorna uma lista com todas as entradas contidas no arquivo ZIP.
     */
    public static function listEntries(): array {
        $entries = [];
        if (self::init() && self::$zip !== false) {
            for ($i = 0; $i < self::$zip->numFiles; $i++) {
                $stat = self::$zip->statIndex($i);
                if ($stat && isset($stat['name'])) {
                    $entries[] = $stat['name'];
                }
            }
        }
        return $entries;
    }
}