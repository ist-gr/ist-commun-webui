/*global define, $ */
define(['angular',
        'text!./youTubeLink.html'], function (angular, youTubeLink) {
	'use strict';

	var module = angular.module('youTubeLink', []);

  module.directive('youtube', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        vid: '@vid',
        label: '@label',
        duration: '@duration',
        flag: '@flag'
      },
      template: youTubeLink
    };
  });

  return module;
});
