/*global define, $ */
define(['angular',
        'jquery-backstretch',
        '../../security/security',
        'css!metronic/css/pages/coming-soon'], function (angular) {
	'use strict';

	// XXX temporary solution to prompt the user for using a supported browser
	var module = angular.module('browserPrompt', ['security']);

	var UNSUPPORTED_BROWSER_PATH = '/unsupported-browser';
	
	module.config(['$routeProvider', function ($routeProvider) {
		$routeProvider
			.when(UNSUPPORTED_BROWSER_PATH, {
				templateUrl: 'modules/support/browser-prompt.html',
				isUnprotected: true
			});
	}]);
	
	module.factory('isBrowserSupported', ['$window', function($window) {
		var userAgent = $window.navigator.userAgent;
		var offset=userAgent.indexOf('Chrome'),
		fullVersion=(offset === -1) ? '' : userAgent.substring(offset+7),
		majorVersion,
		ix;

		// trim the fullVersion string at semicolon/space if present
		if ((ix=fullVersion.indexOf(';'))!==-1) {
			fullVersion=fullVersion.substring(0,ix);
		}
		if ((ix=fullVersion.indexOf(' '))!==-1) {
			fullVersion=fullVersion.substring(0,ix);
		}
		majorVersion = parseInt(''+fullVersion,10);
		
		return majorVersion >= 31;
	}]);

	module.controller('BrowserCtrl', ['isBrowserSupported', function(isBrowserSupported) {
		if (!isBrowserSupported) {
			$.backstretch([
				'metronic/img/bg/1.jpg',
				'metronic/img/bg/2.jpg',
				'metronic/img/bg/3.jpg',
				'metronic/img/bg/4.jpg'
			], {
				fade: 1000,
				duration: 10000
			});
		}
	}]);
	
	module.run(['$rootScope', '$location', 'isBrowserSupported', function ($root, $location, isBrowserSupported) {
    $root.$on('$routeChangeStart', function(event, currRoute/*, prevRoute*/) {
      if (currRoute.originalPath === UNSUPPORTED_BROWSER_PATH) {
        if (isBrowserSupported) {
          // redirect to home if someone comes here but browser is supported
          $location.url('/').replace();
        }
      } else if (!isBrowserSupported) {
        $location.url(UNSUPPORTED_BROWSER_PATH).replace();
      }
    });
	}]);

  return module;
});
