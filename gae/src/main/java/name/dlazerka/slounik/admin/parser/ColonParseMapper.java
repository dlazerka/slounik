package name.dlazerka.slounik.admin.parser;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.repackaged.com.google.common.collect.Lists;

/**
 * Parses simple hand-made colon-based format.
 */
public class ColonParseMapper extends AbstractParseMapper {
	private static final Logger logger = LoggerFactory.getLogger(ColonParseMapper.class);

	@Override
	protected ArrayList<String> mapInternal(String content) throws IOException {
		String[] parts = content.split("::");
		if (parts.length < 2) {
			logger.error("Unsupported format on line '{}'.", content);
			throw new IOException("Unsupported format on line '" + content + "'");
		}
		return Lists.newArrayList(parts[1]);
	}
}
