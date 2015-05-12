/*global define */
define(['angular',
  'text!./logs.html',
  'angular-route',
  '../../core'], function (angular, logsTemplate) {
  'use strict';

  var module = angular.module('logs', []);

  module.config(['$routeProvider', function ($routeProvider) {
    $routeProvider
      .when('/logs', {
        template: logsTemplate,
        resolve:{
          resolvedLogs:['LogsService', function (LogsService) {
            return LogsService.findAll();
          }]
        },
        isUnprotected: true // TODO protect this page
      });
  }]);

  module.factory('LogsService', ['$resource', 'apiService',
    function ($resource, apiService) {
      return $resource(apiService.apiPathname + 'logs', {}, {
        'findAll': { method: 'GET', isArray: true},
        'changeLevel':  { method: 'PUT'}
      });
    }]);

  module.controller('LogsController', ['$scope', 'LogsService',
    function ($scope, LogsService) {
      $scope.log = {};

      $scope.loggers = LogsService.findAll();

      $scope.changeLevel = function (name, level) {
        LogsService.changeLevel({name: name, level: level}, function () {
          $scope.loggers = LogsService.findAll();
        });
      }
    }]);

  module.filter('characters', function () {
    return function (input, chars, breakOnWord) {
      if (isNaN(chars)) return input;
      if (chars <= 0) return '';
      if (input && input.length > chars) {
        input = input.substring(0, chars);

        if (!breakOnWord) {
          var lastspace = input.lastIndexOf(' ');
          //get last space
          if (lastspace !== -1) {
            input = input.substr(0, lastspace);
          }
        } else {
          while(input.charAt(input.length-1) === ' '){
            input = input.substr(0, input.length -1);
          }
        }
        return input + '...';
      }
      return input;
    };
  });

  module.filter('words', function () {
      return function (input, words) {
        if (isNaN(words)) return input;
        if (words <= 0) return '';
        if (input) {
          var inputWords = input.split(/\s+/);
          if (inputWords.length > words) {
            input = inputWords.slice(0, words).join(' ') + '...';
          }
        }
        return input;
      };
    });

  return module;
});
