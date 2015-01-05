'use strict';
angular.module('me.lazerka.slounik', [])
	.controller('SlounikController', function($scope, $http) {
		$scope.input = 'слоўнік';
		$scope.results = [];

		// $scope.found to show/hide "not found" message.
		$scope.found = false;

		$scope.$watch('input', function() {
			if (!$scope.input) return;

			$http.get('/rest/entry/search/be/ru/' + encodeURI($scope.input))
				// binding requested value so we can handle race conditions.
				.then(resultsArrived.bind(this, $scope.input));
		});

		function resultsArrived(requestedInput, response) {
			if ($scope.input != requestedInput) {
				// User already typed more characters, so abandoning the result.
				return;
			}

			/** Divides response onto `match` and `rest`. */
			function getResult(response) {
				if (response.indexOf($scope.input) == 0) {
					return {
						match: $scope.input,
						rest: response.substr($scope.input.length)
					}
				} else {
					return {
						match: '',
						rest: response
					}
				}
			}

			$scope.results = [];
			$scope.results.input = requestedInput;
			angular.forEach(response.data, function(res) {
				// todo
				$scope.results.push({
					ru: getResult(res['ru']),
					be: getResult(res['be'])
				});
			});

			$scope.found = $scope.results.length > 0;
		}
	})
;
