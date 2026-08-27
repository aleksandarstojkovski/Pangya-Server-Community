using System;
using System.Text;

namespace Pangya_GameServer.UTIL
{
    public static class UtilChat
    {
        public enum ChatColor : uint
        {
            // --- CORES PADRÃO / SISTEMA ---
            White = 0xFFFFFFFF, // Reset de cor / Texto padrão
            Red = 0xFFFF0000, // Erros / Alertas críticos / Ban
            Green = 0xFF00FF00, // Sucesso / Avisos normais
            Blue = 0xFF0000FF, // Chat de Guilda antiga
            Yellow = 0xFFFFFF00, // Avisos de Sistema / Eventos
            Cyan = 0xFF00FFFF, // Links / Ciano
            Orange = 0xFFFFA500, // Mensagens Importantes

            // --- CORES DE RANKING / PREMIAÇÃO ---
            Gold = 0xFFFFD700, // 1º Lugar / VIP / Torneios
            Silver = 0xFFC0C0C0, // 2º Lugar / Prata
            Bronze = 0xFFCD7F32, // 3º Lugar / Bronze

            // --- CORES ADICIONAIS / RARIDADES ---
            Magenta = 0xFFFF00FF, // Rosa Choque / Mensagens de GM
            Pink = 0xFFFFC0CB, // Rosa Claro
            Purple = 0xFF800080, // Roxo / Itens Raros (Drop)
            Violet = 0xFFEE82EE, // Violeta Claro
            DarkGreen = 0xFF006400, // Verde Escuro / Floresta
            LightBlue = 0xFF87CEEB, // Azul Céu / Suporte

            // --- TONS DE CINZA ---
            LightGray = 0xFFAAAAAA, // Cinza Claro (Informativos/Rodapé)
            DarkGray = 0xFF555555  // Cinza Escuro (Logs/GameUser)
        }

        /// <summary>
        /// Retorna a string formatada em hexadecimal exigida pelo cliente Pangya (ex: "0xffff0000").
        /// </summary>
        public static string ToHexString(this ChatColor color) => $"0x{(uint)color:x8}";

        /// <summary>
        /// Aplica a tag de cor no texto para exibição no cliente Pangya.
        /// Exemplo: FixColor(ChatColor.Red, "Erro") -> "\c0xffff0000\cErro"
        /// </summary>
        public static string FixColor(ChatColor color, string text)
        {
            if (string.IsNullOrEmpty(text)) return text;

            return $"\\c{color.ToHexString()}\\c{text}";
        }

        /// <summary>
        /// Sobrecarga legada para manter compatibilidade com strings hexadecimais diretas.
        /// </summary>
        public static string FixColor(string hexColor, string text)
        {
            if (string.IsNullOrEmpty(text)) return text;

            return $"\\c{hexColor}\\c{text}";
        }

        /// <summary>
        /// Interpola múltiplos segmentos de texto com suas respectivas cores.
        /// </summary>
        public static string FixColor(params (ChatColor color, string text)[] segments)
        {
            if (segments == null || segments.Length == 0) return string.Empty;

            var sb = new StringBuilder();

            foreach (var (color, text) in segments)
            {
                if (!string.IsNullOrEmpty(text))
                {
                    sb.Append($"\\c{color.ToHexString()}\\c{text}");
                }
            }

            return sb.ToString();
        }
    }
}