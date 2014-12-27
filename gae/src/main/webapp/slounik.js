'use strict';
angular.module('me.lazerka.slounik', [])
	.controller('SlounikController', function($scope) {
		$scope.input = 'слоўнік';
		$scope.results = [];
		$scope.results.push({
			'be': {match: 'слоўнік', rest: ''},
			'ru': {match: '', rest: 'словарь'},
			'arrow': '→'
		});
		$scope.results.push({
			'be': {match: 'слоўнік', rest: 'авы'},
			'ru': {match: '', rest: 'словарный'},
			'arrow': '←'
		});
	})
;
