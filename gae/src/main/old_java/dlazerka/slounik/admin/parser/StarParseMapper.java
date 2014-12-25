package name.dlazerka.slounik.admin.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.appengine.repackaged.com.google.common.collect.Sets;

/**
 * Parses StarDict format.
 *
 * <ol>
 * <li>Skip multiword record line.
 * <li>Drop anything inside {@code <i>(.*)</i>}
 * <li>Drop {@code <i>род.</i>} with word after it.
 * <li>Drop {@code , -.*}
 * <li>Replace {@code <b>.*</b>} to ;
 * <li>Drop all multiwords</li>
 *
 */
public class StarParseMapper extends AbstractParseMapper {
	private static final String WORD_CAP = "[А-ЯЎІЁ][а-яўіё'-]+";
	private static final String WORD_NOCASE = "[0-9A-Za-zА-ЯЎІЁа-яўіё'!.-]+";
	private static final String MULTIWORD_COMMAS = "(?:" + WORD_NOCASE + "(?:[ ,]+))+" + WORD_NOCASE;
	private static final String MULTIWORD = "(?:" + WORD_NOCASE + "(?:[ ]+))+" + WORD_NOCASE;
	private static final Pattern U = Pattern.compile("<u>|</u>");
	private static final String[] DROP = {
		"\\([^\\)]+?\\)",
		"<i>род\\.</i> [^ <]+",
		// skip multiword start lemmas to the end of the line
		"<b>\\s*" + MULTIWORD_COMMAS + "\\s*</b>.*?(?:<br>|$)",
		"<b>.*?</b>",
		"<i>.*?</i>", // before next, see Таити
		" -[^ ,;<]+",
		"\\(.*?\\)",
		"<a .*?</a>",
		" +-+ +",// word1 - word2
		MULTIWORD,// words separated by space
		"<br>",
		"[а-я0-9]\\)",
		"[0-9,;:◊!\\?\\.«»°—]+",
		"[0-9°]+",
		WORD_CAP,
	};
	private static final List<Pattern> DROP_PATTERNS = Lists.newArrayList();
	static {
		StringBuilder sb = new StringBuilder();
		for (String pattern : DROP) {
			sb.append('(');
 			sb.append(pattern);
 			sb.append(")|");
 			DROP_PATTERNS.add(Pattern.compile(pattern, Pattern.MULTILINE));
		}
	}

	@Override
	protected ArrayList<String> mapInternal(String content)
			throws IOException {
		ArrayList<String> result = Lists.newArrayList();

		content = U.matcher(content).replaceAll("");
		for (Pattern pattern : DROP_PATTERNS) {
			content = pattern.matcher(content).replaceAll(" ");
		}
		content = content.trim();

		String[] split = content.split("\\s+");
		for (String str : Sets.newHashSet(split)) {
			if ("".equals(str)) continue;
			result.add(str);
		}
		return result;
	}
}
