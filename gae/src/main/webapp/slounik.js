'use strict';
angular.module('me.lazerka.slounik', [])
	.directive('preselect', function($timeout) {
		return {
			link: function($scope, element, attrs) {
				// Have to do $timeout, because DOM is not yet synced with $scope.
				$timeout(function() {
					element.select();
				});
			}
		}
	})
/*
	.factory('DictService', function() {
		var dicts = {};

		$http.get('/rest/dict/be/ru')
			.then(function(response) {
				angular.forEach(response.data, function(res) {
					dicts[res.id] = dict;
				});
			});

		return {
			getDict : function(dictId) {
				return dicts[dictId];
			}
		}
	})
*/
	.controller('SlounikController', function($scope, $http) {
		$scope.input = localStorage.getItem('input') || 'слоўнік';
		$scope.results = [];

		// $scope.found to show/hide "not found" message.
		$scope.searching = false;
		$scope.found = true;

		$scope.$watch('input', function() {
			localStorage.setItem('input', $scope.input);
			if (!$scope.input) return;
			$scope.searching = true;

			$http.get('/rest/entry/search/be/ru/' + encodeURI($scope.input))
				// binding requested value so we can handle race conditions.
				.then(resultsArrived.bind(this, $scope.input));
		});

		function resultsArrived(requestedInput, response) {
			if ($scope.input != requestedInput) {
				// User already typed more characters, so abandoning the result.
				return;
			}
			$scope.searching = false;

			/** Divides response onto `match` and `rest`. */
			function Translation(lemma, dict) {
				this.lemma = lemma;

				if (lemma.indexOf($scope.input) == 0) {
					this.match = $scope.input;
					this.rest = lemma.substr($scope.input.length);
				} else {
					this.match = '';
					this.rest = lemma;
				}
				this.dicts = [dict];
			}

			var rows = [];
			var rowByLemma = {
				ru: {},
				be: {}
			};
			angular.forEach(response.data, function(res) {
				var lemma = new Translation(res.lemma, res.dict);
				var translations = res.translations.map(function(translation) {
					return new Translation(translation, res.dict);
				});

				var row = {
					ru: {
						translations: res.from == 'ru' ? [lemma] : translations,
						dict: null
					},
					be: {
						translations: res.from == 'be' ? [lemma] : translations,
						dict: null
					},
					arrow: res.from == 'ru' ? '←' : '→'
				};

				var existingRow = rowByLemma[res.from][res.lemma];
				if (existingRow) {
					// Merge with existingRow
					angular.forEach(['ru', 'be'], function(lang) {
						var newTranslations = [];

						angular.forEach(existingRow[lang].translations, function(translation) {
							// Was the translation already seen?
							var newTranslation = row[lang].translations.filter(function(newTranslation) {
								return newTranslation.lemma == translation.lemma;
							})[0];

							if (newTranslation) {
								// Then don't add the translation, only add it's dict.
								translation.dicts = translation.dicts.concat(newTranslation.dicts);
							} else {
								newTranslations = row[lang].translations;
							}
						});
						existingRow[lang].translations = existingRow[lang].translations.concat(newTranslations);
					});
				} else {
					rows.push(row);
					rowByLemma[res.from][res.lemma] = row;
				}
			});

			$scope.rows = rows;
			$scope.found = rows.length > 0;

			$scope.mouseOver = function(translation, row) {
				row.dict = translation.dicts.join(', ');
			};

			$scope.mouseOut = function(row) {
				row.dict = null;
			};
		}
	})
;

