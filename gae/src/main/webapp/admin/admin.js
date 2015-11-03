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
angular.module('me.lazerka.slounik.admin', [])
	.controller('AdminController', function($scope, $http) {
		/**
		 * @param file {File}
		 */
		$scope.uploadFile = function(file) {
			var fr = new FileReader();
			fr.readAsText(file);
			console.log('Reading file ' + file.name);
			fr.onload = function (event) {
				var bytes = event.target.result;

				console.log('Read ' + event.total + ' bytes, sending...');
				return $http({
					url: '/admin/upload',
					method: 'POST',
					data: bytes,
					// Prevent Angular from serializing data.
					transformRequest: angular.identity,
					headers: {
						'Content-Type': 'text/plain'
					}
				})
					.then(function(response) {
						console.log('Received response ' + response.status);
					});
			};
		};
	})
	/**
	 * Angular doesn't handle onchange for <input type="file">-s.
	 */
	.directive('myOnchange', function() {
		return {
			scope: {
				myOnchange: '='
			},
			link: function($scope, element, attrs) {
				element.bind('change', function(event) {
					var files = event.target.files;
					if (!files || !files.length) {
						// May happen if you select a file, and then click Choose again and click Cancel.
						throw Error('No files in ' + event.target);
					}

					for (var i = 0; i < files.length; i++) {
						var file = files[i];

						if (!file.size) {
							alert("File size is 0, is it a file?");
							return;
						} else if (file.size > (32 << 20)) {
							alert("Sorry, files larger than 32 MB aren't supported.");
							return;
						}

						$scope.myOnchange(file);
					}
					//$scope[attrs.myOnchange](file);
				});
			}
		};
	})
;
