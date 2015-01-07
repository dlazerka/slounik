'use strict';
angular.module('me.lazerka.slounik', [])
	.directive('preselect', function() {
		return {
			link: function($scope, element, attrs) {
				$scope.$watch('input', function() {
					if (element.hasClass('ng-pristine')) {
						element[0].select();
					}
				});
			}
		}
	})
	.controller('SlounikController', function($scope, $http) {
		$scope.input = 'слоўнік';
		$scope.results = [];

		// $scope.found to show/hide "not found" message.
		$scope.searching = false;
		$scope.found = true;

		$scope.$watch('input', function() {
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
			function Lemma(lemma) {
				if (lemma.indexOf($scope.input) == 0) {
					this.match = $scope.input;
					this.rest = lemma.substr($scope.input.length);
				} else {
					this.match = '';
					this.rest = lemma;
				}
			}

			$scope.results = [];
			$scope.results.input = requestedInput;
			angular.forEach(response.data, function(res) {
				var lemma = new Lemma(res.lemma);
				var translations = res.translations.map(function(translation) {
					return new Lemma(translation);
				});

				var result = lemma.from == 'ru'
					? {ru: [lemma], be: translations, arrow: '←'}
					: {be: [lemma], ru: translations, arrow: '→'};

				$scope.results.push(result);
			});

			$scope.found = $scope.results.length > 0;
		}
	})
;

