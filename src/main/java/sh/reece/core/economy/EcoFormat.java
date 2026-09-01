package sh.reece.core.economy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sh.reece.tools.Main;

/**
 * Reads a configurable economy message from config.yml (under {@code Economy.*}),
 * substitutes %placeholders%, and hands back a raw string. Callers send it through
 * Util.coloredMessage so the &-codes in the config get colorized.
 *
 * Templates are resolved from config once and cached by path, so a hot command like
 * /pay or /balance doesn't hit getConfig().getString() every time. The cache is
 * cleared on economy (re)load so edited messages take effect.
 *
 * The `def` fallback keeps old configs working even if a key is missing - it mirrors
 * what ships in the default config.yml.
 */
public final class EcoFormat {

	private EcoFormat() {}

	// path -> raw template (config value or its default). Populated lazily.
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

	/** Drop cached templates so the next read re-pulls from config. Call on reload. */
	public static void clearCache() {
		CACHE.clear();
	}

	/**
	 * @param path  path under "Economy." e.g. "Messages.PaySent" or "BalTop.Line"
	 * @param def   default used if the key is absent
	 * @param kv    alternating placeholder/value pairs, e.g. "player","Bob","amount","$5"
	 */
	public static String msg(Main plugin, String path, String def, String... kv) {
		String template = CACHE.get(path);
		if (template == null) {
			template = plugin.getConfig().getString("Economy." + path, def);
			CACHE.put(path, template);
		}
		if (kv.length == 0) {
			return template;
		}
		String s = template;
		for (int i = 0; i + 1 < kv.length; i += 2) {
			s = s.replace("%" + kv[i] + "%", kv[i + 1]);
		}
		return s;
	}
}
