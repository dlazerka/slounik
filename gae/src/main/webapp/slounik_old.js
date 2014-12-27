/** Copyright Dzmitry Lazerka <me@dlazerka.name>
 *  http://slounik.dlazerka.name/
 *  Licensed on GPL terms: http://www.gnu.org/licenses/gpl.html
 */
$(function() {
	var SEARCH_URL = location.href + 'rpc/search';
	if (window.location.protocol == 'chrome-extension:') {
		SEARCH_URL = 'http://slounik.dlazerka.name/rpc/search';
	}
	var inputEl = $('.input');
	var rightEl = $('.right .words');
	var leftEl = $('.left .words');
	var arrowsEl = $('.arrows');

	function toLines(data) {
		var lines = [];
		var query = $('query', data).text();
		$('entry', data).each(function(index, entry) {
			var line = {};
			lines.push(line);
			$('lexeme', entry).each(function(index, lexeme) {
				var word = $(lexeme).text();
				if (word.search(query) == 0) {
					word = word.substring(query.length);
					var span = '<span class="query">' + query + '</span>';
					word = span + word;
					line['arrow'] = line['arrow'] ? '&harr;' :
						(index ? '&larr;' : '&rarr;');
				}
				var lang = $(lexeme).attr('lang');
				line[lang] = word;
			});
		});
		return lines;
	}

	/** Don't compare arrows, only words. */
	function linesComparator(line1, line2) {
		/** First by length, then naively. */
		function wordsComparator(word1, word2) {
			if (word1.length != word2.length) {
				return word1.length - word2.length;
			}
			if (word1 == word2) return 0;
			return 1 - 2*Number(word1 < word2);
		}
		var word1 = line1['arrow'] == '&larr;' ? line1['ru'] : line1['be'];
		var word2 = line2['arrow'] == '&larr;' ? line2['ru'] : line2['be'];
		var word1_ = line1['arrow'] == '&rarr;' ? line2['ru'] : line2['be'];
		var word2_ = line2['arrow'] == '&rarr;' ? line2['ru'] : line2['be'];
		if (word1 != word2) {
			return wordsComparator(word1, word2);
		} else if (word1 != word2_) {
			return wordsComparator(word1, word2_);
		} else if (word2 != word1_) {
			return wordsComparator(word1_, word2);
		} else {
			return 0;
		}
	}

	function found(data, textStatus, jqXHR) {
		var lines = toLines(data);
		if (!lines.length) {
			inputEl.addClass('noResults');
			return;
		}
		inputEl.removeClass('noResults');
		inputEl.select();

		lines.sort(linesComparator);

		var beWords = lines.map(function(line) {
			return line['be'];
		});
		var ruWords = lines.map(function(line) {
			return line['ru'];
		});
		var arrows = lines.map(function(line) {
			return line['arrow'];
		});

//		var lastLine = lines[lines.length - 1];
//		leftEl.css('min-width', lastLine['be'].length + 'ex');
//		rightEl.css('min-width', lastLine['ru'].length + 'ex');

		leftEl.html(beWords.join('<br/>'));
		rightEl.html(ruWords.join('<br/>'));
		arrowsEl.html('<br/>' + arrows.join('<br/>'));
		rightEl.show();
		arrowsEl.show();
		leftEl.show();
	}

	inputEl.bind('keyup', function(event) {
		inputEl.removeClass('noResults');
		if (event.keyCode != 13) return;
		$.get(SEARCH_URL, {'q': inputEl.val()}, found, 'xml');
		rightEl.hide();
		arrowsEl.hide('');
		leftEl.hide('');
	});
	inputEl.bind('touchend', function(event) {
		inputEl.select();
	});
	inputEl.select();
	$.get(SEARCH_URL, {'q': inputEl.val()}, found, 'xml');
});
