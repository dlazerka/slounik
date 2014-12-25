String.prototype.escapeHTML = function () {
    return(
        this.replace(/&/g,'&amp;').
            replace(/>/g,'&gt;').
            replace(/</g,'&lt;').
            replace(/"/g,'&quot;')
    );
};
function fillFields(query) {
	$('#kind').val(query.kind);
	$('#ancestor').val(query.ancestor);
	$('#limit').val(query.limit);
	query.filters.forEach(function(filter){
		var div = addFilterInput();
		$('.name', div).val(filter.field);
		$('.operator option[value="' + filter.operator + '"]', div).attr('selected', true);
		$('.value', div).val(filter.value);
		$('.type option[value="' + filter.type +'"]', div).attr('selected', true);
	});
}
function getValidatedQuery() {
	var query = {};
	var kind = $('#kind').val();
	var ancestor = $('#ancestor').val();
	if (!kind && !ancestor) {  // parse location.hash
		if (!location.hash) return;
		query = location.hash.substring(1);
		query = $.parseJSON(query);
		fillFields(query);
	} else {
		if (kind) query.kind = kind;
		if (query) query.ancestor = ancestor;
		filters = [];
		$('.filter').each(function(i, div) {
			var name = $('.name', div).val();
			var operator = $('.operator', div).val();
			var value = $('.value', div).val();
			var type = $('.type', div).val();
			var active = $('.active:checked', div).length > 0;
			if (!name || !active) return;
			filters.push({
				'field': name,
				'operator': operator,
				'value': value,
				'type': type
			});
		});
		if (filters) query.filters = filters;
		query.limit = $('#limit').val();
		document.location.hash = JSON.stringify(query);
	}
	return query;
}
function refresh() {
	var query = getValidatedQuery();
	if (!query) return;
	document.title = 'Loading...'
	
	$.get('db/list', {query: JSON.stringify(query)}).done(function(data, textStatus, jqXHR) {
		document.title = (query.kind == undefined ? '' : query.kind) + ' ' + query.ancestor;
		if (data.error) {
			alert(data.error);
			return;
		}
		// name -> column index
		var columns = {};
		// array of arrays
		var matrix = [];
		data.entities.forEach(function(entity) {
			var row = [];
			for (column in entity) {
				column = column.escapeHTML();
				if (columns[column] === undefined) {
					columns[column] = Object.keys(columns).length;
				}
				row[columns[column]] = entity[column];
			}
			matrix.push(row);
		});
		// string builder for innerHtml
		var sb = [];
		var columnsCount = Object.keys(columns).length;
		if (!columnsCount) {
			columns['no entities'] = 0;
		}
//		sb.push('<th><input type="checkbox" onchange="$(\'[name=row]\').attr(\'checked\', this.checked);"/></th>');
		Object.keys(columns).forEach(function(column) {
			sb.push('<th>' + column + '</th>');
		});
		var thead = $('#table > thead');
		thead.html(sb.join());
		sb = [];
		matrix.forEach(function(row) {
			sb.push('<tr>');
//			sb.push('<td><input type="checkbox" name="row" value="' + row[0].value.escapeHTML() + '"/></td>');
			for (var i = 0; i < columnsCount; i++) {
				if (row[i]) {
					sb.push('\t<td title="' + row[i].type + '">' + row[i].value.escapeHTML() + '</td>');
				} else {
					sb.push('\t<td></td>');
				}
			}
			sb.push('</tr>');
		});
		var tbody = $('#table > tbody');
		tbody.html(sb.join());

		$('#count').text(matrix.length);
		if (matrix.length == query.limit) {
			$('#count').text($('#count').text() + "+");
		}
	});
};
function delete_() {
	var query = getValidatedQuery();
	if (!query) return;
	$.get('db/delete', {query: JSON.stringify(query)}).done(function(data, textStatus, jqXHR) {
		var tbody = $('#table > tbody');
		tbody.html('');
	});
}
function count() {
	var query = getValidatedQuery();
	if (!query) return;
	$.get('db/count', {query: JSON.stringify(query)}).done(function(data, textStatus, jqXHR) {
		_fillCount(data.count, query);
	});
}
function _fillCount(count, query) {
	$('#count').text(count);
	if (count == query.limit) {
		$('#count').text($('#count').text() + "+");
	}
}
function addFilterInput() {
	var lastFilter = $('.filter:last');
	if (!$('.name', lastFilter).val()) return lastFilter;
	var clone = lastFilter.clone();
	lastFilter.after(clone);
	return clone;
}
$(function() {
	$.ajaxSetup({
		type: 'GET',
		timeout: 100000,
		error: function(jqXHR, textStatus, errorThrown) {
			document.title = textStatus;
			alert (textStatus + '\n' + errorThrown);
		},
	});
	refresh();
});
