/*
 *    Copyright 2015 Dzmitry Lazerka
 *
 *    This file is part of Slounik.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

'use strict';

angular.module('name.dlazerka.slounik', [])
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

